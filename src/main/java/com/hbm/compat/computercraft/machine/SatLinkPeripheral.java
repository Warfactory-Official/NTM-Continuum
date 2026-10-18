package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityMachineSatLink}'s component {@code ntm_satlink}, the ground station. */
public final class SatLinkPeripheral extends SnapshotPeripheral<TileEntityMachineSatLink> {

    private static final int CONNECTED = 0;
    private static final int FREQ = 1;

    public SatLinkPeripheral(TileEntityMachineSatLink machine) {
        super(machine, "ntm_satlink", 2, 0);
    }

    @Override
    protected void capture(TileEntityMachineSatLink machine) {
        put(CONNECTED, machine.connected);
        put(FREQ, machine.freq);
    }

    @LuaFunction
    public final Object[] isConnected() {
        return read(s -> new Object[]{s.booleanAt(CONNECTED)});
    }

    // OpenComputers' checkInteger truncates any number and 1.7 sets it unclamped
    @LuaFunction(mainThread = true)
    public final Object[] setFreq(double freq) {
        TileEntityMachineSatLink link = machine();
        link.freq = (int) freq;
        return new Object[]{};
    }

    @LuaFunction
    public final Object[] getFreq() {
        return read(s -> new Object[]{s.intAt(FREQ)});
    }

    // IPeripheral.getType names the peripheral; the Lua method keeps 1.7's name
    @LuaFunction(value = "getType", mainThread = true)
    public final Object[] satelliteType() {
        return new Object[]{machine().provideRORValue(IRORInfo.PREFIX_VALUE + "type")};
    }

    @LuaFunction(mainThread = true)
    public final Object[] send(String command) {
        machine().runRORFunction(IRORInfo.PREFIX_FUNCTION + "tx", new String[]{command});
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] read() {
        return new Object[]{machine().provideRORValue(IRORInfo.PREFIX_VALUE + "rx")};
    }
}
//?}
