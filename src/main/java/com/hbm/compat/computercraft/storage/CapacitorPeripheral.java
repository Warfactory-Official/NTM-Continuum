package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.TileEntityCapacitor;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code MachineCapacitor.TileEntityCapacitor}'s component {@code capacitor}. */
public final class CapacitorPeripheral extends SnapshotPeripheral<TileEntityCapacitor> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int RECEIVED = 2;
    private static final int SENT = 3;

    public CapacitorPeripheral(TileEntityCapacitor machine) {
        super(machine, "capacitor", 4, 0);
    }

    @Override
    protected void capture(TileEntityCapacitor machine) {
        put(POWER, machine.power);
        put(MAX_POWER, machine.getMaxPower());
        put(RECEIVED, machine.lastPowerReceived);
        put(SENT, machine.lastPowerSent);
    }

    @LuaFunction
    public final Object[] getEnergy() {
        return read(s -> new Object[]{s.longAt(POWER)});
    }

    @LuaFunction
    public final Object[] getMaxEnergy() {
        return read(s -> new Object[]{s.longAt(MAX_POWER)});
    }

    // 1.7 crosses the names: "sent" answers what was received, and the other way round
    @LuaFunction
    public final Object[] getEnergySent() {
        return read(s -> new Object[]{s.longAt(RECEIVED)});
    }

    @LuaFunction
    public final Object[] getEnergyReceived() {
        return read(s -> new Object[]{s.longAt(SENT)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.longAt(RECEIVED), s.longAt(SENT)});
    }
}
//?}
