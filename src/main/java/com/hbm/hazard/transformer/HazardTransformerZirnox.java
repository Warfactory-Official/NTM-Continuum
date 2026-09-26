package com.hbm.hazard.transformer;

import com.hbm.api.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemZirnoxWaste;
import com.hbm.items.machine.ZirnoxFuel;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Applies the original component-sensitive radiation curves for active rods and hot spent fuel. */
public final class HazardTransformerZirnox implements IHazardTransformer {

    @Override
    public void transformPre(ItemStack stack, List<HazardEntry> entries) {}

    @Override
    public void transformPost(ItemStack stack, List<HazardEntry> entries) {
        if (stack.getItem() instanceof ItemZirnoxRod rod) {
            ZirnoxFuel fuel = rod.fuel();
            float progress = Math.min(1F, (float) rod.getLife(stack) / fuel.maxLife);
            float radiation = (float) (fuel.baseRadiation
                    + (fuel.depletedRadiation - fuel.baseRadiation) * Math.pow(progress, 0.4D));
            entries.add(new HazardEntry(HazardRegistry.RADIATION, radiation));
        }
        if (stack.getItem() instanceof ItemZirnoxWaste && ItemZirnoxWaste.isHot(stack)) {
            entries.add(new HazardEntry(HazardRegistry.HOT, 5F));
        }
    }
}
