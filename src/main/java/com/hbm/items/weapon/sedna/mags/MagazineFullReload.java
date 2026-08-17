package com.hbm.items.weapon.sedna.mags;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Loads the whole magazine in one action — the ordinary box-magazine or speedloader reload. */
public class MagazineFullReload extends MagazineSingleTypeBase {

    public MagazineFullReload(int index, int capacity) {
        super(index, capacity);
    }

    /** Fills in one cycle. An empty magazine adopts the first accepted round found in the inventory. */
    @Override
    public void reloadAction(ItemStack stack, Container inventory) {
        standardReload(stack, inventory, this.capacity);
    }
}
