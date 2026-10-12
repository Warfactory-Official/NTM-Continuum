package com.hbm.hazard.modifier;

import com.hbm.hazard.HazardRegistry;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Depleted RBMK fuel's radiation curve, including xenon-135 ({@code hbm:rbmk_radiation}). */
public record HazardModifierRBMKRadiation(double target, boolean linear) implements IHazardModifier {
    @Override
    public double modify(ItemStack stack, LivingEntity entity, double level) {
        if (stack.getItem() instanceof ItemRBMKRod) {
            double e = ItemRBMKRod.getEnrichment(stack);
            level += (target - level) * (linear ? 1D - e : 1D - e * e);
            return level + HazardRegistry.xe135 * ItemRBMKRod.getPoisonLevel(stack);
        }
        if (stack.getItem() instanceof ItemRBMKPellet pellet) {
            int meta = ItemRBMKPellet.rectify(pellet.getVariant(stack));
            level += (target - level) * ((meta % 5) / 4D);
            return ItemRBMKPellet.hasXenon(meta) ? level + HazardRegistry.xe135 * HazardRegistry.nugget : level;
        }
        return level;
    }
}
