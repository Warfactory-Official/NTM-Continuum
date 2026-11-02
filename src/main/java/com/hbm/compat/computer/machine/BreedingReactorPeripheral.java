package com.hbm.compat.computer.machine;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;
import dev.golem.core.computer.ComputerMethod;

/** 1.7 {@code TileEntityMachineReactorBreeding}'s component {@code breeding_reactor}. */
public final class BreedingReactorPeripheral extends SnapshotPeripheral<TileEntityMachineReactorBreeding> {

    private static final int FLUX = 0;
    private static final int PROGRESS = 1;

    public BreedingReactorPeripheral(TileEntityMachineReactorBreeding machine) {
        super(machine, "breeding_reactor", 2, 0);
    }

    @Override
    protected void capture(TileEntityMachineReactorBreeding machine) {
        put(FLUX, machine.flux);
        put(PROGRESS, machine.progress);
    }

    @ComputerMethod
    public final Object[] getFlux() {
        return read(s -> new Object[]{s.intAt(FLUX)});
    }

    @ComputerMethod
    public final Object[] getProgress() {
        return read(s -> new Object[]{(float) s.doubleAt(PROGRESS)});
    }

    @ComputerMethod
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.intAt(FLUX), (float) s.doubleAt(PROGRESS)});
    }
}
