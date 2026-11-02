package com.hbm.compat.computer.storage;

import com.hbm.tileentity.machine.TileEntityMachineFluidTank;

/** 1.7 {@code TileEntityMachineFluidTank}'s {@code ntm_fluid_tank}. */
public final class MachineFluidTankPeripheral extends FluidTankPeripheral<TileEntityMachineFluidTank> {

    public MachineFluidTankPeripheral(TileEntityMachineFluidTank machine) {
        super(machine, machine.tank);
    }
}
