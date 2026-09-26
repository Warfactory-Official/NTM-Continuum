package com.hbm.blocks.multiblock;

import com.hbm.api.fluid.FluidType;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/** Duct answer of a {@link MultiblockCore}: baked fluid-role ports, gated per fluid by {@link #acceptsFluid}. */
public interface FoldedFluidPorts extends IFluidConnectorBlockMK2 {

    /** 1.7 gates the type at the face's call site (turbine: coolant/spent steam; intake: air). */
    default boolean acceptsFluid(FluidType type) {
        return true;
    }

    @Override
    default boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, Direction dir) {
        return acceptsFluid(type)
                && ((MultiblockCore) this).isPortAt(world, new BlockPos(x, y, z), dir, MultiblockCore.ROLE_FLUID);
    }
}
