package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of original-hbm ItemRenderFlaregun. */
public final class FlaregunWeaponRenderer extends WeaponRenderer {
    private static final net.minecraft.resources.Identifier MODEL_ID = Ids.of("hbm", "models/weapons/flaregun.obj");
    private static final Surface BODY = Surface.cutout("hbm:textures/models/weapons/flaregun.png");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(MODEL_ID);
        return model;
    }

    @Override public org.joml.Vector3fc[] extents() {
        float[] min = model().boundsMin(), max = model().boundsMax();
        org.joml.Vector3fc[] out = new org.joml.Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++)
            out[i++] = new org.joml.Vector3f(x == 0 ? min[0] : max[0], y == 0 ? min[1] : max[1], z == 0 ? min[2] : max[2]);
        return out;
    }

    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float o = 0.8F;
        standardAimingTransform(stack, pose, -1.25F * o, -1.5F * o, 2F * o, 0, -5.5 / 8D, 0.5);
    }

    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> held = declaration.joint().visibleIf(s -> s.animated)
                .stretch(s -> 0.125F)
                .slide(Gem.Axes.X, s -> (float) s.bus("RECOIL")[0])
                .slide(Gem.Axes.Y, s -> (float) s.bus("RECOIL")[1])
                .slide(Gem.Axes.Z, s -> (float) s.bus("RECOIL")[2])
                .angle(Gem.Axes.X, s -> (float) (s.bus("RECOIL")[2] * 10))
                .angle(Gem.Axes.X, s -> (float) s.bus("FLIP")[0]);
        held.child(obj.part("Gun"), BODY);
        held.child(obj.part("Hammer"), BODY)
                .spin(Gem.Axes.X, 0F, 1.8125F, -4F, s -> (float) (s.bus("HAMMER")[0] - 15));
        Gem.Part<WeaponPose> barrel = held.joint()
                .spin(Gem.Axes.X, 0F, 2.156F, 1.78F, s -> (float) s.bus("OPEN")[0]);
        barrel.child(obj.part("Barrel"), BODY);
        barrel.child(obj.part("Flare"), BODY)
                .slide(Gem.Axes.X, s -> (float) s.bus("SHELL")[0])
                .slide(Gem.Axes.Y, s -> (float) s.bus("SHELL")[1])
                .slide(Gem.Axes.Z, s -> (float) s.bus("SHELL")[2]);
        declaration.part(obj.parts("Gun", "Hammer", "Barrel", "Flare"), BODY)
                .visibleIf(s -> !s.animated);
    }
}
