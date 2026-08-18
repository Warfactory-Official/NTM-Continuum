package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

//? if >=26.1 {
import net.minecraft.client.renderer.rendertype.RenderType;
//?} else {
/*import net.minecraft.client.renderer.rendertype.RenderType;
*///?}

/**
 * A machine's OBJ model posed as an inventory/hand item: the geometry, the render types, and the
 * framing transform, with nothing version-specific in it.
 *
 * <p>The two eras disagree only about <em>who calls this</em>. 26.1 has a special-model system, so
 * {@link MachineItemModelRenderer} wraps this in a {@code NoDataSpecialModelRenderer} and its config
 * arrives through a codec. Older versions have no such system: {@code MachineItemModels} parses the same
 * item-definition JSON itself and a per-loader BEWLR draws it. Both paths end here, submitting through
 * {@link SubmitSink} — the same facade the in-world {@link MachineObjRenderer} uses — so the item icon
 * and the placed machine cannot drift apart.
 */
public final class MachineItemModel {

    private final WavefrontObj model;
    private final RenderType renderType;
    private final float scale;
    private final float yaw;
    /**
     * Rotation about X, applied before {@link #yaw}. Zero for machines; the armour suits need 180
     * because their OBJs are authored in the biped's flipped space (the helmet sits at negative Y),
     * which is the same {@code rotate(180, 1, 0, 0)} 1.12's {@code setupRenderInv} opened with.
     */
    private final float pitch;
    private final float ty;
    private final Set<String> exclude;
    /** The caller's exclusions only — what the icon is framed on. See the constructor. */
    private final Set<String> userExclude;
    /** Optional second texture + the parts drawn with it (e.g. a tank body vs its frame); may be null. */
    private final @Nullable RenderType overlayType;
    private final List<String> overlayParts;
    /** Parts drawn with an extra X-axis rotation about {@link #rotatePivot} (e.g. the mining laser barrel,
     * so the item icon poses it like the placed machine); rendered separately from the static passes. */
    private final List<String> rotateParts;
    private final float rotateX;
    private final Vector3f rotatePivot;
    /** Multi-texture mode ({@code "mtl": true}): material name → its render type, from the OBJ's .mtl. */
    private final Map<String, RenderType> materialTypes;

    public MachineItemModel(WavefrontObj model, RenderType renderType, float scale, float yaw, float pitch, float ty,
                            Set<String> exclude, @Nullable RenderType overlayType, List<String> overlayParts,
                            List<String> rotateParts, float rotateX, Vector3f rotatePivot,
                            Map<String, RenderType> materialTypes) {
        this.model = model;
        this.renderType = renderType;
        this.scale = scale;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ty = ty;
        this.overlayType = overlayType;
        this.overlayParts = overlayParts;
        this.rotateParts = rotateParts;
        this.rotateX = rotateX;
        this.rotatePivot = rotatePivot;
        this.materialTypes = materialTypes;
        // Framing is centred on what is actually drawn, which is everything except the caller's own
        // exclusions — not the merged set below, since overlay and rotated parts are still on screen.
        this.userExclude = Set.copyOf(exclude);
        // The overlay parts are textured separately, so the base pass must skip them. Rotated parts are
        // also drawn in their own pass, so exclude them from both static passes.
        Set<String> merged = new HashSet<>(exclude);
        if (overlayType != null && !overlayParts.isEmpty()) merged.addAll(overlayParts);
        merged.addAll(rotateParts);
        this.exclude = merged;
    }

    /** Whether {@code part} is drawn in the separate rotated pass (so the static passes must skip it). */
    private boolean isRotated(String part) {
        return rotateParts.contains(part);
    }

    /** Draws the model, framed for an item slot. The pose is left as it was found. */
    public void submit(PoseStack poseStack, SubmitSink sink, int lightCoords, int overlayCoords) {
        poseStack.pushPose();
        applyTransform(poseStack);
        if (!materialTypes.isEmpty()) {
            for (Map.Entry<String, RenderType> e : materialTypes.entrySet()) {
                String mat = e.getKey();
                sink.submitCustomGeometry(poseStack, e.getValue(),
                        (pose, buffer) -> model.renderMaterial(mat, pose, buffer, lightCoords, overlayCoords));
            }
            poseStack.popPose();
            return;
        }
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderExcept(pose, buffer, lightCoords, overlayCoords, exclude));
        if (overlayType != null && !overlayParts.isEmpty()) {
            sink.submitCustomGeometry(poseStack, overlayType, (pose, buffer) -> {
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
            sink.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
                for (String part : rotateParts) model.renderPart(part, pose, buffer, lightCoords, overlayCoords);
            });
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The eight transformed corners of the model's bounding box (26.1 uses these to size the icon). */
    public void extents(Consumer<Vector3fc> output) {
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
        float[][] bounds = model.boundsExcluding(userExclude);
        float[] mn = bounds[0];
        float[] mx = bounds[1];
        float cx = (mn[0] + mx[0]) * 0.5F;
        float cy = (mn[1] + mx[1]) * 0.5F;
        float cz = (mn[2] + mx[2]) * 0.5F;
        poseStack.translate(0.5F, 0.5F + ty, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        if (pitch != 0F) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-cx, -cy, -cz);
    }
}
