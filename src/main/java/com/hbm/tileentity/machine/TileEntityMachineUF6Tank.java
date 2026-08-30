package com.hbm.tileentity.machine;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Uranium hexafluoride tank BlockEntity. */
public class TileEntityMachineUF6Tank extends TileEntityMachineF6Tank {

    public TileEntityMachineUF6Tank(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_UF6_TANK.get(), pos, state, Fluids.UF6);
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                  TileEntityMachineUF6Tank tank) {
        TileEntityMachineF6Tank.serverTick(level, pos, state, tank);
    }

    @Override
    public String getDefaultName() {
        return "container.uf6_tank";
    }
}
