package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Exact Pepperbox part tree and first-person transforms. */
public final class PepperboxWeaponRenderer extends WeaponRenderer {

    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(Ids.of("hbm", "models/weapons/pepperbox.obj"));
        return model;
    }

    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }

    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1.5);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.25F * offset, -0.75F * offset, offset,
                0, -2.5 / 8D, 0.5);
    }

    /**
     * The speedloader needs no held-only test: it shows while the {@code LOADER} bus has moved it, and
     * every bus rests at zero for a draw that is not the held one.
     */
    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Surface body = Surface.cutout("hbm:textures/models/weapons/pepperbox.png");

        Gem.Part<WeaponPose> gun = declaration.joint()
                .stretch(state -> state.animated ? 0.25F : 1F)
                .slide(Gem.Axes.X, state -> (float) state.bus("TRANSLATE")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("TRANSLATE")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("TRANSLATE")[2])
                .spin(Gem.Axes.X, 0F, 0F, -5F, state -> (float) -state.bus("RECOIL")[0]);

        Gem.Part<WeaponPose> loader = gun.joint()
                .visibleIf(PepperboxWeaponRenderer::loading)
                .slide(Gem.Axes.X, state -> (float) state.bus("LOADER")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("LOADER")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("LOADER")[2]);
        loader.child(obj.part("Speedloader"), body);
        loader.child(obj.part("Shot"), body).visibleIf(state -> state.bus("SHOT")[0] != 0);

        gun.child(obj.part("Grip"), body);
        gun.child(obj.part("Cylinder"), body)
                .angle(Gem.Axes.Z, state -> (float) state.bus("ROTATE")[0]);
        gun.child(obj.part("Hammer"), body)
                .spin(Gem.Axes.X, 0F, 0.375F, -1.875F, state -> (float) state.bus("HAMMER")[0]);
        gun.child(obj.part("Trigger"), body)
                .slide(Gem.Axes.Z, state -> (float) (-state.bus("TRIGGER")[0] * 0.5));
    }

    private static boolean loading(WeaponPose state) {
        double[] loader = state.bus("LOADER");
        return loader[0] != 0 || loader[1] != 0 || loader[2] != 0;
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.5F, 1.5F, 1.5F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0.5F, 0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.translate(0F, 1F, 3F);
    }
}
