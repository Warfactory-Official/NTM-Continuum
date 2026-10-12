package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** An RBMK rod's HOT level from its hull heat ({@code hbm:rbmk_hot}). */
public record HazardModifierRBMKHot() implements IHazardModifier {
    @Override
    public double modify(ItemStack stack, LivingEntity entity, double level) {
        return stack.getItem() instanceof ItemRBMKRod ? Math.min(Math.ceil((ItemRBMKRod.getHullHeat(stack) - 100D) / 10D), 60D) : 0D;
    }
}
