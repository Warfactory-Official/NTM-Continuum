package com.hbm.tileentity.network;

import com.hbm.tileentity.ModFluidNetBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.blocks.network.FluidSwitch;

/** The redstone-driven valve's tile: the 1.7 {@code TileEntityFluidValve} under a {@code FluidSwitch}. */
public class TileEntityFluidSwitch extends TileEntityFluidValve {

    public TileEntityFluidSwitch(BlockPos pos, BlockState state) {
        super(ModFluidNetBlockEntities.SWITCH.get(), pos, state);
    }

    protected TileEntityFluidSwitch(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
