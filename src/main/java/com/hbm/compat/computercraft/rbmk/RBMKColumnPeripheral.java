package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import dan200.computercraft.api.lua.LuaFunction;

/**
 * The column heat and position every 1.7 RBMK column component reports ({@code getCoordinates}); each subclass
 * copies its own readout after these four slots.
 */
public abstract class RBMKColumnPeripheral<BE extends BlockEntityRBMKBase> extends SnapshotPeripheral<BE> {

    protected static final int HEAT = 0;
    protected static final int X = 1;
    protected static final int Y = 2;
    protected static final int Z = 3;
    protected static final int SLOTS = 4;

    protected RBMKColumnPeripheral(BE machine, String type, int longs, int refs) {
        super(machine, type, longs, refs);
    }

    @Override
    protected void capture(BE machine) {
        put(HEAT, machine.heat);
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
    }

    @LuaFunction
    public final Object[] getCoordinates() {
        return read(s -> new Object[]{s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
//?}
