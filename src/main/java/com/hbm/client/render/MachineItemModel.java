package com.hbm.client.render;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.SubmitSink;
import net.minecraft.world.item.ItemDisplayContext;
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
    /** Uses the literal legacy item transform instead of the block-space framing convention. */
    private final boolean rawLegacyTransform;
    /** Nested {@code minecraft:special} models retain the block-item framing convention. */
    private final boolean blockFramed;
    private final float yaw;
    /**
     * Rotation about X, applied before {@link #yaw}. Zero for machines; the armour suits need 180
     * because their OBJs are authored in the biped's flipped space (the helmet sits at negative Y),
     * which is the same {@code rotate(180, 1, 0, 0)} 1.12's {@code setupRenderInv} opened with.
     */
    private final float pitch;
    private final float ty;
    /** Legacy item translation applied in item space, before the model's facing rotation. */
    private final Vector3f itemTranslate;
    /** Legacy pre-translation, rotated by yaw but deliberately not scaled. */
    private final Vector3f itemPreTranslate;
    /** Extra transform applied only by the legacy inventory/GUI path. */
    private final Vector3f guiTranslate;
    private final float guiScale;
    private final float guiYaw;
    private final Set<String> exclude;
    /** The caller's exclusions only — what the icon is framed on. See the constructor. */
    private final Set<String> userExclude;
    /** Optional second texture + the parts drawn with it (e.g. a tank body vs its frame); may be null. */
    private final @Nullable RenderType overlayType;
    private final List<String> overlayParts;
    /** An opaque pass for parts whose legacy item renderer drew with texture sampling disabled. */
    private final @Nullable RenderType blackType;
    private final List<String> blackParts;
    /** Parts drawn with an extra X-axis rotation about {@link #rotatePivot} (e.g. the mining laser barrel,
     * so the item icon poses it like the placed machine); rendered separately from the static passes. */
    private final List<String> rotateParts;
    private final float rotateX;
    private final Vector3f rotatePivot;
    /** Parts that use the legacy inventory spin around {@link #spinPivot}. */
    private final List<String> spinParts;
    private final Vector3f spinPivot;
    /** Multi-texture mode ({@code "mtl": true}): material name → its render type, from the OBJ's .mtl. */
    private final Map<String, RenderType> materialTypes;
    /** Explicit OBJ-part texture overrides for models whose sockets do not use an .mtl file. */
    private final Map<String, RenderType> partTypes;
    /** Whether this item has the old continuously rotating inventory pose. */
    private final boolean spin;
    private final Map<String, ContextTransform> contextTransforms;
    /** Optional named item animation. The pump animation keeps all moving parts on one rotor angle. */
    private final String animation;

    /** A legacy renderer adjustment applied after the context display matrix and before the model pose. */
    public record ContextTransform(Vector3f rotation, Vector3f translation, float scale) {
        public void apply(PoseStack poseStack) {
            if (rotation.x() != 0F) poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x()));
            if (rotation.y() != 0F) poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y()));
            if (rotation.z() != 0F) poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z()));
            poseStack.translate(translation.x(), translation.y(), translation.z());
            if (scale != 1F) poseStack.scale(scale, scale, scale);
        }
    }

    public MachineItemModel(WavefrontObj model, RenderType renderType, float scale, boolean rawLegacyTransform,
                            boolean blockFramed,
                            float yaw, float pitch, float ty,
                            Vector3f itemTranslate, Vector3f itemPreTranslate, Vector3f guiTranslate, float guiScale,
                            float guiYaw, Set<String> exclude,
                            @Nullable RenderType overlayType, List<String> overlayParts,
                            @Nullable RenderType blackType, List<String> blackParts,
                             List<String> rotateParts, float rotateX, Vector3f rotatePivot,
                             List<String> spinParts, Vector3f spinPivot, Map<String, RenderType> materialTypes,
                             Map<String, RenderType> partTypes,
                             boolean spin, Map<String, ContextTransform> contextTransforms, String animation) {
        this.model = model;
        this.renderType = renderType;
        this.scale = scale;
        this.rawLegacyTransform = rawLegacyTransform;
        this.blockFramed = blockFramed;
        this.yaw = yaw;
        this.pitch = pitch;
        this.ty = ty;
        this.itemTranslate = itemTranslate;
        this.itemPreTranslate = itemPreTranslate;
        this.guiTranslate = guiTranslate;
        this.guiScale = guiScale;
        this.guiYaw = guiYaw;
        this.overlayType = overlayType;
        this.overlayParts = overlayParts;
        this.blackType = blackType;
        this.blackParts = blackParts;
        this.rotateParts = rotateParts;
        this.rotateX = rotateX;
        this.rotatePivot = rotatePivot;
        this.spinParts = spinParts;
        this.spinPivot = spinPivot;
        this.materialTypes = materialTypes;
        this.partTypes = Map.copyOf(partTypes);
        this.spin = spin;
        this.contextTransforms = Map.copyOf(contextTransforms);
        this.animation = animation;
        // Framing is centred on what is actually drawn, which is everything except the caller's own
        // exclusions — not the merged set below, since overlay and rotated parts are still on screen.
        this.userExclude = Set.copyOf(exclude);
        // The overlay parts are textured separately, so the base pass must skip them. Rotated parts are
        // also drawn in their own pass, so exclude them from both static passes.
        Set<String> merged = new HashSet<>(exclude);
        if (overlayType != null && !overlayParts.isEmpty()) merged.addAll(overlayParts);
        if (blackType != null && !blackParts.isEmpty()) merged.addAll(blackParts);
        merged.addAll(rotateParts);
        merged.addAll(spinParts);
        if (spinParts.contains("Stirling")) merged.addAll(List.of("Cog", "CogSmall", "Piston"));
        this.exclude = merged;
    }

    /** Whether {@code part} is drawn in the separate rotated pass (so the static passes must skip it). */
    private boolean isRotated(String part) {
        return rotateParts.contains(part);
    }

    /** Draws the model, framed for an item slot. The pose is left as it was found. */
    public void submit(PoseStack poseStack, SubmitSink sink, int lightCoords, int overlayCoords) {
        submit(poseStack, sink, lightCoords, overlayCoords, null);
    }

    /** Draws the model with the optional display context used by the context-aware item-model path. */
    public void submit(PoseStack poseStack, SubmitSink sink, int lightCoords, int overlayCoords,
                       @Nullable ItemDisplayContext context) {
        poseStack.pushPose();
        applyTransform(poseStack, context);
        if ("pump".equals(animation)) {
            submitPumpAnimation(poseStack, sink, lightCoords, overlayCoords);
            poseStack.popPose();
            return;
        }
        if (!partTypes.isEmpty()) {
            for (Map.Entry<String, RenderType> entry : partTypes.entrySet()) {
                if (userExclude.contains(entry.getKey())) continue;
                sink.submitCustomGeometry(poseStack, entry.getValue(),
                        (pose, buffer) -> model.renderPart(entry.getKey(), pose, buffer, lightCoords, overlayCoords));
            }
            poseStack.popPose();
            return;
        }
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
        if (blackType != null && !blackParts.isEmpty()) {
            sink.submitCustomGeometry(poseStack, blackType, (pose, buffer) -> {
                for (String part : blackParts) if (!isRotated(part)) {
                    model.renderPart(part, pose, buffer, lightCoords, overlayCoords, 0xFF000000);
                }
            });
        }
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
        if (spin && spinParts.contains("Stirling")) {
            submitStirlingAnimation(poseStack, sink, lightCoords, overlayCoords);
        } else if (spin && !spinParts.isEmpty()) {
            poseStack.pushPose();
            spinAround(poseStack, spinPivot, System.currentTimeMillis() % 3600L * 0.1F);
            sink.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
                for (String part : spinParts) if (!userExclude.contains(part)) {
                    model.renderPart(part, pose, buffer, lightCoords, overlayCoords);
                }
            });
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** The eight transformed corners of the model's bounding box (26.1 uses these to size the icon). */
    public void extents(Consumer<Vector3fc> output) {
        extents(output, null);
    }

    /** The transformed corners for a display context. */
    public void extents(Consumer<Vector3fc> output, @Nullable ItemDisplayContext context) {
        PoseStack ps = new PoseStack();
        applyTransform(ps, context);
        if ("pump".equals(animation)) {
            pumpExtents(ps, output);
            return;
        }
        if (!partTypes.isEmpty()) {
            Set<String> included = new HashSet<>(partTypes.keySet());
            included.removeAll(userExclude);
            emitPartBounds(ps, included, output);
            return;
        }
        if (!materialTypes.isEmpty()) {
            emitBounds(ps, model.boundsIncludingMaterials(materialTypes.keySet()), output);
            return;
        }

        emitBounds(ps, model.boundsExcludingExact(exclude), output);
        emitPartBounds(ps, blackParts.stream().filter(part -> !isRotated(part)).collect(java.util.stream.Collectors.toSet()), output);
        emitPartBounds(ps, overlayParts.stream().filter(part -> !isRotated(part)).collect(java.util.stream.Collectors.toSet()), output);

        if (!rotateParts.isEmpty()) {
            ps.pushPose();
            ps.translate(rotatePivot.x, rotatePivot.y, rotatePivot.z);
            ps.mulPose(Axis.XP.rotationDegrees(rotateX));
            ps.translate(-rotatePivot.x, -rotatePivot.y, -rotatePivot.z);
            emitPartBounds(ps, Set.copyOf(rotateParts), output);
            ps.popPose();
        }
        if (spin && spinParts.contains("Stirling")) {
            stirlingExtents(ps, output);
        } else if (spin && !spinParts.isEmpty()) {
            Set<String> rendered = new HashSet<>(spinParts);
            rendered.removeAll(userExclude);
            ps.pushPose();
            spinAround(ps, spinPivot, System.currentTimeMillis() % 3600L * 0.1F);
            emitPartBounds(ps, rendered, output);
            ps.popPose();
        }
    }

    private void emitPartBounds(PoseStack poseStack, Set<String> parts, Consumer<Vector3fc> output) {
        emitBounds(poseStack, model.boundsIncluding(parts), output);
    }

    private void emitBounds(PoseStack poseStack, @Nullable float[][] bounds, Consumer<Vector3fc> output) {
        if (bounds == null) return;
        Matrix4f m = poseStack.last().pose();
        float[] mn = bounds[0];
        float[] mx = bounds[1];
        for (int xi = 0; xi < 2; xi++) for (int yi = 0; yi < 2; yi++) for (int zi = 0; zi < 2; zi++) {
            Vector3f v = new Vector3f(xi == 0 ? mn[0] : mx[0], yi == 0 ? mn[1] : mx[1], zi == 0 ? mn[2] : mx[2]);
            m.transformPosition(v);
            output.accept(v);
        }
    }

    private void pumpExtents(PoseStack poseStack, Consumer<Vector3fc> output) {
        emitPartBounds(poseStack, Set.of("Base"), output);
        float rotation = System.currentTimeMillis() % 3600L * 0.1F;

        poseStack.pushPose();
        poseStack.translate(0F, 2.25F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation - 90F));
        poseStack.translate(0F, -2.25F, 0F);
        emitPartBounds(poseStack, Set.of("Rotor"), output);
        poseStack.popPose();

        double sin = Math.sin(rotation * Math.PI / 180D) * 0.5D - 0.5D;
        double cos = Math.cos(rotation * Math.PI / 180D) * 0.5D;
        double angle = Math.acos(cos / 2D) * 180D / Math.PI - 90D;
        double lift = 1D - Math.sqrt(1D + (cos * cos) / 2D) + sin;

        poseStack.pushPose();
        poseStack.translate(0D, lift, 0D);
        poseStack.translate(0D, 4.75D, 0D);
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) angle));
        poseStack.translate(0D, -4.75D, 0D);
        emitPartBounds(poseStack, Set.of("Arms"), output);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0D, lift, 0D);
        emitPartBounds(poseStack, Set.of("Piston"), output);
        poseStack.popPose();
    }

    private void stirlingExtents(PoseStack poseStack, Consumer<Vector3fc> output) {
        float rotation = System.currentTimeMillis() % 3600L * 0.1F;
        if (!userExclude.contains("Cog")) {
            poseStack.pushPose();
            spinAround(poseStack, new Vector3f(0F, 1.375F, 0F), -rotation);
            emitPartBounds(poseStack, Set.of("Cog"), output);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0F, 1.375F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 2F + 3F));
        poseStack.translate(0F, -1.375F, -0.25F);
        emitPartBounds(poseStack, Set.of("CogSmall"), output);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(Math.sin(rotation * Math.PI / 90D) * 0.25D + 0.125D, 0F, 0F);
        emitPartBounds(poseStack, Set.of("Piston"), output);
        poseStack.popPose();
    }

    /** The transformed corners for an item-model layer's bounds supplier. */
    public Vector3fc[] extents(@Nullable ItemDisplayContext context) {
        java.util.List<Vector3fc> points = new java.util.ArrayList<>(8);
        extents(points::add, context);
        return points.toArray(Vector3fc[]::new);
    }

    private void applyTransform(PoseStack poseStack, @Nullable ItemDisplayContext context) {
        // The old item renderer inserted its inventory adjustment after the perspective matrix but before
        // renderCommon. The context-aware path has already applied that perspective matrix to the pose.
        ContextTransform contextTransform = context == null || context == ItemDisplayContext.NONE ? null
                : context == ItemDisplayContext.GUI ? contextTransforms.get("gui")
                : context == ItemDisplayContext.GROUND ? contextTransforms.get("ground")
                : contextTransforms.get("non_gui");
        if (contextTransform != null) {
            contextTransform.apply(poseStack);
        } else if (context == ItemDisplayContext.GUI) {
            poseStack.translate(guiTranslate.x, guiTranslate.y, guiTranslate.z);
            poseStack.scale(guiScale, guiScale, guiScale);
            poseStack.mulPose(Axis.YP.rotationDegrees(guiYaw));
        }

        if (rawLegacyTransform || !blockFramed) {
            // Direct context-aware models and explicit raw callbacks both reproduce the old OBJ item
            // geometry. Do not add the block centre or recenter on OBJ bounds: legacy models deliberately
            // used their authored coordinates, with the display matrix supplying the outer item frame.
            poseStack.translate(itemTranslate.x, ty + itemTranslate.y, itemTranslate.z);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.translate(itemPreTranslate.x, itemPreTranslate.y, itemPreTranslate.z);
            if (pitch != 0F) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.scale(scale, scale, scale);
            return;
        }

        // The nested base model (block/block) frames content authored in 0..1 block space, centred at 0.5. The
        // OBJ has an arbitrary origin (often a corner), so: move its bounding-box centre to the block
        // centre (0.5,0.5,0.5), then rotate/scale, then re-centre the model on its own bounds. Without
        // the final translate the model's corner — not its middle — sits at the slot centre.
        float[][] bounds = model.boundsExcluding(userExclude);
        float[] mn = bounds[0];
        float[] mx = bounds[1];
        float cx = (mn[0] + mx[0]) * 0.5F;
        float cy = (mn[1] + mx[1]) * 0.5F;
        float cz = (mn[2] + mx[2]) * 0.5F;
        poseStack.translate(0.5F + itemTranslate.x, 0.5F + ty + itemTranslate.y, 0.5F + itemTranslate.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        // The legacy bake rotates itemPreTranslate by yaw, but adds it after scaling the OBJ.
        poseStack.translate(itemPreTranslate.x, itemPreTranslate.y, itemPreTranslate.z);
        if (pitch != 0F) poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-cx, -cy, -cz);
    }

    /** The old pump item renderer moves every pump part from one shared millisecond-derived rotor angle. */
    private void submitPumpAnimation(PoseStack poseStack, SubmitSink sink, int lightCoords, int overlayCoords) {
        submitPart(poseStack, sink, "Base", lightCoords, overlayCoords);
        float rotation = System.currentTimeMillis() % 3600L * 0.1F;

        poseStack.pushPose();
        poseStack.translate(0F, 2.25F, 0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation - 90F));
        poseStack.translate(0F, -2.25F, 0F);
        submitPart(poseStack, sink, "Rotor", lightCoords, overlayCoords);
        poseStack.popPose();

        double sin = Math.sin(rotation * Math.PI / 180D) * 0.5D - 0.5D;
        double cos = Math.cos(rotation * Math.PI / 180D) * 0.5D;
        double angle = Math.acos(cos / 2D) * 180D / Math.PI - 90D;
        double cath = Math.sqrt(1D + (cos * cos) / 2D);
        double lift = 1D - cath + sin;

        poseStack.pushPose();
        poseStack.translate(0D, lift, 0D);
        poseStack.translate(0D, 4.75D, 0D);
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) angle));
        poseStack.translate(0D, -4.75D, 0D);
        submitPart(poseStack, sink, "Arms", lightCoords, overlayCoords);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0D, lift, 0D);
        submitPart(poseStack, sink, "Piston", lightCoords, overlayCoords);
        poseStack.popPose();
    }

    private void spinAround(PoseStack poseStack, Vector3f pivot, float angle) {
        // applyTransform has already centred the OBJ. Pivots remain in its original local coordinates.
        poseStack.translate(pivot.x, pivot.y, pivot.z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(angle));
        poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
    }

    /** The legacy Stirling inventory icon animates its three moving parts independently. */
    private void submitStirlingAnimation(PoseStack poseStack, SubmitSink sink, int lightCoords, int overlayCoords) {
        float rotation = System.currentTimeMillis() % 3600L * 0.1F;
        if (!userExclude.contains("Cog")) {
            poseStack.pushPose();
            spinAround(poseStack, new Vector3f(0F, 1.375F, 0F), -rotation);
            submitPart(poseStack, sink, "Cog", lightCoords, overlayCoords);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0F, 1.375F, 0.25F);
        poseStack.mulPose(Axis.XP.rotationDegrees(rotation * 2F + 3F));
        poseStack.translate(0F, -1.375F, -0.25F);
        submitPart(poseStack, sink, "CogSmall", lightCoords, overlayCoords);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(Math.sin(rotation * Math.PI / 90D) * 0.25D + 0.125D, 0F, 0F);
        submitPart(poseStack, sink, "Piston", lightCoords, overlayCoords);
        poseStack.popPose();
    }

    private void submitPart(PoseStack poseStack, SubmitSink sink, String part, int lightCoords, int overlayCoords) {
        sink.submitCustomGeometry(poseStack, renderType,
                (pose, buffer) -> model.renderPart(part, pose, buffer, lightCoords, overlayCoords));
    }
}
