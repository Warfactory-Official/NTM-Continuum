package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityRadioTorchReader}'s component {@code radio_reader}; indices start at 0 as in 1.7. */
public final class RadioReaderPeripheral extends SnapshotPeripheral<TileEntityRadioTorchReader> {

    public RadioReaderPeripheral(TileEntityRadioTorchReader machine) {
        super(machine, "radio_reader", 0, 0);
    }

    @Override
    protected void capture(TileEntityRadioTorchReader machine) {}

    private static boolean valid(int index) {
        return index >= 0 && index < TileEntityRadioTorchReader.MAPPING_SIZE;
    }

    // 1.7 checkInteger: a fraction truncates
    @LuaFunction(mainThread = true)
    public final Object[] setChannel(double index, String channel) {
        TileEntityRadioTorchReader machine = machine();
        if (valid((int) index)) machine.setChannel((int) index, channel);
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getChannel(double index) {
        return valid((int) index) ? new Object[]{machine().channels[(int) index]} : new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setName(double index, String name) {
        TileEntityRadioTorchReader machine = machine();
        if (valid((int) index)) machine.setName((int) index, name);
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getName(double index) {
        return valid((int) index) ? new Object[]{machine().names[(int) index]} : new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPolling(boolean polling) {
        machine().polling = polling;
        machine().setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getPolling() {
        return new Object[]{machine().polling};
    }

    @LuaFunction(mainThread = true)
    public final Object[] read(double index) {
        return valid((int) index) ? new Object[]{machine().prev[(int) index]} : new Object[]{};
    }
}
//?}
