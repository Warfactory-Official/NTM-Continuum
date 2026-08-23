package com.hbm.tileentity.machine.rbmk;

import net.minecraft.world.item.ItemStack;

/** RBMK crane loading contract. */
public interface IRBMKLoadable {
    boolean canLoad(ItemStack stack);
    void load(ItemStack stack);
    boolean canUnload();
    ItemStack provideNext();
    void unload();
}
