package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderAberrator's animated part tree. */
public class AberratorWeaponRenderer extends WeaponRenderer {
    protected static final String[] PARTS = {"Gun", "Sight", "Magazine", "Bullet", "Slide", "Hammer"};
    protected final Surface body;
    protected WavefrontObj model;

    public AberratorWeaponRenderer() { body = SpecialWeaponRenderers.texture("aberrator"); }
    protected WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("aberrator");
        return model;
    }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }
    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 1);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1F * offset, -1.25F * offset, 1.25F * offset,
                0, -5.25 / 8D, 0.125);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        for (String part : new String[] {"Gun", "Hammer", "Magazine", "Slide", "Sight"}) {
            declaration.part(obj.part(part), body).visibleIf(state -> !state.animated);
        }
        animated(declaration, obj, body, 0, 1);
    }

    protected void animated(Gem.Model<WeaponPose> declaration, WavefrontObj obj, Surface skin,
            int rail, int side) {
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(state -> state.animated)
                .scale(0.25F)
                .slide(Gem.Axes.Y, state -> (float) state.bus("RISE", rail)[1])
                .translate(0, 1, -2.25F)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP", rail)[0])
                .translate(0, -1, 2.25F)
                .translate(0, -1, -4)
                .angle(Gem.Axes.X, state -> (float) state.bus("RECOIL", rail)[0])
                .translate(0, 1, 4)
                .translate(0, 1, 0)
                .angle(Gem.Axes.Z, state -> (float) state.bus("ROLL", rail)[2] * side)
                .translate(0, -1, 0);
        root.child(obj.part("Gun"), skin);
        root.child(obj.part("Sight"), skin)
                .spin(Gem.Axes.X, 0, 2.4375F, -1.9375F,
                        state -> (float) state.bus("SIGHT", rail)[0]);
        Gem.Part<WeaponPose> magazine = root.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("MAG", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG", rail)[2])
                .translate(0, 1, 0)
                .angle(Gem.Axes.Z, state -> (float) state.bus("MAGROLL", rail)[2] * side)
                .translate(0, -1, 0);
        magazine.child(obj.part("Magazine"), skin);
        magazine.child(obj.part("Bullet"), skin)
                .slide(Gem.Axes.X, state -> (float) state.bus("BULLET", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("BULLET", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("BULLET", rail)[2]);
        root.child(obj.part("Slide"), skin)
                .slide(Gem.Axes.Z, state -> (float) state.bus("SLIDE", rail)[2]);
        root.child(obj.part("Hammer"), skin)
                .translate(0, 1.25F, -3.625F)
                .angle(Gem.Axes.X, state -> -45F + (float) state.bus("HAMMER", rail)[0])
                .translate(0, -1.25F, 3.625F);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(2.5F, 2.5F, 2.5F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.5F, -1F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.translate(0F, -1F, 4F);
        pose.scale(1.5F, 1.5F, 1.5F);
    }
}
