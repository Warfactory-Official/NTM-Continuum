package com.hbm.gametest;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A battery with a port on every side, for the network tests. 1.7's own battery gives its ports no direction, so it
 * reaches no cable at all.
 */
public final class PortedBattery extends TileEntityMachineBattery {

    private PortedBattery(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        return cachedPorts;
    }

    public static TileEntityMachineBattery place(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, ModMachineBlocks.machine_battery.get().defaultBlockState(), 3);
        PortedBattery battery = new PortedBattery(pos, level.getBlockState(pos));
        level.setBlockEntity(battery);
        return battery;
    }
}
