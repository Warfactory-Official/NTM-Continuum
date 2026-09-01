package com.hbm.tileentity.machine.storage;

import dev.golem.core.world.ItemStacks;
import net.minecraft.world.item.ItemStack;

/** Reads the state carried by a mass-storage block item, without exposing item-era details to callers. */
public final class MassStorageItemData {

    private MassStorageItemData() {}

    public static ItemStack filter(ItemStack stack) {
        return ItemStacks.carriedItem(stack, "inventory", 3, TileEntityMassStorage.FILTER_SLOT);
    }

    public static int stockpile(ItemStack stack) {
        return ItemStacks.carriedInt(stack, "stack");
    }

    public static boolean output(ItemStack stack) {
        return ItemStacks.carriedBoolean(stack, "output");
    }
}
