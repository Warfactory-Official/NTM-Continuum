package com.hbm.client.render;

//? if >=26.1 {

import dev.golem.core.render.CompatRenderTypes;
import dev.golem.core.render.VanillaSubmitSink;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Renders a HBM machine's OBJ model as its inventory/hand item, via 26.1's special-model system (the
 * modern replacement for {@code ItemStackTileEntityRenderer}/BEWLR). Data-driven: the obj/texture and
 * a scale/yaw/vertical offset come from the item-model JSON, so the icon's framing can be tuned without
 * recompiling.
 *
 * <p>This class is only the 26.1 plumbing — the codec, and the bridge from vanilla's collector to
 * {@link dev.golem.core.render.SubmitSink}. The model itself lives in {@link MachineItemModel}, which
 * older versions drive from a BEWLR instead.
 */
public class MachineItemModelRenderer implements NoDataSpecialModelRenderer {

    /** Registration id of this special-model codec (referenced by {@code "type"} in the item JSON). */
    public static final Identifier ID = Identifier.fromNamespaceAndPath("hbm", "machine_obj");

    private final MachineItemModel model;

    private MachineItemModelRenderer(MachineItemModel model) {
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        model.submit(poseStack, new VanillaSubmitSink(collector), lightCoords, overlayCoords);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        model.extents(output);
    }

    /** The data-driven, codec-backed factory for this renderer (one entry per machine in the item JSON). */
    public record Unbaked(Identifier obj, Identifier texture, float scale, float yaw, float ty, List<String> exclude,
                          Optional<Identifier> overlayTexture, List<String> overlayParts, boolean mtl,
                          List<String> rotateParts, float rotateX, List<Float> rotatePivot) implements NoDataSpecialModelRenderer.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Identifier.CODEC.fieldOf("obj").forGetter(Unbaked::obj),
                Identifier.CODEC.optionalFieldOf("texture", Identifier.fromNamespaceAndPath("hbm", "textures/models/machines/lava_gray.png")).forGetter(Unbaked::texture),
                Codec.FLOAT.optionalFieldOf("scale", 0.28F).forGetter(Unbaked::scale),
                Codec.FLOAT.optionalFieldOf("yaw", 90.0F).forGetter(Unbaked::yaw),
                Codec.FLOAT.optionalFieldOf("ty", 0.0F).forGetter(Unbaked::ty),
                Codec.STRING.listOf().optionalFieldOf("exclude", List.of()).forGetter(Unbaked::exclude),
                Identifier.CODEC.optionalFieldOf("overlayTexture").forGetter(Unbaked::overlayTexture),
                Codec.STRING.listOf().optionalFieldOf("overlayParts", List.of()).forGetter(Unbaked::overlayParts),
                Codec.BOOL.optionalFieldOf("mtl", false).forGetter(Unbaked::mtl),
                Codec.STRING.listOf().optionalFieldOf("rotateParts", List.of()).forGetter(Unbaked::rotateParts),
                Codec.FLOAT.optionalFieldOf("rotateX", 0.0F).forGetter(Unbaked::rotateX),
                Codec.FLOAT.listOf().optionalFieldOf("rotatePivot", List.of()).forGetter(Unbaked::rotatePivot)
        ).apply(i, Unbaked::new));

        @Override
        public MapCodec<? extends NoDataSpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            RenderType overlay = overlayTexture.map(CompatRenderTypes::entityCutout).orElse(null);
            WavefrontObj loaded = WavefrontObj.load(obj);
            Vector3f pivot = rotatePivot.size() == 3
                    ? new Vector3f(rotatePivot.get(0), rotatePivot.get(1), rotatePivot.get(2))
                    : new Vector3f();
            java.util.Map<String, RenderType> types = java.util.Map.of();
            if (mtl) {
                // Multi-texture OBJ: resolve per-material textures from the .mtl next to the model.
                var lib = WavefrontObj.loadMtl(Identifier.fromNamespaceAndPath(obj.getNamespace(), obj.getPath().replace(".obj", ".mtl")));
                java.util.Map<String, RenderType> resolved = new java.util.LinkedHashMap<>();
                for (String mat : loaded.materialNames()) {
                    Identifier tex = lib.get(mat);
                    if (tex != null) resolved.put(mat, CompatRenderTypes.entityCutout(tex));
                }
                types = resolved;
            }
            return new MachineItemModelRenderer(new MachineItemModel(loaded, CompatRenderTypes.entityCutout(texture),
                    scale, yaw, ty, Set.copyOf(exclude), overlay, List.copyOf(overlayParts),
                    List.copyOf(rotateParts), rotateX, pivot, types));
        }
    }
}
//?}
