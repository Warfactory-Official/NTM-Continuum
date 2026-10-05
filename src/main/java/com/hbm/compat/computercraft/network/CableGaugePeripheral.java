package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.TileEntityCableGauge;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code BlockCableGauge.TileEntityCableGauge}'s component {@code ntm_power_gauge}. */
public final class CableGaugePeripheral extends SnapshotPeripheral<TileEntityCableGauge> {

    private static final int DELTA_TICK = 0;
    private static final int DELTA_SECOND = 1;
    private static final int X = 2;
    private static final int Y = 3;
    private static final int Z = 4;

    public CableGaugePeripheral(TileEntityCableGauge machine) {
        super(machine, "ntm_power_gauge", 5, 0);
    }

    @Override
    protected void capture(TileEntityCableGauge machine) {
        put(DELTA_TICK, machine.deltaTick);
        put(DELTA_SECOND, machine.deltaLastSecond);
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
    }

    @LuaFunction
    public final Object[] getTransfer() {
        return read(s -> new Object[]{s.longAt(DELTA_TICK), s.longAt(DELTA_SECOND)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(DELTA_TICK), s.longAt(DELTA_SECOND), s.intAt(X), s.intAt(Y),
                s.intAt(Z)});
    }
}
//?}
