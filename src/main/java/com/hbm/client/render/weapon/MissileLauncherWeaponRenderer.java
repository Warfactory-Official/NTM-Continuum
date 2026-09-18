package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderMissileLauncher. */
public final class MissileLauncherWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("missile_launcher");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("missile_launcher");
        return model;
    }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.5F * offset, -1.25F * offset, 0.5F * offset,
                -1F * offset, -1.25F * offset, 0F);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.parts("Launcher", "Barrel", "Front"), BODY)
                .visibleIf(state -> !state.animated);
        declaration.part(obj.part("Missile"), BODY)
                .visibleIf(state -> !state.animated && SpecialWeaponRenderers.amount(state) > 0);
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.5F)
                .translate(0, -2, -2)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, 2, 2);
        root.child(obj.part("Launcher"), BODY);
        Gem.Part<WeaponPose> front = root.joint()
                .translate(0, 0.25F, 1.6875F)
                .angle(Gem.Axes.X, state -> (float) state.bus("OPEN")[0])
                .translate(0, -0.25F, -1.6875F);
        front.child(obj.part("Front"), BODY);
        front.child(obj.part("Barrel"), BODY)
                .slide(Gem.Axes.Z, state -> (float) state.bus("BARREL")[2]);
        front.child(obj.part("Missile"), BODY)
                .slide(Gem.Axes.X, state -> (float) state.bus("MISSILE")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MISSILE")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MISSILE")[2]);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, -0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(2.5F, 2.5F, 2.5F);
        pose.translate(0F, -0.5F, -2F);
    }
}
