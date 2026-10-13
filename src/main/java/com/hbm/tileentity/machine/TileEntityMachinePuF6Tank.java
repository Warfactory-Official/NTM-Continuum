package com.hbm.tileentity.machine;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Plutonium hexafluoride tank BlockEntity. */
public class TileEntityMachinePuF6Tank extends TileEntityMachineF6Tank {

    public TileEntityMachinePuF6Tank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PUF6_TANK.get(), pos, state, Fluids.PUF6);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  TileEntityMachinePuF6Tank tank) {
        TileEntityMachineF6Tank.serverTick(level, pos, state, tank);
    }

    @Override
    public String getDefaultName() {
        return "container.puf6_tank";
    }
}
