package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.impl.ItemGunNI4NI;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of original-hbm's ItemRenderNI4NI model hierarchy and dynamic material state. */
public final class Ni4niWeaponRenderer extends WeaponRenderer {
    private static final Surface NORMAL = SpecialWeaponRenderers.texture("n_i_4_n_i");
    private static final Surface GREYSCALE = SpecialWeaponRenderers.texture("n_i_4_n_i_greyscale");
    // Original GL disables texturing for the coin meshes.  The Core material equivalent is the
    // existing opaque white surface with the requested vertex colour.
    private static final Surface COIN = Surface.solid("hbm:textures/models/misc/white.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("n_i_4_n_i");
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
        standardAimingTransform(stack, pose, -offset, -offset, offset, 0, -5 / 8D, 0.125);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("FrameDark"), NORMAL)
                .visibleIf(state -> !state.animated).skin(Ni4niWeaponRenderer::body)
                .tint(state -> color(state, 0));
        declaration.part(obj.part("Grip"), NORMAL)
                .visibleIf(state -> !state.animated).skin(Ni4niWeaponRenderer::body)
                .tint(state -> color(state, 2));
        declaration.part(obj.part("FrameLight"), NORMAL)
                .visibleIf(state -> !state.animated).skin(Ni4niWeaponRenderer::body)
                .tint(state -> color(state, 1));
        declaration.part(obj.part("Cylinder"), NORMAL)
                .visibleIf(state -> !state.animated).skin(Ni4niWeaponRenderer::body)
                .tint(state -> color(state, 1));
        declaration.part(obj.part("CylinderHighlights"), NORMAL)
                .visibleIf(state -> !state.animated).skin(Ni4niWeaponRenderer::body)
                .light(state -> FULL_BRIGHT);
        declaration.part(obj.part("Barrel"), NORMAL)
                .visibleIf(state -> !state.animated).skin(Ni4niWeaponRenderer::body)
                .light(state -> FULL_BRIGHT);
        staticCoins(declaration, obj);
        animated(declaration, obj);
    }

    private void animated(Gem.Model<WeaponPose> declaration, WavefrontObj obj) {
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.3125F)
                .translate(0, 0, -2.25F)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0])
                .translate(0, 0, 2.25F)
                .translate(0, -1, -6)
                .angle(Gem.Axes.X, state -> (float) state.bus("RECOIL")[0])
                .translate(0, 1, 6);
        root.child(obj.part("FrameDark"), NORMAL).skin(Ni4niWeaponRenderer::body)
                .tint(state -> color(state, 0));
        root.child(obj.part("Grip"), NORMAL).skin(Ni4niWeaponRenderer::body)
                .tint(state -> color(state, 2));
        root.child(obj.part("FrameLight"), NORMAL).skin(Ni4niWeaponRenderer::body)
                .tint(state -> color(state, 1));
        root.child(obj.part("Cylinder"), NORMAL).skin(Ni4niWeaponRenderer::body)
                .tint(state -> color(state, 1))
                .spin(Gem.Axes.Z, 0, 1.1875F, 0, state -> (float) state.bus("DRUM")[2]);
        root.child(obj.part("CylinderHighlights"), NORMAL).skin(Ni4niWeaponRenderer::body)
                .light(state -> FULL_BRIGHT)
                .spin(Gem.Axes.Z, 0, 1.1875F, 0, state -> (float) state.bus("DRUM")[2]);
        root.child(obj.part("Barrel"), NORMAL).skin(Ni4niWeaponRenderer::body)
                .light(state -> FULL_BRIGHT);
        animatedCoins(root, obj);
    }

    private static void staticCoins(Gem.Model<WeaponPose> declaration, WavefrontObj obj) {
        // original-hbm's renderOther always showed all four coin sockets; only the held render gated them.
        declaration.part(obj.part("Coin1"), COIN).visibleIf(state -> !state.animated)
                .tint(state -> 0xFF00FF00).light(state -> FULL_BRIGHT);
        declaration.part(obj.part("Coin2"), COIN).visibleIf(state -> !state.animated)
                .tint(state -> 0xFF00FF00).light(state -> FULL_BRIGHT);
        declaration.part(obj.part("Coin3"), COIN).visibleIf(state -> !state.animated)
                .tint(state -> 0xFF00FF00).light(state -> FULL_BRIGHT);
        declaration.part(obj.part("Coin4"), COIN).visibleIf(state -> !state.animated)
                .tint(state -> 0xFF00FF00).light(state -> FULL_BRIGHT);
    }

    private static void animatedCoins(Gem.Part<WeaponPose> root, WavefrontObj obj) {
        root.child(obj.part("Coin1"), COIN).visibleIf(state -> coinVisible(state, 1))
                .tint(state -> coinColor(state, 7)).light(state -> FULL_BRIGHT);
        root.child(obj.part("Coin2"), COIN).visibleIf(state -> coinVisible(state, 2))
                .tint(state -> coinColor(state, 6)).light(state -> FULL_BRIGHT);
        root.child(obj.part("Coin3"), COIN).visibleIf(state -> coinVisible(state, 3))
                .tint(state -> coinColor(state, 5)).light(state -> FULL_BRIGHT);
        root.child(obj.part("Coin4"), COIN).visibleIf(state -> coinVisible(state, 4))
                .tint(state -> coinColor(state, 4)).light(state -> FULL_BRIGHT);
    }

    private static Surface body(WeaponPose state) {
        return ItemGunNI4NI.getColors(state.stack) == null ? NORMAL : GREYSCALE;
    }

    private static int color(WeaponPose state, int index) {
        int[] colors = ItemGunNI4NI.getColors(state.stack);
        return 0xFF000000 | (colors == null ? 0xFFFFFF : colors[index]);
    }

    private static boolean coinVisible(WeaponPose state, int coin) {
        return ItemGunNI4NI.getCoinCount(state.stack) > 4 - coin;
    }

    private static int coinColor(WeaponPose state, int readyAt) {
        return ItemGunNI4NI.getCoinCount(state.stack) > readyAt
                ? 0xFFFFFF00 : 0xFF00FF00;
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(2.5F, 2.5F, 2.5F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, 0F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.translate(0F, 0.25F, 3F);
        pose.scale(1.5F, 1.5F, 1.5F);
    }
}
