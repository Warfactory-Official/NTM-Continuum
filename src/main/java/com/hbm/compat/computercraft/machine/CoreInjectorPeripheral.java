package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityCoreInjector}'s component {@code dfc_injector}. */
public final class CoreInjectorPeripheral extends SnapshotPeripheral<TileEntityCoreInjector> {

    private static final int FILL_0 = 0;
    private static final int FILL_1 = 1;
    private static final int TYPE_0 = 0;
    private static final int TYPE_1 = 1;

    public CoreInjectorPeripheral(TileEntityCoreInjector machine) {
        super(machine, "dfc_injector", 2, 2);
    }

    @Override
    protected void capture(TileEntityCoreInjector machine) {
        put(FILL_0, machine.tanks[0].getFill());
        put(FILL_1, machine.tanks[1].getFill());
        putRef(TYPE_0, machine.tanks[0].getTankType().getName());
        putRef(TYPE_1, machine.tanks[1].getTankType().getName());
    }

    @LuaFunction
    public final Object[] getFuel() {
        return read(s -> new Object[]{s.intAt(FILL_0), s.intAt(FILL_1)});
    }

    @LuaFunction
    public final Object[] getTypes() {
        return read(s -> new Object[]{s.refAt(TYPE_0), s.refAt(TYPE_1)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.intAt(FILL_0), s.refAt(TYPE_0), s.intAt(FILL_1), s.refAt(TYPE_1)});
    }
}
//?}
