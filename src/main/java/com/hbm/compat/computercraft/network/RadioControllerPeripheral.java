package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.TileEntityRadioTorchController;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityRadioTorchController}'s component {@code radio_controller}. */
public final class RadioControllerPeripheral extends SnapshotPeripheral<TileEntityRadioTorchController> {

    public RadioControllerPeripheral(TileEntityRadioTorchController machine) {
        super(machine, "radio_controller", 0, 0);
    }

    @Override
    protected void capture(TileEntityRadioTorchController machine) {}

    @LuaFunction(mainThread = true)
    public final Object[] setChannel(String channel) {
        machine().setChannelName(channel);
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getChannel() {
        return new Object[]{machine().channel};
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
    public final Object[] send(String command) {
        TileEntityRadioTorchController machine = machine();
        if (machine.channel != null && !machine.channel.isEmpty() && !command.isEmpty()) {
            RTTYSystem.broadcast(machine.getLevel(), machine.channel, command);
        }
        return new Object[]{};
    }
}
//?}
