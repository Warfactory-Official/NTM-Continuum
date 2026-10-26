package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.items.weapon.GunB92;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.SubmitSink;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * 1.7 {@code ItemRenderGunAnim} drawing {@code ModelB92}: a plain item renderer, so the first-person body is
 * drawn in the engine's hand slot, not in camera space like the Sedna guns.
 */
public final class B92WeaponRenderer extends WeaponRenderer {

    private static final Surface BODY = Surface.cutout("hbm:textures/models/weapons/b92.png");

    /** Name, rotation point and Z rotation in degrees, in 1.7's render order; the pumps slide with the reload. */
    private record Part(String name, float x, float y, float z, float roll) {}

    private static final Part[] PARTS = {
            new Part("Muzzle1", -24F, 0.5F, -1F, 0F), new Part("Barrel1", -24F, 1F, -1.5F, 0F),
            new Part("Barrel2", -22F, 0.5F, -1F, 0F), new Part("Grip", -20F, 3F, -2F, 0F),
            new Part("Front1", -22F, 0.5F, -2F, 0F), new Part("Front2", -22F, 0F, -1.5F, 0F),
            new Part("Body", 0F, 0.5F, -2F, 0F), new Part("Top", 0F, 0F, -1.5F, 0F),
            new Part("GripBottom", -18F, 5.5F, -1F, 0F), new Part("Handle", 6F, 7F, -2F, -13F),
            new Part("HandleBack", 6F, 7F, -1.5F, -13F), new Part("Frame1", 0.5F, 11F, -1F, 0F),
            new Part("Frame2", -2F, 6.5F, -1F, 0F), new Part("Frame3", -2F, 10.5F, -1F, 30F),
            new Part("Trigger", 4F, 7F, -0.5F, 11F), new Part("BackPlate1", 15F, 0F, -1.5F, -30F),
            new Part("Back", 15F, 3.5F, -2F, 0F), new Part("BackPlate2", 15F, 0.5F, -2F, -28F),
            new Part("Pump1", 10F, 1F, -1F, 0F), new Part("Pump2", 17F, 0.5F, -3.5F, 0F),
            new Part("BodyPlate", 1.5F, 2F, -2.5F, 0F)};

    @Override
    public void submitFirstPerson(ItemStack stack, PoseStack pose, SubmitSink collector, int light,
            int overlay, boolean otherArm) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-135F));
        pose.translate(-0.5F, 0F, -0.2F);
        pose.scale(0.25F, 0.25F, 0.25F);
        pose.translate(-0.2F, -0.1F, -0.1F);
        renderFirstPerson(stack, pose, collector, light, overlay,
                otherArm ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        pose.popPose();
    }

    /** The reload tilt is first person only and sits outside the model's 1/16 scale, in blocks. */
    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        Gem.Part<WeaponPose> root = declaration.joint()
                .angle(Gem.Axes.Z, state -> state.animated ? -GunB92.getRotationFromAnim(state.stack) * 90F : 0F)
                .slide(Gem.Axes.X, state -> state.animated ? -GunB92.getRotationFromAnim(state.stack) : 0F)
                .slide(Gem.Axes.Y, state -> state.animated ? -GunB92.getRotationFromAnim(state.stack) : 0F)
                .scale(0.0625F);

        for (Part part : PARTS) {
            Gem.Part<WeaponPose> joint = root.joint();
            // ModelRenderer.offsetX is applied before the scale, so the pump's slide is in blocks
            if (part.name.startsWith("Pump")) joint.slide(Gem.Axes.X, state -> GunB92.getTransFromAnim(state.stack) * 16F);
            joint.translate(part.x, part.y, part.z);
            if (part.roll != 0F) joint.rotate(Gem.Axes.Z, part.roll);
            joint.child(B92BoxGeometry.part(part.name), BODY);
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        pose.mulPose(Axis.ZP.rotationDegrees(-200F));
        pose.mulPose(Axis.YP.rotationDegrees(75F));
        pose.mulPose(Axis.XP.rotationDegrees(-30F));
        pose.translate(0F, -0.2F, -0.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(-5F));
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(-0.3F, -0.4F, 0.15F);
    }

    @Override
    protected void setupEntity(PoseStack pose, ItemStack stack) {
        setupThirdPerson(pose, stack);
    }
}
