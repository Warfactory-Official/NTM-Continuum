package com.hbm.hazard.transformer;

import java.util.List;

import com.hbm.hazard.HazardEntry;
import net.minecraft.world.item.ItemStack;

/**
 * Stack-sensitive hazard rewriting (runs on every lookup, before/after the registered data is
 * collected). Ported from the fork's {@code IHazardTransformer}. The ForgeFluid/ME/Container/custom
 * transformers are deferred with their systems.
 */
public interface IHazardTransformer {

    void transformPre(ItemStack stack, List<HazardEntry> entries);

    void transformPost(ItemStack stack, List<HazardEntry> entries);
}
