package com.hbm.compat.opencomputers;

//? if opencomputers {
/*import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/^*
 * OpenComputers block driver so Adapter blocks can interface with HBM machines and multiblock cells.
 ^/
public final class HbmDriverBlock implements DriverBlock {

    @Override
    public boolean worksWith(Level level, BlockPos pos, Direction side) {
        if (!(level instanceof ServerLevel server)) return false;
        return OpenComputersPeripherals.findPeripheral(server, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level level, BlockPos pos, Direction side) {
        if (!(level instanceof ServerLevel server)) return null;
        return OpenComputersPeripherals.at(server, pos, server.getBlockState(pos), server.getBlockEntity(pos), side);
    }
}
*///?}
