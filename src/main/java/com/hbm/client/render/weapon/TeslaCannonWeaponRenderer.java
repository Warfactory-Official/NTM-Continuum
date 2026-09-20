package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderTeslaCannon. */
public final class TeslaCannonWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("tesla_cannon");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("tesla_cannon");
        return model;
    }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.75F * offset, -0.5F * offset, 1.75F * offset,
                -1.3125F * offset, 0F, -0.5F * offset);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Gun"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Extension"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Cog"), BODY).visibleIf(state -> !state.animated);
        staticCapacitors(declaration, obj);

        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.75F)
                .translate(0, -2, -2)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, 2, 2)
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2])
                .angle(Gem.Axes.X, state -> (float) state.bus("RECOIL")[2] * 2F);
        root.child(obj.part("Gun"), BODY);
        root.child(obj.part("Extension"), BODY);
        Gem.Part<WeaponPose> moving = root.joint()
                .spin(Gem.Axes.Z, 0F, -1.625F, 0F, state -> (float) state.bus("CYCLE")[2]);
        moving.child(obj.part("Cog"), BODY);
        Gem.Part<WeaponPose> caps = moving.joint();
        for (int i = 0; i < 8; i++) {
            final int capacitor = i;
            caps.child(obj.part("Capacitor"), BODY)
                    .visibleIf(state -> capacitor < Math.min(8, Math.max((int) state.bus("COUNT")[0],
                            SpecialWeaponRenderers.amount(state))));
            if (i < 4) {
                caps = caps.joint().translate(0, -1.625F, 0)
                        .rotate(Gem.Axes.Z, -22.5F)
                        .translate(0, 1.625F, 0);
            } else if (i == 4) {
                caps = caps.joint()
                        .angle(Gem.Axes.Z, state -> (float) -state.bus("CYCLE")[2])
                        .slide(Gem.Axes.X, state -> (float) (-state.bus("CYCLE")[2] * 0.5 / 22.5));
            } else {
                caps = caps.joint().translate(0.5F, 0, 0);
            }
        }
    }

    private void staticCapacitors(Gem.Model<WeaponPose> declaration, WavefrontObj obj) {
        Gem.Part<WeaponPose> caps = declaration.joint().visibleIf(state -> !state.animated)
                .translate(0, 0, 0);
        for (int i = 0; i < 10; i++) {
            caps.child(obj.part("Capacitor"), BODY);
            if (i < 4) {
                caps = caps.joint().translate(0, -1.625F, 0)
                        .rotate(Gem.Axes.Z, -22.5F)
                        .translate(0, 1.625F, 0);
            } else {
                caps = caps.joint().translate(0.5F, 0, 0);
            }
        }
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, 0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(2.75F, 2.75F, 2.75F);
        pose.translate(0F, 1.5F, 1F);
    }
}
