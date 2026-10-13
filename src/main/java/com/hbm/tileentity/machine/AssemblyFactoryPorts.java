package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import net.minecraft.core.BlockPos;

/** Port matching shared by the factory block's energy and fluid connector views. */
public final class AssemblyFactoryPorts {

    private AssemblyFactoryPorts() {}

    /** Includes the ordinary machine anchors and the four coolant proxy anchors. */
    public static boolean isPort(TileEntityMachineAssemblyFactory factory, BlockPos cell, ForgeDirection direction) {
        return IEnergyConnectorBlock.isPort(factory.getConPos(), cell, direction)
                || IEnergyConnectorBlock.isPort(factory.getCoolPos(), cell, direction);
    }

    /** Coolant proxy cells accept only the factory's water and spent-steam channels. */
    public static boolean isFluidPort(TileEntityMachineAssemblyFactory factory, BlockPos cell,
                                      ForgeDirection direction, FluidType type) {
        return IEnergyConnectorBlock.isPort(factory.getConPos(), cell, direction)
                || (type != null && (type == Fluids.WATER || type == Fluids.SPENTSTEAM)
                && IEnergyConnectorBlock.isPort(factory.getCoolPos(), cell, direction));
    }
}
