package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.TileEntityFluidCounterValve;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityFluidCounterValve}'s component {@code ntm_fluid_counter_valve}. */
public final class FluidCounterValvePeripheral extends SnapshotPeripheral<TileEntityFluidCounterValve> {

    private static final int COUNTER = 0;
    private static final int OPEN = 1;
    private static final int FLUID = 0;

    public FluidCounterValvePeripheral(TileEntityFluidCounterValve machine) {
        super(machine, "ntm_fluid_counter_valve", 2, 1);
    }

    @Override
    protected void capture(TileEntityFluidCounterValve machine) {
        put(COUNTER, machine.getCounter());
        put(OPEN, machine.isOpen());
        putRef(FLUID, PipeFluid.name(machine.getFluidType()));
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.refAt(FLUID)});
    }

    @LuaFunction
    public final Object[] getCounter() {
        return read(s -> new Object[]{s.longAt(COUNTER)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] resetCounter() {
        machine().resetCounter();
        return new Object[]{};
    }

    @LuaFunction
    public final Object[] getState() {
        return read(s -> new Object[]{s.booleanAt(OPEN) ? 1 : 0});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setState(double value) throws LuaException {
        int state = (int) value;
        // 1.7 throws a message-less IllegalArgumentException, which OpenComputers raises as "bad argument"
        if (state != 0 && state != 1) throw new LuaException("bad argument");
        machine().setState(state);
        return new Object[]{};
    }
}
//?}
