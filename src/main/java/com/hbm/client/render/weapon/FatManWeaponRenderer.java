package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderFatMan. */
public final class FatManWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("fatman");
    private static final Surface MININUKE = SpecialWeaponRenderers.texture("fatman_mininuke");
    private static final Surface BALEFIRE = SpecialWeaponRenderers.texture("fatman_balefire");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("fatman");
        return model;
    }
    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.5F * offset, -1.25F * offset, 0.5F * offset,
                -1F * offset, -1.25F * offset, 0F);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Launcher"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Handle"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Gauge"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Lid"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Piston"), BODY)
                .visibleIf(state -> !state.animated)
                .slide(Gem.Axes.Z, state -> SpecialWeaponRenderers.amount(state) > 0 ? 0F : 3F);
        declaration.part(obj.part("MiniNuke"), MININUKE)
                .visibleIf(state -> !state.animated && SpecialWeaponRenderers.amount(state) > 0)
                .skin(state -> SpecialWeaponRenderers.ammoPath(state).equals("nuke_balefire") ? BALEFIRE : MININUKE);

        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.5F)
                .translate(0, 1, -2)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, -1, 2);
        root.child(obj.part("Launcher"), BODY);
        Gem.Part<WeaponPose> handle = root.joint().slide(Gem.Axes.Z,
                state -> (float) state.bus("HANDLE")[2]);
        handle.child(obj.part("Handle"), BODY);
        handle.child(obj.part("Gauge"), BODY)
                .translate(0.4375F, -0.875F, 0)
                .angle(Gem.Axes.Z, state -> (float) state.bus("GAUGE")[2])
                .translate(-0.4375F, 0.875F, 0);
        root.child(obj.part("Lid"), BODY)
                .spin(Gem.Axes.Z, 0.25F, 0.125F, 0, state -> (float) state.bus("LID")[2]);
        root.child(obj.part("Piston"), BODY)
                .visibleIf(state -> SpecialWeaponRenderers.amount(state) > 0 || state.bus("PISTON")[2] != 0)
                .slide(Gem.Axes.Z, state -> (float) state.bus("PISTON")[2]);
        root.child(obj.part("Piston"), BODY)
                .visibleIf(state -> SpecialWeaponRenderers.amount(state) <= 0 && state.bus("PISTON")[2] == 0)
                .translate(0, 0, 3);
        root.child(obj.part("MiniNuke"), MININUKE)
                .visibleIf(state -> SpecialWeaponRenderers.amount(state) > 0
                        || state.bus("NUKE")[0] != 0 || state.bus("NUKE")[1] != 0 || state.bus("NUKE")[2] != 0)
                .slide(Gem.Axes.X, state -> (float) state.bus("NUKE")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("NUKE")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("NUKE")[2])
                .skin(state -> SpecialWeaponRenderers.ammoPath(state).equals("nuke_balefire") ? BALEFIRE : MININUKE);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.375F, 1.375F, 1.375F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, -0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(2.5F, 2.5F, 2.5F);
        pose.translate(-0.5F, 0.5F, -3F);
    }
}
