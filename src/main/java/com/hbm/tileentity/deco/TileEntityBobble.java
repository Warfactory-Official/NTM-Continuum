package com.hbm.tileentity.deco;

import com.hbm.blocks.generic.BlockBobble;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code BlockBobble.TileEntityBobble}: holds the figure type; never ticks. */
public class TileEntityBobble extends TileEntityTrinket {

    public TileEntityBobble(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.BOBBLE.get(), pos, state);
    }

    @Override
    protected Enum<?>[] typeValues() {
        return BlockBobble.BobbleType.VALUES;
    }

    public BlockBobble.BobbleType type() {
        return BlockBobble.BobbleType.of(typeOrdinal());
    }
}
