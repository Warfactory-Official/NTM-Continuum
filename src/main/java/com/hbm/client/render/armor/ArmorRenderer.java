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
 * <p><b>One thing upstream did that is deliberately not ported:</b> {@code ModelArmorBase} carries a
 * table of hand-tuned crouch offsets ({@code head.offsetY = 4.24}, {@code body.offsetY = 3.425}, legs
 * {@code offsetZ = 4} / {@code rotationPointY = 12} …). Those exist because 1.12 crouched the player by
 * translating in {@code RenderPlayer}, outside the model, so an armour model had to reproduce the shift
 * itself. Modern {@code HumanoidModel.setupAnim} folds the crouch into the parts — {@code head.y += 4.2},
 * {@code body.y += 3.2}, arms {@code += 3.2}, legs shifted down and back — which are the same numbers.
 * Reading the live part therefore gives the crouch pose already applied, and re-adding the offsets would
 * apply it twice.
 */
public abstract class ArmorRenderer {

    /** Vanilla model space is sixteen units to the block; OBJ vertices are authored in those units. */
    private static final float UNIT = 1F / 16F;

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
            String part, ModelPart bone, int light, int overlay) {

        if (!bone.visible) return;

        pose.pushPose();

        pose.translate(bone.x * UNIT, bone.y * UNIT, bone.z * UNIT);
        if (bone.zRot != 0F) pose.mulPose(Axis.ZP.rotation(bone.zRot));
        if (bone.yRot != 0F) pose.mulPose(Axis.YP.rotation(bone.yRot));
        if (bone.xRot != 0F) pose.mulPose(Axis.XP.rotation(bone.xRot));
        pose.translate(-bone.x * UNIT, -bone.y * UNIT, -bone.z * UNIT);

        pose.scale(UNIT, UNIT, UNIT);

        sink.submitCustomGeometry(pose, type,
                (drawPose, buffer) -> model.renderPart(part, drawPose, buffer, light, overlay));

        pose.popPose();
    }
}
