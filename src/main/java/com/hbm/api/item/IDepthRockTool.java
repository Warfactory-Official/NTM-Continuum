package com.hbm.api.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A held tool that may opt into the legacy depth-rock hardness override. */
public interface IDepthRockTool {
    boolean canBreakRock(Level level, Player player, ItemStack tool, BlockState state, BlockPos pos);
}
