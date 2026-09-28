package com.hbm.blocks.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code BlockCMGlass} ({@code cm_tank}): not opaque, and a face against another tank of the same block is
 * not drawn, so a wall of tanks reads as one pane.
 */
public class BlockCMGlass extends BlockCM {

    public BlockCMGlass(Properties properties) {
        super(properties);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, side);
    }
}
