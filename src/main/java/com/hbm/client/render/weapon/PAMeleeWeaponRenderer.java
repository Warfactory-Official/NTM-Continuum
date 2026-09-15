package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** GEM port of the armor-provided NCR/RPA melee arm renderer. */
public final class PAMeleeWeaponRenderer extends WeaponRenderer {
    private static final Surface NCR_SKIN = Surface.cutout("hbm:textures/armor/ncrpa_arm.png");
    private static final Surface RPA_SKIN = Surface.cutout("hbm:textures/armor/rpa_arm.png");
    private WavefrontObj ncr;
    private WavefrontObj rpa;

    private WavefrontObj ncr() {
        if (ncr == null) ncr = WavefrontObj.load(Ids.of("hbm", "models/armor/ncrpa.obj"));
        return ncr;
    }
    private WavefrontObj rpa() {
        if (rpa == null) rpa = WavefrontObj.load(Ids.of("hbm", "models/armor/remnant.obj"));
        return rpa;
    }
    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(ncr()); }
    @Override protected float getSwayMagnitude(ItemStack stack) { return 2F; }
    @Override protected float getSwayPeriod(ItemStack stack) { return 0.5F; }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, -1.5F, 0.5F);
        pose.scale(0.125F, 0.125F, 0.125F);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj ncrModel = ncr();
        WavefrontObj rpaModel = rpa();
        declaration.part(ncrModel.parts("LeftArm", "RightArm"), NCR_SKIN)
                .visibleIf(state -> !state.animated
                        && state.context != net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                        && state.context != net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        firstPerson(declaration, ncrModel, NCR_SKIN, false);
        firstPerson(declaration, rpaModel, RPA_SKIN, true);
    }

    private void firstPerson(Gem.Model<WeaponPose> declaration, WavefrontObj obj, Surface skin, boolean remnant) {
        Gem.Part<WeaponPose> root = declaration.joint()
                .visibleIf(state -> state.animated && isRemnant() == remnant);
        if (remnant) rpaArms(root, obj, skin);
        else ncrArms(root, obj, skin);
    }

    private void ncrArms(Gem.Part<WeaponPose> root, WavefrontObj obj, Surface skin) {
        Gem.Part<WeaponPose> left = root.joint()
                .slide(Gem.Axes.X, state -> -14F * bus(state, "SWINGLEFT") - 4F * bus(state, "SWEEPTURN"))
                .slide(Gem.Axes.Y, state -> 6F * bus(state, "SWEEPCUT"))
                .slide(Gem.Axes.Z, state -> 2F * bus(state, "SWINGLEFT") + 8F * bus(state, "SWEEPCUT"))
                .angle(Gem.Axes.X, state -> 60F - 60F * bus(state, "EQUIP")
                        + 40F * bus(state, "SWINGRIGHT") - 60F * bus(state, "SWEEPCUT"))
                .translate(3, 0, 0)
                .translate(6, 8, 0)
                .angle(Gem.Axes.Z, state -> 90F * bus(state, "SWINGLEFT"))
                .angle(Gem.Axes.Y, state -> 60F + 30F * bus(state, "SWINGLEFT")
                        - 90F * bus(state, "SWEEPTURN"))
                .translate(-6, -8, 0);
        left.child(obj.part("LeftArm"), skin);

        Gem.Part<WeaponPose> right = root.joint()
                .slide(Gem.Axes.X, state -> 14F * bus(state, "SWINGRIGHT") + 4F * bus(state, "SWEEPTURN"))
                .slide(Gem.Axes.Y, state -> 6F * bus(state, "SWEEPCUT"))
                .slide(Gem.Axes.Z, state -> 2F * bus(state, "SWINGRIGHT") + 8F * bus(state, "SWEEPCUT"))
                .angle(Gem.Axes.X, state -> 60F - 60F * bus(state, "EQUIP")
                        + 40F * bus(state, "SWINGLEFT") - 60F * bus(state, "SWEEPCUT"))
                .translate(-3, 0, 0)
                .translate(-6, 8, 0)
                .angle(Gem.Axes.Z, state -> -90F * bus(state, "SWINGRIGHT"))
                .angle(Gem.Axes.Y, state -> -60F - 30F * bus(state, "SWINGRIGHT")
                        + 90F * bus(state, "SWEEPTURN"))
                .translate(6, -8, 0);
        right.child(obj.part("RightArm"), skin);
    }

    private void rpaArms(Gem.Part<WeaponPose> root, WavefrontObj obj, Surface skin) {
        Gem.Part<WeaponPose> left = root.joint()
                .slide(Gem.Axes.X, state -> -12F * bus(state, "SWINGLEFT") + 2F * bus(state, "SLAPTURN")
                        - 5F * bus(state, "SLAP"))
                .slide(Gem.Axes.Y, state -> 6F * bus(state, "SLAP"))
                .slide(Gem.Axes.Z, state -> 5F * bus(state, "SWINGLEFT") + 8F * bus(state, "SLAP"))
                .angle(Gem.Axes.X, state -> 60F - 60F * bus(state, "EQUIP")
                        - 20F * bus(state, "SWINGRIGHT"))
                .translate(3, 0, 0)
                .translate(6, 8, 0)
                .angle(Gem.Axes.Z, state -> 60F * bus(state, "SWINGLEFT") + 45F * bus(state, "SLAP"))
                .angle(Gem.Axes.Y, state -> 60F + 15F * bus(state, "SWINGLEFT")
                        + 45F * bus(state, "SLAPTURN"))
                .translate(-6, -8, 0);
        left.child(obj.part("LeftArm"), skin);

        Gem.Part<WeaponPose> right = root.joint()
                .slide(Gem.Axes.X, state -> 12F * bus(state, "SWINGRIGHT") - 2F * bus(state, "SLAPTURN")
                        + 5F * bus(state, "SLAP"))
                .slide(Gem.Axes.Y, state -> 6F * bus(state, "SLAP"))
                .slide(Gem.Axes.Z, state -> 5F * bus(state, "SWINGRIGHT") + 8F * bus(state, "SLAP"))
                .angle(Gem.Axes.X, state -> 60F - 60F * bus(state, "EQUIP")
                        - 20F * bus(state, "SWINGLEFT"))
                .translate(-3, 0, 0)
                .translate(-6, 8, 0)
                .angle(Gem.Axes.Z, state -> -60F * bus(state, "SWINGRIGHT") - 45F * bus(state, "SLAP"))
                .angle(Gem.Axes.Y, state -> -60F - 15F * bus(state, "SWINGRIGHT")
                        - 45F * bus(state, "SLAPTURN"))
                .translate(6, -8, 0);
        right.child(obj.part("RightArm"), skin);
    }

    private static float bus(WeaponPose state, String name) { return (float) state.bus(name)[0]; }

    private static boolean isRemnant() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        ItemStack chest = minecraft.player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty()) return false;
        return BuiltInRegistries.ITEM.getKey(chest.getItem()).getPath().contains("rpa")
                && !BuiltInRegistries.ITEM.getKey(chest.getItem()).getPath().contains("ncr");
    }
}
