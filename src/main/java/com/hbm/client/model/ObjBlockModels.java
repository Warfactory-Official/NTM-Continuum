//? if >=26.1 {
package com.hbm.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import dev.golem.core.Ids;
import dev.golem.core.client.UnbakedModels;
import dev.golem.core.render.gem.Geometry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

// Resource-backed block OBJ geometry: the ordinary native model and item bakers consume the same quads.
// Baked block quads use native directional face lighting; authored positions and UVs stay in the mesh.
public final class ObjBlockModels {
    private ObjBlockModels() {}

    public static void install() {
        UnbakedModels.register(Ids.of("hbm", "obj"), ObjBlockModels::read);
    }

    private static UnbakedModel read(JsonObject json, JsonDeserializationContext context) {
        if (json.has("groups") || json.has("hidden") || json.has("transform") || json.has("visibility")) {
            throw new IllegalArgumentException("OBJ group poses, visibility and root transforms are not supported by this baker");
        }
        var inherited = json.deepCopy();
        inherited.remove("fabric:type");
        inherited.remove("loader");
        inherited.remove("golem:loader");
        var base = context.<UnbakedModel>deserialize(inherited, UnbakedModel.class);
        var skins = new LinkedHashMap<String, List<String>>();
        for (var entry : json.getAsJsonObject("skins").entrySet()) {
            skins.put(entry.getKey(), entry.getValue().getAsJsonArray().asList().stream()
                    .map(value -> value.getAsString()).toList());
        }
        var source = Ids.parse(json.get("model").getAsString());
        // OBJ block coordinates are centered in X/Z; source offset_y places the authored vertical origin.
        var offset = new Vector3f(0.5F + number(json, "offset_x"), number(json, "offset_y"),
                0.5F + number(json, "offset_z"));
        var geometry = new ObjGeometry(source, skins, offset);
        return new CuboidModel(geometry, base.guiLight(), base.ambientOcclusion(), base.transforms(),
                base.textureSlots(), base.parent());
    }

    private static float number(JsonObject json, String field) {
        return json.has(field) ? json.get(field).getAsFloat() : 0F;
    }

    // The native baker owns this cache, so an OBJ is shared across facings/items and re-read on the next reload.
    private record MeshKey(Identifier source) implements ModelBaker.SharedOperationKey<WavefrontObj> {
        @Override public WavefrontObj compute(ModelBaker baker) { return WavefrontObj.load(source); }
    }

    private record ObjGeometry(Identifier source, Map<String, List<String>> skins, Vector3f offset)
            implements UnbakedGeometry {
        @Override
        public QuadCollection bake(TextureSlots textures, ModelBaker baker, ModelState state, ModelDebugName name) {
            var mesh = baker.compute(new MeshKey(source));
            var output = new QuadCollection.Builder();
            var rotation = state.transformation().getMatrix();
            var interner = baker.interner();
            for (var skin : skins.entrySet()) {
                var material = baker.materials().resolveSlot(textures, skin.getKey(), name);
                var sprite = material.sprite();
                var info = interner.materialInfo(BakedQuad.MaterialInfo.of(material, sprite.transparency(), -1, true, 0));
                var vertices = mesh.parts(skin.getValue().toArray(String[]::new)).vertices();
                var stride = Geometry.FLOATS_PER_VERTEX;
                for (int at = 0; at < vertices.length; at += 3 * stride) {
                    var a = position(vertices, at, offset, rotation);
                    var b = position(vertices, at + stride, offset, rotation);
                    var c = position(vertices, at + 2 * stride, offset, rotation);
                    var normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
                    var face = Direction.getApproximateNearest(normal.x, normal.y, normal.z);
                    if (!state.faceTransformation(face).equals(ModelState.NO_TRANSFORM)) {
                        throw new IllegalArgumentException("OBJ UV locking requires an explicit authored UV policy: " + name.debugName());
                    }
                    long uv0 = uv(vertices, at, sprite), uv1 = uv(vertices, at + stride, sprite);
                    long uv2 = uv(vertices, at + 2 * stride, sprite);
                    // The same degenerate fourth vertex as WavefrontObj's native QUADS draw path.
                    output.addUnculledFace(new BakedQuad(interner.vector(a), interner.vector(b), interner.vector(c),
                            interner.vector(c), uv0, uv1, uv2, uv2, face, info));
                }
            }
            return output.build();
        }
    }

    private static Vector3f position(float[] vertices, int at, Vector3f offset, Matrix4fc rotation) {
        var result = new Vector3f(vertices[at], vertices[at + 1], vertices[at + 2]).add(offset).sub(0.5F, 0.5F, 0.5F);
        rotation.transformPosition(result);
        return result.add(0.5F, 0.5F, 0.5F);
    }

    private static long uv(float[] vertices, int at, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        float u = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * vertices[at + 3];
        float v = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * vertices[at + 4];
        return UVPair.pack(u, v);
    }
}
//?}
