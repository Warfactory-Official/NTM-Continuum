package com.hbm.tileentity.deco;

import com.hbm.blocks.generic.BlockSnowglobe;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code BlockSnowglobe.TileEntitySnowglobe}: holds the figure type; never ticks. */
public class TileEntitySnowglobe extends TileEntityTrinket {

    public TileEntitySnowglobe(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.SNOWGLOBE.get(), pos, state);
    }

    @Override
    protected int typeCount() {
        return BlockSnowglobe.SnowglobeType.VALUES.length;
    }

    public BlockSnowglobe.SnowglobeType type() {
        return BlockSnowglobe.SnowglobeType.of(typeOrdinal());
    }
}
