package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Exact Gem port of 1.7 ItemRenderFlaregun. */
public final class FlaregunWeaponRenderer extends WeaponRenderer {
    private static final Identifier MODEL_ID = Ids.of("hbm", "models/weapons/flaregun.obj");
    private static final Surface BODY = Surface.cutout("hbm:textures/models/weapons/flaregun.png");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(MODEL_ID);
        return model;
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
                .angle(Gem.Axes.X, s -> (float) s.bus("FLIP")[0])
                .spin(Gem.Axes.X, 0F, 0F, -8F, s -> (float) -s.bus("EQUIP")[0]);
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

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1F, 1F, 1F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.5F, 0F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(0.5F, 0.5F, 0.5F);
        pose.translate(0F, 0.25F, 3F);
    }

    @Override
    protected void setupEntity(PoseStack pose, ItemStack stack) {
        super.setupEntity(pose, stack);
        pose.scale(0.5F, 0.5F, 0.5F);
    }
}
