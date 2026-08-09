package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import net.minecraft.world.level.BlockGetter;

/**
 * Block-level connection check, so a multiblock machine footprint can connect to fluid ducts without a
 * BlockEntity at every cell (mirrors {@link com.hbm.api.energymk2.IEnergyConnectorBlock}).
 */
public interface IFluidConnectorBlockMK2 {

    /** dir is the face that is connected to, the direction going outwards from the block. */
    boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir);
}
