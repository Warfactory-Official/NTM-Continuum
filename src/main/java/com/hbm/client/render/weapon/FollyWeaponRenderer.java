package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderFolly's cannon hierarchy. */
public final class FollyWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("moonlight");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("folly");
        return model;
    }
    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2F : 2.5F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        float aim = 0.75F;
        standardAimingTransform(stack, pose, -2.5F * offset, -1.5F * offset, 2.75F * offset,
                -2 * aim, -1 * aim, 2.25F * offset);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.all(), BODY).visibleIf(state -> !state.animated);
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.75F)
                .translate(0, 1, -4)
                .angle(Gem.Axes.X, state -> (float) -state.bus("EQUIP")[0])
                .translate(0, -1, 4)
                .translate(0, -2, -2)
                .angle(Gem.Axes.X, state -> (float) state.bus("LOAD")[0])
                .translate(0, 2, 2);
        root.child(obj.part("Cannon"), BODY);
        root.child(obj.part("Barrel"), BODY)
                .slide(Gem.Axes.X, state -> (float) state.bus("RECOIL")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("RECOIL")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2]);
        root.child(obj.part("Shell"), BODY)
                .slide(Gem.Axes.X, state -> (float) state.bus("SHELL")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("SHELL")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("SHELL")[2]);
        Gem.Part<WeaponPose> breech = root.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("BREECH")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("BREECH")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("BREECH")[2]);
        breech.child(obj.part("Breech"), BODY);
        breech.child(obj.part("Cog"), BODY)
                .translate(0, 1, 0)
                .angle(Gem.Axes.Z, state -> (float) state.bus("SCREW")[2])
                .translate(0, -1, 0);
    }
}
