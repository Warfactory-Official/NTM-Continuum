package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import net.minecraft.world.level.BlockGetter;

public interface IFluidConnectorBlockMK2 {

    boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir);
}
