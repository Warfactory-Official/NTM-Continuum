package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityBatteryREDD}'s {@code ntm_energy_storage}: the arbitrary-precision store as a double. */
public final class BatteryREDDPeripheral extends EnergyStoragePeripheral<TileEntityBatteryREDD> {

    private static final int POWER = SLOTS;
    private static final int DELTA = SLOTS + 1;

    public BatteryREDDPeripheral(TileEntityBatteryREDD machine) {
        super(machine, SLOTS + 2, 0);
    }

    @Override
    protected void capture(TileEntityBatteryREDD machine) {
        super.capture(machine);
        put(POWER, machine.power.doubleValue());
        put(DELTA, machine.delta.longValue());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.doubleAt(POWER), s.longAt(DELTA)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.doubleAt(POWER), s.longAt(DELTA), s.intAt(RED_LOW), s.intAt(RED_HIGH),
                s.intAt(PRIORITY)});
    }
}
//?}
