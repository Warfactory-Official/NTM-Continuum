package com.hbm.hazard.transformer;

import java.util.List;

import com.hbm.hazard.HazardEntry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.items.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import dev.golem.core.hook.Hook;
import java.util.function.ToDoubleFunction;

/**
 * Adds radiation to any stack carrying the {@code hbm:hazard_radiation} data component: the 26.1
 * re-implementation of the old {@code hfrHazRadiation} NBT hook (used by pack-makers/compat to make
 * arbitrary items radioactive).
 */
public class HazardTransformerRadiationNBT implements IHazardTransformer {

    @Override
    public boolean readsLiveStorage() { return true; }

    @Override
    public boolean readsLiveStorage(ItemStack stack) { return ModDataComponents.HAZARD_RADIATION.has(stack); }

    /** Per-item activation added to explicit component radiation before the positivity check. */
    public static final Hook<ToDoubleFunction<ItemStack>> COMPONENT_RADIATION = Hook.notifying();

    @Override
    public void transformPre(final ItemStack stack, final List<HazardEntry> entries) {
    }

    @Override
    public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {
        Float rad = ModDataComponents.hazardRadiation(stack);
        if (rad != null) {
            double combined = rad;
            for (var contribution : COMPONENT_RADIATION) combined += contribution.applyAsDouble(stack);
            if (combined > 0) entries.add(new HazardEntry(HazardRegistry.RADIATION, combined));
        }
    }
}
