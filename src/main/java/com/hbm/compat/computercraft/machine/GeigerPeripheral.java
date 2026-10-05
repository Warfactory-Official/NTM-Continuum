package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityGeiger;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityGeiger}'s component {@code ntm_geiger}. */
public final class GeigerPeripheral extends SnapshotPeripheral<TileEntityGeiger> {

    public GeigerPeripheral(TileEntityGeiger machine) {
        super(machine, "ntm_geiger", 0, 0);
    }

    @Override
    protected void capture(TileEntityGeiger machine) {
    }

    // 1.7: check() samples the chunk radiation at call time, a world query rather than a field, so it runs on the
    // server thread instead of reading a snapshot
    @LuaFunction(mainThread = true)
    public final Object[] getRads() {
        return new Object[]{machine().check()};
    }
}
//?}
