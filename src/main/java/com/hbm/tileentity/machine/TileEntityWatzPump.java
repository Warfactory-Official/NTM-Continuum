package com.hbm.tileentity.machine;

import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Watz coolant pump's block entity. It holds no state and runs no logic — it exists because the
 * pump is a {@link com.hbm.blocks.BlockDummyable}, whose core cell must carry a block entity, and
 * because the OBJ renderer hangs off it. The reactor detects the pump by looking up this block, not
 * the other way round.
 */
public class TileEntityWatzPump extends TileEntityLoadedBase {

    public TileEntityWatzPump(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATZ_PUMP.get(), pos, state);
    }
}
