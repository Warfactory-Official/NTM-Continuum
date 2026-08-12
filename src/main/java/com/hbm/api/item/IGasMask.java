package com.hbm.api.item;

import com.hbm.util.ArmorRegistry.HazardClass;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** A helmet which can hold one replaceable gas-mask filter. */
public interface IGasMask {
    List<HazardClass> getBlacklist(ItemStack stack);

    ItemStack getFilter(ItemStack stack);

    boolean isFilterApplicable(ItemStack stack, ItemStack filter);

    void installFilter(ItemStack stack, ItemStack filter);

    void damageFilter(ItemStack stack, int damage);
}
