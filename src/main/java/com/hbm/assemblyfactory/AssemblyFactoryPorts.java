package com.hbm.assemblyfactory;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import net.minecraft.core.BlockPos;

/** Port matching shared by the factory block's energy and fluid connector views. */
public final class AssemblyFactoryPorts {

    private AssemblyFactoryPorts() {}

    /** Includes the ordinary machine anchors and the four coolant proxy anchors. */
    public static boolean isPort(TileEntityMachineAssemblyFactory factory, BlockPos cell, ForgeDirection direction) {
        return com.hbm.lib.Ports.isPort(factory.getConPos(), cell, direction.toDirection())
                || com.hbm.lib.Ports.isPort(factory.getCoolPos(), cell, direction.toDirection());
    }

    /** Coolant proxy cells accept only the factory's water and spent-steam channels. */
    public static boolean isFluidPort(TileEntityMachineAssemblyFactory factory, BlockPos cell,
                                      ForgeDirection direction, FluidType type) {
        return com.hbm.lib.Ports.isPort(factory.getConPos(), cell, direction.toDirection())
                || (type != null && (type == Fluids.WATER || type == Fluids.SPENTSTEAM)
                && com.hbm.lib.Ports.isPort(factory.getCoolPos(), cell, direction.toDirection()));
    }
}
