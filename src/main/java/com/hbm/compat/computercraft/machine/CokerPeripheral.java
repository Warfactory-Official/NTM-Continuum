package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityMachineCoker}'s component {@code ntm_coker}. */
public final class CokerPeripheral extends SnapshotPeripheral<TileEntityMachineCoker> {

    private static final int FILL_0 = 0;
    private static final int FILL_1 = 1;
    private static final int HEAT = 2;
    private static final int TYPE_0 = 0;
    private static final int TYPE_1 = 1;

    public CokerPeripheral(TileEntityMachineCoker machine) {
        super(machine, "ntm_coker", 3, 2);
    }

    @Override
    protected void capture(TileEntityMachineCoker machine) {
        put(FILL_0, machine.tanks[0].getFill());
        put(FILL_1, machine.tanks[1].getFill());
        put(HEAT, machine.heat);
        putRef(TYPE_0, machine.tanks[0].getTankType().getName());
        putRef(TYPE_1, machine.tanks[1].getTankType().getName());
    }

    @LuaFunction
    public final Object[] getTypeStored() {
        return read(s -> new Object[]{s.refAt(TYPE_0), s.refAt(TYPE_1)});
    }

    @LuaFunction
    public final Object[] getFluidStored() {
        return read(s -> new Object[]{s.intAt(FILL_0), s.intAt(FILL_1)});
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.intAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.refAt(TYPE_0), s.refAt(TYPE_1), s.intAt(FILL_0), s.intAt(FILL_1),
                s.intAt(HEAT)});
    }
}
//?}
