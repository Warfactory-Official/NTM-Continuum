package com.hbm.hazard.transformer;

import java.util.List;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.items.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Adds radiation to any stack carrying the {@code hbm:hazard_radiation} data component — the 26.1
 * re-implementation of the old {@code hfrHazRadiation} NBT hook (used by pack-makers/compat to make
 * arbitrary items radioactive).
 */
public class HazardTransformerRadiationNBT implements IHazardTransformer {

    @Override
    public void transformPre(final ItemStack stack, final List<HazardEntry> entries) {
    }

    @Override
    public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {
        Float rad = stack.get(ModDataComponents.HAZARD_RADIATION.get());
        if (rad != null && rad > 0) {
            entries.add(new HazardEntry(HazardRegistry.RADIATION, rad));
        }
    }
}
