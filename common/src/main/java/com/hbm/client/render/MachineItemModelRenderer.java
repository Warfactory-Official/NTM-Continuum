package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Renders a HBM machine's OBJ model as its inventory/hand item, via 26.1's special-model system (the
 * modern replacement for {@code ItemStackTileEntityRenderer}/BEWLR). Data-driven: the obj/texture and
 * a scale/yaw/vertical offset come from the item-model JSON, so the icon's framing can be tuned without
 * recompiling. Reuses {@link WavefrontObj} and the same {@code submitCustomGeometry} path as the
 * in-world {@link MachineObjRenderer}.
 */
public class MachineItemModelRenderer implements NoDataSpecialModelRenderer {

    /** Registration id of this special-model codec (referenced by {@code "type"} in the item JSON). */
    public static final Identifier ID = Identifier.fromNamespaceAndPath("hbm", "machine_obj");

    private final WavefrontObj model;
    private final RenderType renderType;
    private final float scale;
    private final float yaw;
    private final float ty;
    private final Set<String> exclude;
    /** Optional second texture + the parts drawn with it (e.g. a tank body vs its frame); may be null. */
    private final @Nullable RenderType overlayType;
    private final List<String> overlayParts;
    /** Parts drawn with an extra X-axis rotation about {@link #rotatePivot} (e.g. the mining laser barrel,
     * so the item icon poses it like the placed machine); rendered separately from the static passes. */
    private final List<String> rotateParts;
    private final float rotateX;
    private final Vector3f rotatePivot;
    /** Multi-texture mode ({@code "mtl": true}): material name → its render type, from the OBJ's .mtl. */
    private java.util.Map<String, RenderType> materialTypes = java.util.Map.of();

    private MachineItemModelRenderer(WavefrontObj model, RenderType renderType, float scale, float yaw, float ty,
                                     Set<String> exclude, @Nullable RenderType overlayType, List<String> overlayParts,
                                     List<String> rotateParts, float rotateX, Vector3f rotatePivot) {
        this.model = model;
        this.renderType = renderType;
        this.scale = scale;
        this.yaw = yaw;
        this.ty = ty;
        this.overlayType = overlayType;
        this.overlayParts = overlayParts;
        this.rotateParts = rotateParts;
        this.rotateX = rotateX;
        this.rotatePivot = rotatePivot;
        // The overlay parts are textured separately, so the base pass must skip them. Rotated parts are
        // also drawn in their own pass, so exclude them from both static passes.
        Set<String> merged = new java.util.HashSet<>(exclude);
        if (overlayType != null && !overlayParts.isEmpty()) merged.addAll(overlayParts);
        merged.addAll(rotateParts);
        this.exclude = merged;
    }

    /** Whether {@code part} is drawn in the separate rotated pass (so the static passes must skip it). */
    private boolean isRotated(String part) {
        return rotateParts.contains(part);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        poseStack.pushPose();
        applyTransform(poseStack);
        if (!materialTypes.isEmpty()) {
            for (var e : materialTypes.entrySet()) {
                String mat = e.getKey();
                collector.submitCustomGeometry(poseStack, e.getValue(),
                        (pose, buffer) -> model.renderMaterial(mat, pose, buffer, lightCoords, overlayCoords));
            }
            poseStack.popPose();
            return;
        }
        collector.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderExcept(pose, buffer, lightCoords, overlayCoords, exclude));
        if (overlayType != null && !overlayParts.isEmpty()) {
            collector.submitCustomGeometry(poseStack, overlayType, (pose, buffer) -> {
                for (String part : overlayParts) if (!isRotated(part)) model.renderPart(part, pose, buffer, lightCoords, overlayCoords);
            });
        }

        // Rotated parts (e.g. the mining laser barrel): the same X-rotation the placed machine applies,
        // about the connection pivot, so the item icon poses like the in-world model.
        if (!rotateParts.isEmpty()) {
            RenderType type = overlayType != null ? overlayType : renderType;
            poseStack.pushPose();
            poseStack.translate(rotatePivot.x, rotatePivot.y, rotatePivot.z);
            poseStack.mulPose(Axis.XP.rotationDegrees(rotateX));
            poseStack.translate(-rotatePivot.x, -rotatePivot.y, -rotatePivot.z);
            collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                for (String part : rotateParts) model.renderPart(part, pose, buffer, lightCoords, overlayCoords);
            });
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        PoseStack ps = new PoseStack();
        applyTransform(ps);
        Matrix4f m = ps.last().pose();
        float[] mn = model.boundsMin();
        float[] mx = model.boundsMax();
        for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
            Vector3f v = new Vector3f(xi == 0 ? mn[0] : mx[0], yi == 0 ? mn[1] : mx[1], zi == 0 ? mn[2] : mx[2]);
            m.transformPosition(v);
            output.accept(v);
        }
    }

    private void applyTransform(PoseStack poseStack) {
        // The base model (block/block) frames content authored in 0..1 block space, centred at 0.5. The
        // OBJ has an arbitrary origin (often a corner), so: move its bounding-box centre to the block
        // centre (0.5,0.5,0.5), then rotate/scale, then re-centre the model on its own bounds. Without
        // the final translate the model's corner — not its middle — sits at the slot centre.
        float[] mn = model.boundsMin();
        float[] mx = model.boundsMax();
        float cx = (mn[0] + mx[0]) * 0.5F;
        float cy = (mn[1] + mx[1]) * 0.5F;
        float cz = (mn[2] + mx[2]) * 0.5F;
        poseStack.translate(0.5F, 0.5F + ty, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-cx, -cy, -cz);
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
            RenderType overlay = overlayTexture.map(RenderTypes::entityCutout).orElse(null);
            WavefrontObj loaded = WavefrontObj.load(obj);
            Vector3f pivot = rotatePivot.size() == 3
                    ? new Vector3f(rotatePivot.get(0), rotatePivot.get(1), rotatePivot.get(2))
                    : new Vector3f();
            MachineItemModelRenderer renderer = new MachineItemModelRenderer(loaded, RenderTypes.entityCutout(texture), scale, yaw, ty,
                    Set.copyOf(exclude), overlay, List.copyOf(overlayParts), List.copyOf(rotateParts), rotateX, pivot);
            if (mtl) {
                // Multi-texture OBJ: resolve per-material textures from the .mtl next to the model.
                var lib = WavefrontObj.loadMtl(Identifier.fromNamespaceAndPath(obj.getNamespace(), obj.getPath().replace(".obj", ".mtl")));
                java.util.Map<String, RenderType> types = new java.util.LinkedHashMap<>();
                for (String mat : loaded.materialNames()) {
                    Identifier tex = lib.get(mat);
                    if (tex != null) types.put(mat, RenderTypes.entityCutout(tex));
                }
                renderer.materialTypes = types;
            }
            return renderer;
        }
    }
}
