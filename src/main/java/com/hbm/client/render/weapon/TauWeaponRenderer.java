package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderTau. */
public final class TauWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.noCullTexture("tau");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("tau");
        return model;
    }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.75F * offset, -1.75F * offset, 3.5F * offset,
                -1.75F * offset, -1.75F * offset, 3.5F * offset);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.all(), BODY).visibleIf(state -> !state.animated);
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.75F)
                .translate(0, -1, -4)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, 1, 4)
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2])
                .translate(0, 0, -2)
                .angle(Gem.Axes.X, state -> (float) state.bus("RECOIL")[2] * 5F)
                .translate(0, 0, 2);
        root.child(obj.part("Body"), BODY);
        root.child(obj.part("Rotor"), BODY)
                .spin(Gem.Axes.Z, 0F, -0.25F, 0F, state -> (float) state.bus("ROTATE")[2]);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(2F, 2F, 2F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.25F, 0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(2.5F, 2.5F, 2.5F);
        pose.translate(0F, 1F, 2F);
    }
}
