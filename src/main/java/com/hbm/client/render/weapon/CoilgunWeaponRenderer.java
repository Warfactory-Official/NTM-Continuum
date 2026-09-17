package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderCoilgun. */
public final class CoilgunWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("coilgun");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("coilgun");
        return model;
    }
    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.25F * offset, -1.5F * offset, 2.5F * offset,
                0, -7.5 / 8D, 1);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.all(), BODY).visibleIf(state -> !state.animated);
        declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.75F)
                .rotate(Gem.Axes.Y, -90F)
                .translate(-1.5F, 0, 0)
                .slide(Gem.Axes.X, state -> (float) (-state.bus("RECOIL")[0] * 0.5))
                .angle(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[0] * 45F)
                .translate(1.5F, 0, 0)
                .translate(-2.5F, 0, 0)
                .angle(Gem.Axes.Z, state -> (float) state.bus("RELOAD")[0] * -45F)
                .translate(2.5F, 0, 0)
                .child(obj.all(), BODY);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(4F, 4F, 4F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.25F, -0.25F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(3F, 3F, 3F);
        pose.translate(0F, 0.25F, 1.25F);
    }
}
