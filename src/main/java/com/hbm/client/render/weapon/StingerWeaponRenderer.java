package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderStinger, including its panzerschreck rocket mesh. */
public final class StingerWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("stinger");
    private static final Surface ROCKET = SpecialWeaponRenderers.texture("panzerschreck");
    private WavefrontObj model;
    private WavefrontObj rocketModel;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("stinger");
        return model;
    }
    private WavefrontObj rocketModel() {
        if (rocketModel == null) rocketModel = SpecialWeaponRenderers.model("panzerschreck");
        return rocketModel;
    }
    @Override public org.joml.Vector3fc[] extents() {
        return SpecialWeaponRenderers.extents(model());
    }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.5F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -3.75F * offset, -9F * offset, -3.5F * offset,
                -2.625F * offset, -6.5, -8.5F);
    }
    @Override protected boolean scopedIn(ItemStack stack) {
        return fullyAimed();
    }

    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.all(), BODY).visibleIf(state -> !state.animated);
        Gem.Part<WeaponPose> root = declaration.joint()
                .visibleIf(state -> state.animated)
                .scale(1.5F)
                .translate(0, -1, -1)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, 1, 1)
                .translate(0, -4, -3)
                .angle(Gem.Axes.X, state -> (float) state.bus("RELOAD")[0])
                .translate(0, 4, 3)
                .rotate(Gem.Axes.Y, 180F);
        root.child(obj.all(), BODY);
        root.child(rocketModel().part("Rocket"), ROCKET)
                .translate(0, 3.5F, -3F)
                .slide(Gem.Axes.X, state -> (float) state.bus("ROCKET")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("ROCKET")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("ROCKET")[2]);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.0625F, 1.0625F, 1.0625F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(225F));
        pose.translate(0.25F, -2.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.translate(0F, -2.5F, -3.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180F));
    }
}
