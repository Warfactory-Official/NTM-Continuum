package com.hbm.blocks.machine;

import com.hbm.blocks.BlockDummyable;
import com.hbm.tileentity.machine.TileEntityWatzPump;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The two-block coolant pump that caps a Watz column. Its presence three blocks above the bottom
 * segment's core, plus a redstone signal two blocks above that, is what switches the reactor on — the
 * pump has no logic of its own; the reactor looks it up.
 */
public class WatzPump extends BlockDummyable {

    public WatzPump(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWatzPump(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{1, 0, 0, 0, 0, 0}; }

    @Override public int getOffset() { return 0; }
}
