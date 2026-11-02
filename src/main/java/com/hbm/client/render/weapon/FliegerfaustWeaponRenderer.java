package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Gem port of 1.7 ItemRenderFliegerfaust. The drum shows every rocket except while UNLOADING runs. */
public final class FliegerfaustWeaponRenderer extends WeaponRenderer {
    private static final Identifier MODEL_ID = Ids.of("hbm", "models/weapons/fliegerfaust.obj");
    private static final Surface BODY = Surface.cutout("hbm:textures/models/weapons/fliegerfaust.png");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = WavefrontObj.load(MODEL_ID);
        return model;
    }

    @Override protected float getTurnMagnitude(ItemStack s) { return ItemGunBaseNT.getIsAiming(s) ? 2.5F : -.25F; }

    @Override public float viewFov(ItemStack s, float f, float p) { return aimingFov(s, f, p, .33F); }

    @Override
    public void setupFirstPerson(ItemStack s, PoseStack p) {
        p.translate(0, 0, .875);
        float o = .8F;
        standardAimingTransform(s, p, -2.75F * o, -2F * o, -1.5F * o, -.9375, -7.75 / 8D, -2.5);
    }

    @Override
    protected void setup(Gem.Model<WeaponPose> d) {
        WavefrontObj o = model();
        Gem.Part<WeaponPose> g = d.joint().visibleIf(s -> s.animated).stretch(s -> 1.25F)
                .angle(Gem.Axes.Z, s -> aimLerp(0, -45))
                .spin(Gem.Axes.X, 0, 0, 1, s -> (float) s.bus("EQUIP")[0])
                .spin(Gem.Axes.X, 0, -6, -1, s -> (float) s.bus("RELOAD")[0])
                .slide(Gem.Axes.Z, s -> (float) s.bus("RECOIL")[2]);
        g.child(o.part("Launcher"), BODY);
        Gem.Part<WeaponPose> drum = g.joint().translate(0, 0, 2)
                .slide(Gem.Axes.X, s -> (float) s.bus("DRUM")[0])
                .slide(Gem.Axes.Y, s -> (float) s.bus("DRUM")[1])
                .slide(Gem.Axes.Z, s -> (float) s.bus("DRUM")[2])
                .spin(Gem.Axes.X, 0, 0, -8.5F, s -> (float) s.bus("DRUMROT")[0]);
        drum.child(o.part("Drum"), BODY);
        for (int i = 1; i <= 9; i++) {
            int rocket = i;
            drum.child(o.part("Rocket" + i), BODY).visibleIf(s -> rocket <= rockets(s));
        }
        d.part(o.part("Launcher"), BODY).visibleIf(s -> !s.animated);
        d.joint().visibleIf(s -> !s.animated).translate(0, 0, 2).child(o.part("Drum"), BODY);
    }

    private static int rockets(WeaponPose s) {
        return s.bus("UNLOADING")[0] > 0 ? SpecialWeaponRenderers.amount(s) : 9;
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.375F, 1.375F, 1.375F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(0F, 0.5F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(2F, 2F, 2F);
        pose.translate(0F, 1F, -2F);
    }
}
