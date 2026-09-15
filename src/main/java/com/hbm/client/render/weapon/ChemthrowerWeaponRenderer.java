package com.hbm.client.render.weapon;

import com.hbm.client.model.WavefrontObj;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import net.minecraft.world.item.ItemStack;

/** GEM port of ItemRenderChemthrower. */
public final class ChemthrowerWeaponRenderer extends WeaponRenderer {
    private static final Surface BODY = SpecialWeaponRenderers.texture("chemthrower");
    private WavefrontObj model;

    private WavefrontObj model() {
        if (model == null) model = SpecialWeaponRenderers.model("chemthrower");
        return model;
    }

    @Override public org.joml.Vector3fc[] extents() { return SpecialWeaponRenderers.extents(model()); }

    @Override protected float getTurnMagnitude(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack) ? 2.5F : -0.25F;
    }

    @Override public void setupFirstPerson(ItemStack stack, PoseStack pose) {
        pose.translate(0, 0, 0.875);
        float offset = 0.8F;
        standardAimingTransform(stack, pose, -2.5F * offset, -2.5F * offset, 2.5F * offset,
                0, -4.375 / 8D, 1);
    }

    @Override protected void setup(Gem.Model<WeaponPose> declaration) {
        WavefrontObj obj = model();
        declaration.part(obj.part("Gun"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Hose"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Nozzle"), BODY).visibleIf(state -> !state.animated);
        declaration.part(obj.part("Gauge"), BODY).visibleIf(state -> !state.animated);
        Gem.Part<WeaponPose> root = declaration.joint().visibleIf(SpecialWeaponRenderers::animated)
                .scale(0.75F)
                .translate(0, -2, -4)
                .angle(Gem.Axes.X, state -> (float) -state.bus("EQUIP")[0])
                .translate(0, 2, 4)
                .rotate(Gem.Axes.Y, 90F);
        root.child(obj.part("Gun"), BODY);
        root.child(obj.part("Hose"), BODY);
        root.child(obj.part("Nozzle"), BODY);
        root.child(obj.part("Gauge"), BODY)
                .translate(0, 0.875F, 1.75F)
                .angle(Gem.Axes.X, state -> {
                    if (!(state.stack.getItem() instanceof ItemGunBaseNT gun)) return 135F;
                    var magazine = gun.getPrimaryMagazine(state.stack, 0);
                    if (magazine == null) return 135F;
                    return 135F - 270F * SpecialWeaponRenderers.amount(state)
                            / Math.max(1F, magazine.getCapacity(state.stack));
                })
                .translate(0, -0.875F, -1.75F);
    }
}
