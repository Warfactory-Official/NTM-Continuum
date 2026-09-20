package com.hbm.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 1.7 {@code api.hbm.block.IInsertable}: a block the piston inserter can push one item into. */
public interface IInsertable { //uwu

    boolean insertItem(Level level, BlockPos pos, Direction dir, ItemStack stack);
}
