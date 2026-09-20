package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderDrill. */
public final class DrillWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("drill");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("drill");
        return model;
    }
    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 0F : -0.5F;
    }
    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.25F * offset, -1.75F * offset, 1.75F * offset,
                -1F * offset, -1.75F * offset, 1.25F * offset);
    }
    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.all(), BODY).visibleIf(state -> !state.animated);
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.375F)
                .angle(Gem.Axes.Y, state -> 15F * (1F - (float) state.bus("DEPLOY")[0] * 0.5F))
                .angle(Gem.Axes.X, state -> -10F * (1F - (float) state.bus("DEPLOY")[0] * 0.5F))
                .translate(0, 2, -6)
                .angle(Gem.Axes.Y, state -> (float) state.bus("EQUIP")[0] * -45F)
                .angle(Gem.Axes.X, state -> (float) state.bus("EQUIP")[0] * -20F)
                .translate(0, -2, 6)
                .angle(Gem.Axes.X, state -> (float) state.bus("LIFT")[0])
                .slide(Gem.Axes.Z, state -> (float) state.bus("DEPLOY")[0]);
        root.child(obj.part("Base"), BODY);
        root.child(obj.part("Gauge"), BODY)
                .translate(1, 2.0625F, -1.75F)
                .rotate(Gem.Axes.X, 45F)
                .angle(Gem.Axes.Z, state -> -135F + 270F * SpecialWeaponRenderers.amount(state)
                        / Math.max(1F, state.stack.getItem() instanceof ItemGunBaseNT gun
                                ? gun.getPrimaryMagazine(state.stack, 0).getCapacity(state.stack) : 1F))
                .rotate(Gem.Axes.X, -45F)
                .translate(-1, -2.0625F, 1.75F);
        piston(root, obj, "Piston1", 0);
        piston(root, obj, "Piston2", 2F * (float) Math.PI / 3F);
        piston(root, obj, "Piston3", 4F * (float) Math.PI / 3F);
        root.child(obj.part("DrillBack"), BODY)
                .angle(Gem.Axes.Z, state -> (float) -state.bus("SPIN")[0]);
        root.child(obj.part("DrillFront"), BODY)
                .angle(Gem.Axes.Z, state -> (float) state.bus("SPIN")[0]);
    }

    private void piston(Gem.Part<WeaponPose> root, WavefrontObj obj, String name, float phase) {
        root.child(obj.part(name), BODY)
                .slide(Gem.Axes.Y, state -> (float) Math.sin((state.bus("SPIN")[0] * 5F)
                        * Math.PI / 180F + phase) * 0.125F - 0.125F);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-0.5F, 0F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(2.25F, 2.25F, 2.25F);
        pose.translate(1F, -2F, 6F);
    }
}
