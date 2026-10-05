package com.hbm.compat.computercraft.fusion;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityFusionMHDT}'s component {@code ntm_fusion_mhdt}; its energy info is the buffer alone. */
public final class FusionMHDTPeripheral extends SnapshotPeripheral<TileEntityFusionMHDT> {

    private static final int POWER = 0;
    private static final int PLASMA = 1;
    private static final int FILL_0 = 2;
    private static final int MAX_0 = 3;
    private static final int FILL_1 = 4;
    private static final int MAX_1 = 5;

    public FusionMHDTPeripheral(TileEntityFusionMHDT machine) {
        super(machine, "ntm_fusion_mhdt", 6, 0);
    }

    @Override
    protected void capture(TileEntityFusionMHDT machine) {
        put(POWER, machine.power);
        put(PLASMA, machine.plasmaEnergySync());
        put(FILL_0, machine.tanks[0].getFill());
        put(MAX_0, machine.tanks[0].getMaxFill());
        put(FILL_1, machine.tanks[1].getFill());
        put(MAX_1, machine.tanks[1].getMaxFill());
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER)});
    }

    @LuaFunction
    public final Object[] getPlasmaEnergy() {
        return read(s -> new Object[]{s.longAt(PLASMA)});
    }

    @LuaFunction
    public final Object[] getCoolant() {
        return read(s -> new Object[]{s.intAt(FILL_0), s.intAt(MAX_0), s.intAt(FILL_1), s.intAt(MAX_1)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(PLASMA), s.intAt(FILL_0), s.intAt(MAX_0),
                s.intAt(FILL_1), s.intAt(MAX_1)});
    }
}
//?}
