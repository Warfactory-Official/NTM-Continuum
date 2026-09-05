package com.hbm.hazard.modifier;

import com.hbm.items.machine.ItemRTGPellet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Interpolates a pellet's radiation toward its decay target using the exact power decay curve. */
public final class HazardModifierRTGRadiation implements IHazardModifier {
    private final double target;

    public HazardModifierRTGRadiation(double target) {
        this.target = target;
    }

    @Override
    public double modify(ItemStack stack, LivingEntity holder, double level) {
        if (stack.getItem() instanceof ItemRTGPellet pellet) {
            double depletion = 1D - ItemRTGPellet.getDecay(pellet, stack);
            level = level + (target - level) * depletion;
        }
        return level;
    }
}
