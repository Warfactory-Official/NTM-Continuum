package com.hbm.api.hazard;

import com.hbm.hazard.HazardSystem;
import net.minecraft.world.item.ItemStack;

/** Hazard queries on live stacks (after registration; see the {@code hazards} content phase). */
public final class ItemHazards {

    private ItemHazards() {}

    /** Effective level of {@code type} on {@code stack}, modifiers applied. */
    public static double level(ItemStack stack, IHazardType type) {
        return HazardSystem.getHazardLevelFromStack(stack, type);
    }
}
