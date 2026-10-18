package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.TileEntityRadioAUTOCAL;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.nbt.CompoundTag;

/**
 * 1.7 {@code TileEntityRadioAUTOCAL}'s component {@code radio_autocal}. Every method runs on the server thread, since
 * the script and buffer are plain strings the interpreter rewrites each tick.
 */
public final class RadioAutocalPeripheral extends SnapshotPeripheral<TileEntityRadioAUTOCAL> {

    public RadioAutocalPeripheral(TileEntityRadioAUTOCAL machine) {
        super(machine, "radio_autocal", 0, 0);
    }

    @Override
    protected void capture(TileEntityRadioAUTOCAL machine) {}

    @LuaFunction(mainThread = true)
    public final Object[] setBuffer(String buffer) {
        TileEntityRadioAUTOCAL machine = machine();
        if (machine.ctx == null) return new Object[]{};
        return new Object[]{machine.ctx.writeBuffer(buffer)};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getBuffer() {
        TileEntityRadioAUTOCAL machine = machine();
        return new Object[]{machine.ctx == null ? "" : machine.ctx.readBuffer()};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setScript(String script) {
        CompoundTag data = new CompoundTag();
        data.putString("payload", script);
        machine().receiveControl(data);
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getScript() {
        StringBuilder joined = new StringBuilder();
        for (String line : machine().script) joined.append(line).append('\n');
        return new Object[]{joined.toString()};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setState(boolean state) {
        TileEntityRadioAUTOCAL machine = machine();
        if (state) machine.isOn = true;
        else machine.stop("User requested shutdown");
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getState() {
        return new Object[]{machine().isOn};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setIgnoreError(boolean ignore) {
        machine().ignoreError = ignore;
        machine().setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getIgnoreError() {
        return new Object[]{machine().ignoreError};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setAutoReboot(boolean auto) {
        machine().autoReboot = auto;
        machine().setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getAutoReboot() {
        return new Object[]{machine().autoReboot};
    }

    // 1.7 checkInteger: a fraction truncates
    @LuaFunction(mainThread = true)
    public final Object[] getHistory(double index) {
        String[] history = machine().history;
        int i = (int) index;
        return i >= 0 && i < history.length ? new Object[]{history[i]} : new Object[]{};
    }
}
//?}
