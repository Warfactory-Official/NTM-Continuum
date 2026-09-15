package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderChargeThrower. */
public final class ChargeThrowerWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("charge_thrower");
    private static final Surface MORTAR = SpecialWeaponRenderers.texture("charge_thrower_mortar");
    private static final Surface HOOK = SpecialWeaponRenderers.texture("charge_thrower_hook");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("charge_thrower");
        return model;
    }
    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        if (SpecialWeaponRenderers.hasUpgrade(stack, 202)) {
            standardAimingTransform(stack, pose, -1.5F * offset, -1.25F * offset, 3.5F * offset,
                    -0.15625, -6.5 / 8D, 1.6875);
        } else {
            standardAimingTransform(stack, pose, -1.5F * offset, -1.25F * offset, 3.5F * offset,
                    -1.5F * 0.5F, -1.25F * 0.5F, 3.5F * 0.5F);
        }
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Gun"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Scope"), BODY)
                .visibleIf(state -> !state.animated && SpecialWeaponRenderers.hasUpgrade(state, 202));
        declaration.part(obj.part("Hook"), HOOK).visibleIf(state -> !state.animated && isHook(state));
        declaration.part(obj.part("Mortar"), MORTAR)
                .visibleIf(state -> !state.animated && isMortar(state));
        declaration.part(obj.part("Oomph"), MORTAR)
                .visibleIf(state -> !state.animated && isMortarCharge(state));

        Gem.Part<WeaponPose> root = normalRoot(declaration);
        root.child(obj.part("Gun"), BODY);
        root.child(obj.part("Scope"), BODY)
                .visibleIf(state -> SpecialWeaponRenderers.hasUpgrade(state, 202)
                        && !SpecialWeaponRenderers.fullyAimed(state));
        Gem.Part<WeaponPose> payload = root.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("AMMO")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("AMMO")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("AMMO")[2])
                .angle(Gem.Axes.Z, state -> (float) state.bus("TWIST")[2]);
        payload.child(obj.part("Hook"), HOOK).visibleIf(ChargeThrowerWeaponRenderer::isHook);
        payload.child(obj.part("Mortar"), MORTAR).visibleIf(ChargeThrowerWeaponRenderer::isMortar);
        payload.child(obj.part("Oomph"), MORTAR).visibleIf(ChargeThrowerWeaponRenderer::isMortarCharge);
    }

    private Gem.Part<WeaponPose> normalRoot(Gem.Model<WeaponPose> declaration) {
        return declaration.joint().visibleIf(state -> state.animated)
                .scale(0.5F)
                .stretch(state -> usingScope(state) ? 7F : 1F)
                .slide(Gem.Axes.X, state -> usingScope(state) ? -0.5F : 0F)
                .slide(Gem.Axes.Y, state -> usingScope(state) ? -1.5F : 0F)
                .slide(Gem.Axes.Z, state -> usingScope(state) ? -4F : 0F)
                .translate(0, 0, -7)
                .angle(Gem.Axes.X, state -> (float) -state.bus("EQUIP")[0])
                .translate(0, 0, 7)
                .translate(0, -7, 4)
                .angle(Gem.Axes.X, state -> (float) state.bus("RAISE")[0])
                .translate(0, 7, -4)
                .slide(Gem.Axes.X, state -> (float) state.bus("RECOIL")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RECOIL")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2])
                .translate(0, 0, -2)
                .angle(Gem.Axes.Y, state -> (float) state.bus("TURN")[1])
                .translate(0, 0, 2)
                .translate(0, -1, 0)
                .angle(Gem.Axes.Z, state -> (float) state.bus("ROLL")[2])
                .translate(0, 1, 0);
    }

    private static boolean isHook(WeaponPose state) { return SpecialWeaponRenderers.ammoPath(state).equals("ct_hook"); }
    private static boolean isMortar(WeaponPose state) {
        String ammo = SpecialWeaponRenderers.ammoPath(state);
        return ammo.equals("ct_mortar") || ammo.equals("ct_mortar_charge");
    }
    private static boolean isMortarCharge(WeaponPose state) {
        return SpecialWeaponRenderers.ammoPath(state).equals("ct_mortar_charge");
    }
    private static boolean usingScope(WeaponPose state) {
        return SpecialWeaponRenderers.hasUpgrade(state, 202) && SpecialWeaponRenderers.fullyAimed(state);
    }
}
