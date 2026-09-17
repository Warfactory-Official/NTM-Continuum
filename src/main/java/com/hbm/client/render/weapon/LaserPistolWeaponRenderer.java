package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderLaserPistol; variants only change the weapon skin and attachments. */
public final class LaserPistolWeaponRenderer extends WeaponRenderer {
    private final Surface body;
    private WavefrontObj model;

    public LaserPistolWeaponRenderer(String texture) { body = SpecialWeaponRenderers.texture(texture); }
    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("laser_pistol");
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
        standardAimingTransform(stack, pose, -1.75F * offset, -2F * offset, 2.75F * offset,
                0, -10 / 8D, 1.25);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Gun"), body).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Latch"), body).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Capacitors"), body)
                .visibleIf(state -> !state.animated && SpecialWeaponRenderers.hasPath(state, "gun_laser_pistol_pew_pew"));
        declaration.part(obj.part("Tape"), body)
                .visibleIf(state -> !state.animated && SpecialWeaponRenderers.hasPath(state, "gun_laser_pistol_pew_pew"));

        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.375F)
                .translate(0, -1, -6)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, 1, 6)
                .translate(0, 2, -2)
                .angle(Gem.Axes.X, state -> (float) state.bus("LIFT")[0])
                .translate(0, -2, 2)
                .translate(0, -1, -1)
                .angle(Gem.Axes.X, state -> (float) state.bus("SWIRL")[0])
                .translate(0, 1, 1)
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2])
                .slide(Gem.Axes.X, state -> (float) state.bus("JOLT")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("JOLT")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("JOLT")[2]);
        root.child(obj.part("Gun"), body);
        root.child(obj.part("Capacitors"), body)
                .visibleIf(state -> SpecialWeaponRenderers.hasPath(state, "gun_laser_pistol_pew_pew"));
        root.child(obj.part("Tape"), body)
                .visibleIf(state -> SpecialWeaponRenderers.hasPath(state, "gun_laser_pistol_pew_pew"));
        Gem.Part<WeaponPose> latch = root.joint()
                .translate(1.125F, 0, -1.9125F)
                .angle(Gem.Axes.Y, state -> (float) state.bus("LATCH")[1])
                .translate(-1.125F, 0, 1.9125F);
        latch.child(obj.part("Latch"), body);
        latch.child(obj.part("Battery"), body)
                .slide(Gem.Axes.X, state -> (float) state.bus("BATTERY")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("BATTERY")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("BATTERY")[2]);
    }
}
