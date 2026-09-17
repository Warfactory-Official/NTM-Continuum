package com.hbm.interfaces;

import net.minecraft.world.item.ItemStack;

/**
 * A physical, locally owned slot array visited on the server thread on both loaders. Reads return the owned
 * stack, not an extraction view. Replacement must update persisted contents before invoking world callbacks;
 * refusal changes nothing. Open or closing transactions refuse mutation. Virtual and infinite slots are absent.
 */
public interface StoredItemSlots {
    int storedSlotCount();

    ItemStack storedItem(int slot);

    boolean replaceStoredItem(int slot, ItemStack expected, ItemStack replacement);
}
