package com.hbm.api.tab;

import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.RegistryObject;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** NTM's creative tabs. Contents are read when a tab builds, so adding is valid until then. */
public final class HbmTabs {

    private HbmTabs() {}

    public static void add(HbmTab tab, RegistryObject<? extends Item> item) {
        ModTabs.addTo(tab, item);
    }

    /** Display stacks (variants carried on components), resolved when the tab builds. */
    public static void addStacks(HbmTab tab, Supplier<List<ItemStack>> stacks) {
        ModTabs.addStacksTo(tab, stacks);
    }

    public static ResourceKey<CreativeModeTab> key(HbmTab tab) {
        return ModTabs.get(tab).getKey();
    }

    /** Whether {@code item} was added to {@code tab} as an item entry. */
    public static boolean contains(HbmTab tab, Item item) {
        return ModTabs.CONTENTS.get(key(tab)).stream().anyMatch(entry -> entry.get() == item);
    }
}
