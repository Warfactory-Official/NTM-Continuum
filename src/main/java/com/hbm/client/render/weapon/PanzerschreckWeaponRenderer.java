package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderPanzerschreck. */
public final class PanzerschreckWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("panzerschreck");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("panzerschreck");
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
        standardAimingTransform(stack, pose, -2.75F * offset, -2F * offset, 2.5F * offset,
                -0.9375, -9.25 / 8D, 0.25);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Tube"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Shield"), BODY)
                .visibleIf(state -> !state.animated && !SpecialWeaponRenderers.hasUpgrade(state, 204));
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(1.25F)
                .translate(0, -1, -1)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, 1, 1)
                .translate(0, -4, -3)
                .angle(Gem.Axes.X, state -> (float) state.bus("RELOAD")[0])
                .translate(0, 4, 3);
        root.child(obj.part("Tube"), BODY);
        root.child(obj.part("Shield"), BODY)
                .visibleIf(state -> !SpecialWeaponRenderers.hasUpgrade(state, 204));
        root.child(obj.part("Rocket"), BODY)
                .slide(Gem.Axes.X, state -> (float) state.bus("ROCKET")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("ROCKET")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("ROCKET")[2]);
    }
}
