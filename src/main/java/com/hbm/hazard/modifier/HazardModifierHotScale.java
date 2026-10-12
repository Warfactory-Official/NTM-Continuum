package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemDepletedFuel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Scales an entry down to {@code coldFactor} while a spent plate is not hot ({@code hbm:hot_scale}). 1.7 had the hot and
 * cold plates as two metadata subtypes with their own registrations; the port's plate carries a hot flag instead.
 */
public record HazardModifierHotScale(double coldFactor) implements IHazardModifier {
    @Override
    public double modify(ItemStack stack, LivingEntity holder, double level) {
        return ItemDepletedFuel.isHot(stack) ? level : level * coldFactor;
    }
}
