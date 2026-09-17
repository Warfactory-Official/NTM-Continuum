package com.hbm.client.render.weapon;

import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** The unmodified G3 renderer; optional weapon-mod geometry is intentionally not claimed. */
public final class G3WeaponRenderer extends WeaponRenderer {

    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(Ids.of("hbm", "models/weapons/g3.obj"));
        return model;
    }

    @Override public org.joml.Vector3fc[] extents() {
        WavefrontObj obj = model();
        float[] min = obj.boundsMin(), max = obj.boundsMax();
        org.joml.Vector3fc[] out = new org.joml.Vector3fc[8];
        int i = 0;
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
            out[i++] = new org.joml.Vector3f(x == 0 ? min[0] : max[0], y == 0 ? min[1] : max[1],
                    z == 0 ? min[2] : max[2]);
        }
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
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.25F * offset, -offset, 2.75F * offset,
                0, -3.5625 / 8D, 1.75);
    }

    /**
     * The chambered round and the cocking plug are held-only: they were never in
     * {@code ItemRenderG3.renderOther}'s part list, so an icon does not show them.
     */
    @Override
    protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        Surface body = Surface.cutout("hbm:textures/models/weapons/g3.png");

        Gem.Part<WeaponPose> gun = declaration.joint()
                .stretch(state -> state.animated ? 0.375F : 1F)
                .spin(Gem.Axes.X, 0F, -2F, -6F, state -> (float) state.bus("EQUIP")[0])
                .spin(Gem.Axes.X, 0F, 0F, -4F, state -> (float) state.bus("LIFT")[0])
                .slide(Gem.Axes.Z, state -> (float) state.bus("RECOIL")[2]);

        for (String part : G3StaticLayout.PARTS) {
            if (part.equals("Magazine") || part.equals("Guide_And_Bolt") || part.equals("Handle")) continue;
            gun.child(obj.part(part), body);
        }

        Gem.Part<WeaponPose> magazine = gun.joint()
                .slide(Gem.Axes.X, state -> (float) state.bus("MAG")[0])
                .slide(Gem.Axes.Y, state -> (float) state.bus("MAG")[1])
                .slide(Gem.Axes.Z, state -> (float) state.bus("MAG")[2])
                .translate(0F, -1.75F, -0.5F)
                .angle(Gem.Axes.Z, state -> (float) state.bus("SPEEN")[2])
                .angle(Gem.Axes.Y, state -> (float) state.bus("SPEEN")[1])
                .translate(0F, 1.75F, 0.5F);
        magazine.child(obj.part("Magazine"), body);
        magazine.child(obj.part("Bullet"), body)
                .visibleIf(state -> state.animated && state.bus("BULLET")[0] == 0);

        gun.child(obj.part("Guide_And_Bolt"), body)
                .slide(Gem.Axes.Z, state -> (float) state.bus("BOLT")[2]);

        Gem.Part<WeaponPose> plug = gun.joint()
                .translate(0F, 0.625F, 0F)
                .slide(Gem.Axes.Z, state -> (float) state.bus("PLUG")[2])
                .angle(Gem.Axes.Z, state -> (float) state.bus("HANDLE")[2])
                .translate(0F, -0.625F, 0F);
        plug.child(obj.part("Plug"), body).visibleIf(state -> state.animated);

        plug.joint()
                .translate(0F, 0.625F, 5.25F)
                .rotate(Gem.Axes.Z, 22.5F)
                .angle(Gem.Axes.Y, state -> (float) state.bus("HANDLE")[1])
                .rotate(Gem.Axes.Z, -22.5F)
                .translate(0F, -0.625F, -5.25F)
                .child(obj.part("Handle"), body);

        gun.child(obj.part("Selector"), body)
                .spin(Gem.Axes.X, 0F, -0.875F, -3.5F, state -> state.animated
                        ? -30F * (1 - ItemGunBaseNT.getMode(state.stack, 0))
                        : -30F);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        boolean silencer = hasSilencer(stack);
        if (hasStock(stack)) {
            pose.scale(0.875F, 0.875F, 0.875F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(silencer ? 50F : 45F));
            pose.translate(silencer ? 0.75F : -0.5F, 0.5F, 0F);
        } else {
            pose.scale(1.125F, 1.125F, 1.125F);
            pose.mulPose(Axis.XP.rotationDegrees(25F));
            pose.mulPose(Axis.YP.rotationDegrees(silencer ? 55F : 45F));
            pose.translate(2.5F, 0.5F, 0F);
        }
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1F, 1F, 1F);
        pose.translate(0F, 2F, 4F);
    }

    static boolean hasStock(ItemStack stack) {
        return !XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_NO_STOCK);
    }

    static boolean hasSilencer(ItemStack stack) {
        return SpecialWeaponRenderers.itemPath(stack).equals("gun_g3_zebra")
                || XWeaponModManager.hasUpgrade(stack, 0, XWeaponModManager.ID_SILENCER);
    }
}
