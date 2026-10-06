package com.hbm.items;

import net.minecraft.world.item.ItemStack;

/**
 * An item that stands for one 1.7 metadata value of a split family (a member of an {@link ItemFamily}, or one of
 * several block items over one block). The number is derived from the item at run time - for code that still speaks
 * 1.7's {@code (item, meta)} (structure wands' replacement meta, QMAW manual pages) - and is never saved: the
 * {@code hbm:item_variant} component that used to carry it was removed (History §19es-itemvariant).
 */
public interface LegacyMeta {

    int legacyMeta();

    /** The 1.7 metadata the stack's item stands for, or 0 for an item that was never part of a metadata family. */
    static int of(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof LegacyMeta meta ? meta.legacyMeta() : 0;
    }
}
