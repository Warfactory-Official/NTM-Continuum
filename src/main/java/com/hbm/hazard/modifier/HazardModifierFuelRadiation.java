package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemPlateFuel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Interpolates a fuel item's radiation from fresh to depleted, as in the old hazard registry ({@code hbm:fuel_radiation}). */
public record HazardModifierFuelRadiation(double target) implements IHazardModifier {

    @Override
    public double modify(ItemStack stack, LivingEntity entity, double level) {
        if (stack.getItem() instanceof ItemPlateFuel fuel) {
            double depletion = Math.pow(fuel.getDurabilityForDisplay(stack), 0.4D);
            return level + (target - level) * depletion;
        }
        return level;
    }
}
