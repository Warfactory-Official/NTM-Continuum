package com.hbm.items;

import net.minecraft.world.item.ItemStack;

/**
 * An item that keeps one registered id but draws different sprites by its typed state (a cell by its fluid, a pipette
 * by its fill, an open toolbox, a stirling engine without its cog, an RBMK pellet by its stage). The value is computed
 * from that state on the client for the {@code hbm:model_state} item-model select property and never stored: it
 * replaced the saved {@code hbm:item_variant} int (History §19es-itemvariant).
 */
public interface ModelState {

    int modelState(ItemStack stack);

    /** The item's state, or its block's when the item is a plain block item of a {@code ModelState} block. */
    static int of(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.getItem() instanceof ModelState state) return state.modelState(stack);
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof ModelState state) {
            return state.modelState(stack);
        }
        return 0;
    }
}
