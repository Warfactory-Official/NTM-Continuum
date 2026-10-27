package com.hbm.blocks.generic;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** A block whose block items each stand for one of its states (a 1.7 metadata family such as the concrete colours). */
public interface ItemStateBlock {

    BlockState stateForItem(ItemStack stack);

    /** The state a block item stands for: its family member's state, else the block's default (1.7 block + item meta). */
    static BlockState of(BlockItem item, ItemStack stack) {
        return item.getBlock() instanceof ItemStateBlock family ? family.stateForItem(stack) : item.getBlock().defaultBlockState();
    }
}
