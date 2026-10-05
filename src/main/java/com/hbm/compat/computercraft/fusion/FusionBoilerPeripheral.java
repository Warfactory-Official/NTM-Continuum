package com.hbm.compat.computercraft.fusion;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityFusionBoiler}'s component {@code ntm_fusion_boiler}. */
public final class FusionBoilerPeripheral extends SnapshotPeripheral<TileEntityFusionBoiler> {

    private static final int PLASMA = 0;
    private static final int FILL_0 = 1;
    private static final int MAX_0 = 2;
    private static final int FILL_1 = 3;
    private static final int MAX_1 = 4;

    public FusionBoilerPeripheral(TileEntityFusionBoiler machine) {
        super(machine, "ntm_fusion_boiler", 5, 0);
    }

    @Override
    protected void capture(TileEntityFusionBoiler machine) {
        put(PLASMA, machine.plasmaEnergySync());
        put(FILL_0, machine.tanks[0].getFill());
        put(MAX_0, machine.tanks[0].getMaxFill());
        put(FILL_1, machine.tanks[1].getFill());
        put(MAX_1, machine.tanks[1].getMaxFill());
    }

    @LuaFunction
    public final Object[] getPlasmaEnergy() {
        return read(s -> new Object[]{s.longAt(PLASMA)});
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.intAt(FILL_0), s.intAt(MAX_0), s.intAt(FILL_1), s.intAt(MAX_1)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(PLASMA), s.intAt(FILL_0), s.intAt(MAX_0), s.intAt(FILL_1),
                s.intAt(MAX_1)});
    }
}
//?}
