package com.hbm.compat.computercraft.network;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.network.TileEntityRadioTorchBase;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

/**
 * 1.7 {@code TileEntityRadioTorchBase}'s component {@code radio_torch}, on the sender and the receiver. 1.7 has the
 * four setters only (ntm-next's getters are not in 1.7). Writes go through the torch's own {@code receiveControl},
 * the path its screen uses, so the port's limits (channel 10, mapping 32 characters) and the radio resync apply.
 */
public final class RadioTorchPeripheral extends SnapshotPeripheral<TileEntityRadioTorchBase> {

    public RadioTorchPeripheral(TileEntityRadioTorchBase machine) {
        super(machine, "radio_torch", 0, 0);
    }

    @Override
    protected void capture(TileEntityRadioTorchBase machine) {
    }

    @LuaFunction(mainThread = true)
    public final Object[] setChannel(String channel) {
        CompoundTag data = new CompoundTag();
        data.putString("channel", channel);
        machine().receiveControl(data);
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setPolling(boolean polling) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("isPolling", polling);
        machine().receiveControl(data);
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setCustomMap(boolean customMap) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("hasMapping", customMap);
        machine().receiveControl(data);
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setCustomMapValues(Map<?, ?> values) {
        CompoundTag data = new CompoundTag();
        for (int i = 1; i <= TileEntityRadioTorchBase.MAPPING_SIZE; i++) {
            // 1.7 looks the redstone value up as an Integer key; CC hands Lua numbers over as Doubles
            Object value = values.containsKey(i) ? values.get(i) : values.get((double) i);
            if (value instanceof String) data.putString("mapping" + (i - 1), (String) value);
        }
        machine().receiveControl(data);
        return new Object[]{};
    }
}
//?}
