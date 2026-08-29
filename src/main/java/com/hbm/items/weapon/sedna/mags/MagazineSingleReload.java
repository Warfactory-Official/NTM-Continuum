package com.hbm.items.weapon.sedna.mags;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Loads one round per reload cycle, as used by lever-action and tube-fed weapons. */
public final class MagazineSingleReload extends MagazineSingleTypeBase {

    public MagazineSingleReload(int index, int capacity) {
        super(index, capacity);
    }

    /** The one-round limit is what makes the state machine emit RELOAD_CYCLE repeatedly. */
    @Override
    public void reloadAction(ItemStack stack, Container inventory) {
        standardReload(stack, inventory, 1);
    }
}
