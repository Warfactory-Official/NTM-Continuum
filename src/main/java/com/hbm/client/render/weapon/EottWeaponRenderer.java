package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** Two-rail GEM port of ItemRenderEOTT; both Aberrator halves remain independently animated. */
public final class EottWeaponRenderer extends WeaponRenderer {
    private static final String[] PARTS = {"Gun", "Sight", "Magazine", "Bullet", "Slide", "Hammer"};
    private static final Surface BODY = SpecialWeaponRenderers.texture("eott");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("aberrator");
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
        pose.translate(0, 0, 1);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Gem.Part<WeaponPose> ordinary = declaration.joint()
                .visibleIf(state -> !state.animated && !doubleStaticContext(state) && !worldStaticContext(state));
        staticBody(ordinary, obj);
        // The inventory renderer made two different copies after setupInv; the item base supplies setupInv.
        Gem.Part<WeaponPose> guiFirst = declaration.joint()
                .visibleIf(state -> !state.animated && state.context == net.minecraft.world.item.ItemDisplayContext.GUI)
                .translate(0, 1, 0).rotate(Gem.Axes.Z, 225F).rotate(Gem.Axes.Y, 90F)
                .rotate(Gem.Axes.X, 25F).rotate(Gem.Axes.Y, 45F).translate(-1, 0, 0);
        staticBody(guiFirst, obj);
        Gem.Part<WeaponPose> guiSecond = declaration.joint()
                .visibleIf(state -> !state.animated && state.context == net.minecraft.world.item.ItemDisplayContext.GUI)
                .translate(0, 1, 5).rotate(Gem.Axes.Z, 225F).rotate(Gem.Axes.Y, -90F)
                .rotate(Gem.Axes.X, -90F).rotate(Gem.Axes.X, 25F).rotate(Gem.Axes.Y, -45F)
                .translate(1, 0, 0);
        staticBody(guiSecond, obj);
        Gem.Part<WeaponPose> worldFirst = declaration.joint()
                .visibleIf(state -> !state.animated && worldStaticContext(state)).translate(-1, 1, 0);
        staticBody(worldFirst, obj);
        Gem.Part<WeaponPose> worldSecond = declaration.joint()
                .visibleIf(state -> !state.animated && worldStaticContext(state)).translate(1, 1, 0);
        staticBody(worldSecond, obj);
        for (int i = 0; i < 2; i++) rail(declaration, obj, i, i == 0 ? -1 : 1);
    }

    private static boolean doubleStaticContext(WeaponPose state) {
        return state.context == net.minecraft.world.item.ItemDisplayContext.GUI;
    }

    private static boolean worldStaticContext(WeaponPose state) {
        return state.context == net.minecraft.world.item.ItemDisplayContext.GROUND
                || state.context == net.minecraft.world.item.ItemDisplayContext.FIXED;
    }

    private static void staticBody(Gem.Part<WeaponPose> root, WavefrontObj obj) {
        for (String part : new String[] {"Gun", "Hammer", "Magazine", "Slide", "Sight"}) {
            root.child(obj.part(part), BODY);
        }
    }

    private void rail(Gem.Model<WeaponPose> declaration, WavefrontObj obj, int rail, int side) {
        float offset = 0.8F;
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .slide(Gem.Axes.X, state -> aimLerp(-offset * side, 0))
                .slide(Gem.Axes.Y, state -> aimLerp(-1.25F * offset, -5.25F / 8F))
                .slide(Gem.Axes.Z, state -> aimLerp(1.25F * offset, 0.125F))
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
        root.child(obj.part("Gun"), BODY);
        root.child(obj.part("Sight"), BODY)
                .spin(Gem.Axes.X, 0, 2.4375F, -1.9375F,
                        state -> (float) state.bus("SIGHT", rail)[0]);
        Gem.Part<WeaponPose> magazine = root.joint()
                .slide(Gem.Axes.X, state -> side * (float) state.bus("MAG", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG", rail)[2])
                .translate(0, 1, 0)
                .angle(Gem.Axes.Z, state -> side * (float) state.bus("MAGROLL", rail)[2])
                .translate(0, -1, 0);
        magazine.child(obj.part("Magazine"), BODY);
        magazine.child(obj.part("Bullet"), BODY)
                .slide(Gem.Axes.X, state -> (float) state.bus("BULLET", rail)[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("BULLET", rail)[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("BULLET", rail)[2]);
        root.child(obj.part("Slide"), BODY)
                .slide(Gem.Axes.Z, state -> (float) state.bus("SLIDE", rail)[2]);
        root.child(obj.part("Hammer"), BODY)
                .translate(0, 1.25F, -3.625F)
                .angle(Gem.Axes.X, state -> -45F + (float) state.bus("HAMMER", rail)[0])
                .translate(0, -1.25F, 3.625F);
    }
}
