package com.hbm.blocks.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.util.ConveyorBend;

/** 1.7 {@code blocks/network/BlockConveyor}: the plain belt, the only type whose sneak-screw ring reaches the lift. */
public class BlockConveyor extends BlockConveyorBendable {

    public BlockConveyor(Properties properties) {
        super(properties);
    }

    /** 1.7: straight, left, right, then the "switcheroo" into a lift carrying the straight metadata. */
    @Override
    protected void onSneakScrew(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(BEND) != ConveyorBend.RIGHT) {
            super.onSneakScrew(level, pos, state);
            return;
        }

        level.setBlock(pos, ModConveyorBlocks.CONVEYOR_LIFT.get().defaultBlockState()
                .setValue(FACING, state.getValue(FACING)), UPDATE_ALL);
    }
}
