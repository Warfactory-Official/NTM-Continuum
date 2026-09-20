package com.hbm.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code ItemConveyorWand.construct}'s {@code IBlockAccess buildWorld}: the level on a commit, a scratch
 * volume ({@code WorldInAJar}) for the wand's ghost preview. Reads answer what the placer itself holds, so a
 * lift's part or a chute's connections see the run being laid rather than the world it is about to replace.
 */
public interface ConveyorPlacer extends BlockGetter {

    void place(BlockPos pos, BlockState state);
}
