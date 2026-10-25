package com.hbm.inventory;

import dev.golem.core.world.ItemStacks;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A crate slot: like a shulker box, it takes nothing that carries other items. */
public class SlotCrate extends Slot {

    public SlotCrate(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return ItemStacks.fitsInContainerItem(stack);
    }
}
