package com.hbm.client.render.flywheel;

//? if flywheel {

/*import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.IndexSequence;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.vertex.MutableVertexList;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.golem.core.render.gem.Geometry;
import dev.golem.core.render.gem.Surface;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import org.lwjgl.system.MemoryUtil;

/^*
 * Turns a gem {@link Geometry} on a {@link Surface} into the published engine's {@link Model}.
 *
 * <p>A bucket is a draw call, and the engine keys one off the model instance, so the same lump on the
 * same skin has to come back as the same object every time or every block entity allocates its own.
 * That is what the cache is for; it is keyed on the pair because one OBJ group is drawn on several
 * skins (the arc furnace's electrodes, hot and cold) and each pairing is its own bucket.
 ^/
final class GemModels {

    private static final Map<Key, Built> MODELS = new ConcurrentHashMap<>();
    private static final Map<Surface, Material> MATERIALS = new ConcurrentHashMap<>();

    private GemModels() {}

    /^*
     * Keyed on the lump's <em>name</em> rather than the object, because a resource reload rebuilds the
     * renderers and the same OBJ comes back as a fresh {@link Geometry}, with different triangles if a
     * pack replaced the file. Keying on identity would hold the old model forever and grow the map on
     * every reload; keying on the name replaces it, which is also what makes a pack swap take effect.
     ^/
    static Model of(Geometry geometry, Surface surface) {
        // Atomic, not get-then-put: frame plans run on the engine's worker threads, and two of them
        // racing here would mint two models for one lump and split it across two draw calls.
        return MODELS.compute(new Key(geometry.name(), surface), (key, held) ->
                held != null && held.geometry() == geometry
                        ? held
                        : new Built(geometry, new SingleMeshModel(new GeometryMesh(geometry), materialOf(surface))))
                .model();
    }

    /^*
     * The engine has no notion of a render type, so a surface's blend mode is spelled out here as the
     * material properties that reproduce it. {@code CUTOUT_CULL} and {@code SOLID} both land on the
     * culling cutout: the old renderers ran with the alpha test on, so a fully transparent texel is
     * discarded rather than drawn as opaque black.
     ^/
    private static Material materialOf(Surface surface) {
        return MATERIALS.computeIfAbsent(surface, skin -> {
            SimpleMaterial.Builder builder = SimpleMaterial.builder()
                    .texture(dev.golem.core.Ids.parse(skin.texture()))
                    .mipmap(false);
            return switch (skin.blend()) {
                case SOLID, CUTOUT, CUTOUT_CULL -> builder.cutout(CutoutShaders.ONE_TENTH).build();
                case CUTOUT_NO_CULL -> builder.cutout(CutoutShaders.ONE_TENTH).backfaceCulling(false).build();
                case TRANSLUCENT -> builder.transparency(Transparency.TRANSLUCENT)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .build();
                // Molten metal and glowing rods: blended, lit by nothing, and not depth-writing, so the
                // self-overlapping geometry does not occlude its own far side.
                case TRANSLUCENT_EMISSIVE -> builder.transparency(Transparency.ADDITIVE)
                        .light(LightShaders.FLAT)
                        .writeMask(WriteMask.COLOR)
                        .backfaceCulling(false)
                        .build();
                // 1.7's SRC_ALPHA/ONE glow: alpha-weighted additive, unculled and not depth-writing.
                case PLASMA_GLOW -> builder.transparency(Transparency.ADDITIVE)
                        .light(LightShaders.FLAT)
                        .writeMask(WriteMask.COLOR)
                        .backfaceCulling(false)
                        .build();
            };
        });
    }

    /^* The eight floats per vertex {@code Geometry} hands over, in the order the GPU layout wants them. ^/
    private record GeometryMesh(Geometry geometry) implements Mesh {

        private static final int STRIDE = 8;

        /^*
         * {@code WavefrontObj} emits unindexed triangle soup, three fresh vertices per triangle and an
         * identity index list, so index <i>i</i> is vertex <i>i</i> for every mesh here, and one
         * shared run serves them all exactly as {@code QuadIndexSequence.INSTANCE} serves every quad.
         ^/
        private static final IndexSequence SEQUENTIAL = (ptr, count) -> {
            for (int index = 0; index < count; index++) {
                MemoryUtil.memPutInt(ptr + index * 4L, index);
            }
        };

        @Override
        public int vertexCount() {
            return geometry.vertexCount();
        }

        @Override
        public void write(MutableVertexList list) {
            float[] vertices = geometry.vertices();
            for (int vertex = 0; vertex < geometry.vertexCount(); vertex++) {
                int at = vertex * STRIDE;
                list.x(vertex, vertices[at]);
                list.y(vertex, vertices[at + 1]);
                list.z(vertex, vertices[at + 2]);
                list.u(vertex, vertices[at + 3]);
                list.v(vertex, vertices[at + 4]);
                list.normalX(vertex, vertices[at + 5]);
                list.normalY(vertex, vertices[at + 6]);
                list.normalZ(vertex, vertices[at + 7]);
                list.r(vertex, 1F);
                list.g(vertex, 1F);
                list.b(vertex, 1F);
                list.a(vertex, 1F);
                list.light(vertex, 0);
                list.overlay(vertex, OverlayTexture.NO_OVERLAY);
            }
        }

        /^*
         * The one shared sequence, never a fresh lambda.
         *
         * <p>{@code IndexPool} keys its runs on the sequence's <em>identity</em> and looks a mesh's offset
         * back up by that same identity. A new object per call misses the lookup, and the mesh draws from
         * whatever offset the miss returns: geometry that is partly or wholly absent.
         ^/
        @Override
        public IndexSequence indexSequence() {
            return SEQUENTIAL;
        }

        @Override
        public int indexCount() {
            return geometry.indices().length;
        }

        /^*
         * A sphere around the lump's own vertices. The engine culls against it, and a machine model
         * sits well outside its block, so deriving it from the geometry rather than assuming a unit
         * cube is what keeps a tower from vanishing when its block leaves the frustum.
         ^/
        @Override
        public Vector4fc boundingSphere() {
            float[] vertices = geometry.vertices();
            int count = geometry.vertexCount();
            if (count == 0) return new Vector4f(0F, 0F, 0F, 0F);
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (int vertex = 0; vertex < count; vertex++) {
                int at = vertex * STRIDE;
                minX = Math.min(minX, vertices[at]);
                maxX = Math.max(maxX, vertices[at]);
                minY = Math.min(minY, vertices[at + 1]);
                maxY = Math.max(maxY, vertices[at + 1]);
                minZ = Math.min(minZ, vertices[at + 2]);
                maxZ = Math.max(maxZ, vertices[at + 2]);
            }
            float centreX = (minX + maxX) * 0.5F;
            float centreY = (minY + maxY) * 0.5F;
            float centreZ = (minZ + maxZ) * 0.5F;
            float radius = 0F;
            for (int vertex = 0; vertex < count; vertex++) {
                int at = vertex * STRIDE;
                float dx = vertices[at] - centreX;
                float dy = vertices[at + 1] - centreY;
                float dz = vertices[at + 2] - centreZ;
                radius = Math.max(radius, dx * dx + dy * dy + dz * dz);
            }
            return new Vector4f(centreX, centreY, centreZ, (float) Math.sqrt(radius));
        }
    }

    private record Key(String name, Surface surface) {}

    private record Built(Geometry geometry, Model model) {}
}
*///?}
