package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityCoreReceiver}'s component {@code dfc_receiver}. */
public final class CoreReceiverPeripheral extends SnapshotPeripheral<TileEntityCoreReceiver> {

    private static final int JOULES = 0;
    private static final int POWER = 1;
    private static final int CRYOGEL = 2;

    public CoreReceiverPeripheral(TileEntityCoreReceiver machine) {
        super(machine, "dfc_receiver", 3, 0);
    }

    @Override
    protected void capture(TileEntityCoreReceiver machine) {
        put(JOULES, machine.joules);
        put(POWER, machine.getPower());
        put(CRYOGEL, machine.tank.getFill());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(JOULES), s.longAt(POWER)});
    }

    @LuaFunction
    public final Object[] getCryogel() {
        return read(s -> new Object[]{s.intAt(CRYOGEL)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(JOULES), s.longAt(POWER), s.intAt(CRYOGEL)});
    }
}
//?}
