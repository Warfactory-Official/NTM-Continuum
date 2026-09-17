package com.hbm.interfaces;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Physical item storage not exposed through vanilla inventories or equipment. Both loaders visit it on the
 * server thread while its carrier is loaded. Report each owned stack once, never a preview or copied view;
 * a changed result requires publishing the modified stack through the carrier's synchronization mechanism.
 * Mutable visits defer notifications through {@link Visitor#afterChanges}. Stores with copied views or saved
 * mirrors use {@link Visitor#visitOwned} or {@link StoredItemSlots}; their setters publish before callbacks.
 */
public interface StoredItems {
    void visitStoredItems(Visitor visitor);

    interface Visitor {
        boolean visit(ItemStack stack);

        boolean visit(Container container);

        boolean visit(BlockEntity blockEntity);

        boolean visit(StoredItemSlots slots);

        boolean visitOwned(Container container);

        void afterChanges(Runnable notification);
    }
}
