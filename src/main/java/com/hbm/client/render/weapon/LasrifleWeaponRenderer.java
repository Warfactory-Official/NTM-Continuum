package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderLasrifle. Weapon-mod attachments remain data owned by the future mod system. */
public final class LasrifleWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("lasrifle");
    private static final Surface MODS = SpecialWeaponRenderers.texture("lasrifle_mods");
    private WavefrontObj model;
    private WavefrontObj mods;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("lasrifle");
        return model;
    }
    private WavefrontObj mods() {
        if (mods == null) mods = SpecialWeaponRenderers.model("lasrifle_mods");
        return mods;
    }
    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick,
                SpecialWeaponRenderers.hasUpgrade(stack, 218) ? 0.66F : 0.75F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        if (SpecialWeaponRenderers.hasUpgrade(stack, 218)) {
            standardAimingTransform(stack, pose, -1.5F * offset, -1.5F * offset, 2.5F * offset,
                    0, -5.25 / 8D, 1);
        } else {
            standardAimingTransform(stack, pose, -1.5F * offset, -1.5F * offset, 2.5F * offset,
                    0, -7.375 / 8D, 0.75);
        }
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Gun"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Stock"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Scope"), BODY)
                .visibleIf(state -> !state.animated && !SpecialWeaponRenderers.hasUpgrade(state, 218));
        declaration.part(obj.part("Lever"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Battery"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Barrel"), BODY)
                .visibleIf(state -> !state.animated && !SpecialWeaponRenderers.hasUpgrade(state, 216));
        Gem.Part<WeaponPose> staticMods = declaration.joint().visibleIf(state -> !state.animated);
        staticMods.child(mods().part("BarrelShotgun"), MODS)
                .visibleIf(state -> SpecialWeaponRenderers.hasUpgrade(state, 216));
        staticMods.child(mods().part("UnderBarrel"), MODS)
                .visibleIf(state -> SpecialWeaponRenderers.hasUpgrade(state, 217));

        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.3125F)
                .translate(0, -1, -6)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, 1, 6)
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2]);
        root.child(obj.part("Gun"), BODY);
        root.child(obj.part("Stock"), BODY);
        root.child(obj.part("Scope"), BODY)
                .visibleIf(state -> !SpecialWeaponRenderers.hasUpgrade(state, 218));
        root.child(obj.part("Lever"), BODY)
                .spin(Gem.Axes.X, 0, -0.375F, 2.375F, state -> (float) state.bus("LEVER")[0]);
        root.child(obj.part("Battery"), BODY)
                .slide(Gem.Axes.X, state -> (float) state.bus("MAG")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG")[2]);
        root.child(obj.part("Barrel"), BODY)
                .visibleIf(state -> !SpecialWeaponRenderers.hasUpgrade(state, 216));
        root.child(mods().part("BarrelShotgun"), MODS)
                .visibleIf(state -> SpecialWeaponRenderers.hasUpgrade(state, 216));
        root.child(mods().part("UnderBarrel"), MODS)
                .visibleIf(state -> SpecialWeaponRenderers.hasUpgrade(state, 217));
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.03125F, 1.03125F, 1.03125F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0.75F, 0F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.translate(0F, 0F, 4F);
    }
}
