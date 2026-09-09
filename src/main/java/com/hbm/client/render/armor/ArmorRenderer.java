package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.render.HumanoidPose;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.world.ArmorSlot;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * The worn body of one modelled armour set: an OBJ drawn over the wearer's skeleton.
 *
 * <p>Ported from 1.12.2's {@code ModelArmorBase} + {@code ModelRendererObj}. The transform in
 * {@link #submitPart} is that class's {@code render(float)} verbatim, and its one non-obvious property
 * is worth stating plainly: <b>the geometry is never moved to the bone</b>. An armour OBJ is authored in
 * absolute biped space (helmet around y ≈ −8…1, boots around y ≈ 18…24, sixteen units to the block), so
 * the bone's pivot is used only as a <i>centre of rotation</i> — translate to it, rotate, translate
 * straight back. Move the geometry as well and every limb ends up at twice its offset.
 *
 * <p>Crouching changes each live bone pivot, but the OBJ remains authored around the fixed rest pivots.
 * The old {@code ModelRendererObj} therefore translated to the live pivot and back from the base pivot;
 * using the live pivot in both directions cancels the crouch translation and detaches the armour.
 */
public abstract class ArmorRenderer {

    /** Vanilla model space is sixteen units to the block; OBJ vertices are authored in those units. */
    private static final float UNIT = 1F / 16F;

    protected enum Pivot {
        HEAD(0F, 0F, 0F, 1F), BODY(0F, 0F, 0F, 0F),
        LEFT_ARM(5F, 2F, 0F, 2F), RIGHT_ARM(-5F, 2F, 0F, 2F),
        LEFT_LEG(1.9F, 12F, 0F, 9F), RIGHT_LEG(-1.9F, 12F, 0F, 9F);

        private final float x;
        private final float y;
        private final float z;
        private final float crouchY;

        Pivot(float x, float y, float z, float crouchY) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.crouchY = crouchY;
        }
    }

    /** Draws the piece worn in {@code slot}. Slots this suit does not model simply return. */
    public abstract void submit(ArmorSlot slot, PoseStack pose, SubmitSink sink, int light, int overlay,
            HumanoidPose parts);

    /**
     * Submits one named OBJ part, posed by one of the wearer's bones.
     *
     * <p>{@code ModelRendererObj.render} in modern form: rotate about the bone's pivot without moving
     * the geometry there, then scale out of model units. Rotation order is Z, Y, X — vanilla's, and
     * changing it visibly twists limbs at the extremes of the walk cycle.
     */
    protected static void submitPart(SubmitSink sink, PoseStack pose, RenderType type, WavefrontObj model,
            String part, ModelPart bone, Pivot pivot, boolean crouching, int light, int overlay) {

        if (!bone.visible) return;

        pose.pushPose();

        pose.translate(bone.x * UNIT, (crouching ? pivot.crouchY : bone.y) * UNIT, bone.z * UNIT);
        if (bone.zRot != 0F) pose.mulPose(Axis.ZP.rotation(bone.zRot));
        if (bone.yRot != 0F) pose.mulPose(Axis.YP.rotation(bone.yRot));
        if (bone.xRot != 0F) pose.mulPose(Axis.XP.rotation(bone.xRot));
        pose.translate(-pivot.x * UNIT, -pivot.y * UNIT, -pivot.z * UNIT);

        pose.scale(UNIT, UNIT, UNIT);

        sink.submitCustomGeometry(pose, type,
                (drawPose, buffer) -> model.renderPart(part, drawPose, buffer, light, overlay));

        pose.popPose();
    }

    /**
     * Submits a model rooted at a humanoid bone, preserving ModelRenderer's rooted-model transform.
     * Unlike an absolute armour part, the model's vertices are local to the bone and the pivot is not
     * translated back after rotation. This is the transform used by the legacy ModelJetPack root.
     */
    public static void submitRootedModel(SubmitSink sink, PoseStack pose, RenderType type, WavefrontObj model,
            ModelPart bone, int light, int overlay) {
        if (!bone.visible) return;
        pose.pushPose();
        pose.translate(bone.x * UNIT, bone.y * UNIT, bone.z * UNIT);
        if (bone.zRot != 0F) pose.mulPose(Axis.ZP.rotation(bone.zRot));
        if (bone.yRot != 0F) pose.mulPose(Axis.YP.rotation(bone.yRot));
        if (bone.xRot != 0F) pose.mulPose(Axis.XP.rotation(bone.xRot));
        pose.scale(UNIT, UNIT, UNIT);
        sink.submitCustomGeometry(pose, type,
                (drawPose, buffer) -> model.renderAll(drawPose, buffer, light, overlay));
        pose.popPose();
    }
}
