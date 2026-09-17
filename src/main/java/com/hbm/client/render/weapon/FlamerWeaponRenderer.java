package com.hbm.client.render.weapon;

import com.mojang.math.Axis;
import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderFlamer; the three skins are separate renderer instances. */
public final class FlamerWeaponRenderer extends WeaponRenderer {
    private final Surface body;
    private WavefrontObj model;

    public FlamerWeaponRenderer(String texture) {
        body = SpecialWeaponRenderers.texture(texture);
    }

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("flamethrower");
        return model;
    }

    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }

    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.5F;
    }

    @Override public float viewFov(ItemStack stack, float fov, float partialTick) {
        return aimingFov(stack, fov, partialTick, 0.33F);
    }

    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -1.5F * offset, -1.5F * offset, 2.75F * offset,
                0, -4.625 / 8D, 0.25);
    }

    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Gun"), body).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Tank"), body).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Gauge"), body).visibleIf(state -> !state.animated);
        declaration.part(obj.part("HeatShield"), body)
                .visibleIf(state -> !state.animated && SpecialWeaponRenderers.hasPath(state, "gun_flamer_daybreaker"));

        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated).scale(0.375F)
                .translate(0, 2, -6)
                .angle(Gem.Axes.X, state -> (float) -state.bus("EQUIP")[0])
                .translate(0, -2, 6)
                .translate(0, 1, 0)
                .angle(Gem.Axes.Z, state -> (float) state.bus("ROTATE")[2])
                .translate(0, -1, 0);
        root.child(obj.part("Gun"), body);
        root.child(obj.part("HeatShield"), body)
                .visibleIf(state -> SpecialWeaponRenderers.hasPath(state, "gun_flamer_daybreaker"));
        root.child(obj.part("Tank"), body);
        root.child(obj.part("Gauge"), body)
                .translate(1.25F, 1.25F, 0)
                .angle(Gem.Axes.Z, state -> -135F + 270F * SpecialWeaponRenderers.amount(state)
                        / Math.max(1F, state.stack.getItem() instanceof ItemGunBaseNT gun
                                ? gun.getPrimaryMagazine(state.stack, 0).getCapacity(state.stack) : 1F))
                .translate(-1.25F, -1.25F, 0);
    }

    @Override
    protected void setupInv(PoseStack pose, ItemStack stack) {
        super.setupInv(pose, stack);
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.mulPose(Axis.XP.rotationDegrees(25F));
        pose.mulPose(Axis.YP.rotationDegrees(45F));
        pose.translate(-1F, 1F, 0F);
    }

    @Override
    protected void setupThirdPerson(PoseStack pose, ItemStack stack) {
        super.setupThirdPerson(pose, stack);
        pose.scale(1.75F, 1.75F, 1.75F);
        pose.translate(0F, -3F, 4F);
    }
}
