package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ntm-next's ItemRenderDebug, including its lilmac mesh and reload hierarchy. */
public final class DebugWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("debug_gun");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("lilmac");
        return model;
    }

    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }

    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.0F * offset, -0.75F * offset, offset,
                0, -3.875 / 8D, 0);
    }

    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.parts("Gun", "Cylinder", "Bullets", "Casings", "Pivot", "Hammer"), BODY)
                .visibleIf(state -> !state.animated);

        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.125F)
                .rotate(Gem.Axes.Y, 90F)
                .angle(Gem.Axes.Z, state -> (float) state.bus("ROTATE")[0])
                .slide(Gem.Axes.X, state -> aimLerp(0, -state.bus("RECOIL")[2]))
                .slide(Gem.Axes.Z, state -> aimLerp(state.bus("RECOIL")[2], 0))
                .angle(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2] * 10F)
                .angle(Gem.Axes.Z, state -> (float) state.bus("RELOAD_LIFT")[0])
                .slide(Gem.Axes.X, state -> (float) state.bus("RELOAD_JOLT")[0])
                .angle(Gem.Axes.X, state -> (float) state.bus("RELAOD_TILT")[0]);
        root.child(obj.part("Gun"), BODY);

        Gem.Part<WeaponPose> cylinder = root.joint()
                .angle(Gem.Axes.X, state -> (float) state.bus("RELOAD_CYLINDER")[0]);
        cylinder.child(obj.part("Pivot"), BODY);
        Gem.Part<WeaponPose> drum = cylinder.joint()
                .translate(0, 1.75F, 0)
                .angle(Gem.Axes.X, state -> (float) -state.bus("DRUM")[2] * 60F)
                .translate(0, -1.75F, 0);
        drum.child(obj.part("Cylinder"), BODY);
        Gem.Part<WeaponPose> bullets = drum.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("RELOAD_BULLETS")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RELOAD_BULLETS")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RELOAD_BULLETS")[2]);
        bullets.child(obj.part("Bullets"), BODY)
                .visibleIf(state -> state.bus("RELOAD_BULLETS_CON")[0] != 1);
        bullets.child(obj.part("Casings"), BODY);

        root.child(obj.part("Hammer"), BODY)
                .translate(4F, 1.25F, 0)
                .angle(Gem.Axes.Z, state -> -30F + 30F * (float) state.bus("HAMMER")[2])
                .translate(-4F, -1.25F, 0);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(0.75F, 0.75F, 0.75F);
        pose.translate(0F, 1F, 3F);
    }
}
