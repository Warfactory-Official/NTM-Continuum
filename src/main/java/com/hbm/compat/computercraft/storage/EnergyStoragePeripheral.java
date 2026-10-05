package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.storage.TileEntityBatteryBase;
import dan200.computercraft.api.lua.LuaFunction;

/**
 * 1.7 {@code TileEntityBatteryBase}'s component {@code ntm_energy_storage}: the mode half every battery shares. 1.7's
 * base {@code getEnergyInfo}/{@code getInfo} are overridden by both of its subclasses (socket, REDD), so each
 * subclass peripheral supplies its own.
 */
public abstract class EnergyStoragePeripheral<BE extends TileEntityBatteryBase> extends SnapshotPeripheral<BE> {

    protected static final int RED_LOW = 0;
    protected static final int RED_HIGH = 1;
    protected static final int PRIORITY = 2;
    protected static final int SLOTS = 3;

    protected EnergyStoragePeripheral(BE machine, int longs, int refs) {
        super(machine, "ntm_energy_storage", longs, refs);
    }

    @Override
    protected void capture(BE machine) {
        put(RED_LOW, machine.redLow);
        put(RED_HIGH, machine.redHigh);
        put(PRIORITY, machine.getPriority().ordinal() - 1);
    }

    @LuaFunction
    public final Object[] getModeInfo() {
        return read(s -> new Object[]{s.intAt(RED_LOW), s.intAt(RED_HIGH), s.intAt(PRIORITY)});
    }

    // OpenComputers' checkInteger truncates any number, where a CC int parameter rejects a fraction: take a double
    @LuaFunction(mainThread = true)
    public final Object[] setModeLow(double mode) {
        short newMode = (short) (int) mode;
        if (newMode < TileEntityBatteryBase.mode_input || newMode > TileEntityBatteryBase.mode_none) {
            return new Object[]{"Invalid mode"};
        }
        BE machine = machine();
        machine.redLow = newMode;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setModeHigh(double mode) {
        short newMode = (short) (int) mode;
        if (newMode < TileEntityBatteryBase.mode_input || newMode > TileEntityBatteryBase.mode_none) {
            return new Object[]{"Invalid mode"};
        }
        BE machine = machine();
        machine.redHigh = newMode;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPriority(double priority) {
        int newPriority = (int) priority;
        if (newPriority < 0 || newPriority > 2) return new Object[]{"Invalid mode"};
        BE machine = machine();
        // 1.7 EnumUtil.grabEnumSafely(ConnectionPriority.class, newPriority + 1): LOW, NORMAL or HIGH
        machine.priority = ConnectionPriority.VALUES[newPriority + 1];
        machine.setChanged();
        return new Object[]{};
    }
}
//?}
