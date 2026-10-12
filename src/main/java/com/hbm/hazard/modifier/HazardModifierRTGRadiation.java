package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1.7: interpolates a pellet's radiation toward its decay target by the life used up, {@code 1 - life / maxLife} ({@code hbm:rtg_radiation}). */
public record HazardModifierRTGRadiation(double target) implements IHazardModifier {

    @Override
    public double modify(ItemStack stack, LivingEntity holder, double level) {
        if (stack.getItem() instanceof ItemRTGPellet pellet) {
            double depletion = 1D - ItemRTGPellet.getRemaining(pellet, stack);
            level = level + (target - level) * depletion;
        }
        return level;
    }
}
