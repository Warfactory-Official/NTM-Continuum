package com.hbm.hazard.modifier;

import java.util.List;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Scales a hazard level by context (e.g. RBMK rod depletion). The concrete RBMK/RTG/fuel modifiers
 * are deferred until their items are ported; the interface + evaluation order are kept so registered
 * entries carry them unchanged.
 */
public interface IHazardModifier {

    double modify(ItemStack stack, LivingEntity holder, double level);

    /** Returns the level after applying all modifiers to it, in order. {@code entity} nullable. */
    static double evalAllModifiers(final ItemStack stack, final LivingEntity entity, double level, final List<IHazardModifier> mods) {
        for (final IHazardModifier mod : mods) {
            level = mod.modify(stack, entity, level);
        }
        return level;
    }
}
