package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;
import dan200.computercraft.api.lua.LuaFunction;

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

    @LuaFunction
    public final Object[] getFlux() {
        return read(s -> new Object[]{s.intAt(FLUX)});
    }

    @LuaFunction
    public final Object[] getProgress() {
        return read(s -> new Object[]{(float) s.doubleAt(PROGRESS)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.intAt(FLUX), (float) s.doubleAt(PROGRESS)});
    }
}
//?}
