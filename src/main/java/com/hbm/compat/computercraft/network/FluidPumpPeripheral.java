package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.TileEntityFluidPump;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code FluidPump.TileEntityFluidPump}'s component {@code ntm_fluid_pump}. */
public final class FluidPumpPeripheral extends SnapshotPeripheral<TileEntityFluidPump> {

    private static final int PRESSURE = 0;
    private static final int FLOW = 1;
    private static final int FLUID = 0;
    private static final int PRIORITY = 1;

    public FluidPumpPeripheral(TileEntityFluidPump machine) {
        super(machine, "ntm_fluid_pump", 2, 2);
    }

    @Override
    protected void capture(TileEntityFluidPump machine) {
        put(PRESSURE, machine.tank[0].getPressure());
        put(FLOW, machine.bufferSize);
        // 1.7 FluidType.getUnlocalizedName, "hbmfluid.<name>"
        putRef(FLUID, machine.tank[0].getTankType().getTranslationKey());
        // OpenComputers answers an enum it has no converter for with toString()
        putRef(PRIORITY, machine.getFluidPriority().toString());
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.refAt(FLUID)});
    }

    @LuaFunction
    public final Object[] getPressure() {
        return read(s -> new Object[]{s.intAt(PRESSURE)});
    }

    @LuaFunction
    public final Object[] getFlow() {
        return read(s -> new Object[]{s.intAt(FLOW)});
    }

    @LuaFunction
    public final Object[] getPriority() {
        return read(s -> new Object[]{s.refAt(PRIORITY)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.refAt(FLUID), s.intAt(PRESSURE), s.intAt(FLOW), s.refAt(PRIORITY)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPriority(double priority) {
        ConnectionPriority chosen;
        switch ((int) priority) {
            case 0: chosen = ConnectionPriority.LOWEST; break;
            case 1: chosen = ConnectionPriority.LOW; break;
            case 2: chosen = ConnectionPriority.NORMAL; break;
            case 3: chosen = ConnectionPriority.HIGH; break;
            case 4: chosen = ConnectionPriority.HIGHEST; break;
            default: return new Object[]{null, "Not a valid Priority."};
        }
        TileEntityFluidPump machine = machine();
        machine.priority = chosen;
        machine.setChanged();
        return new Object[]{true};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setFlow(double flow) {
        int input = (int) flow;
        if (input > 10000 || input < 0) return new Object[]{null, "Number outside of bounds."};
        TileEntityFluidPump machine = machine();
        machine.bufferSize = input;
        machine.setChanged();
        return new Object[]{true};
    }
}
//?}
