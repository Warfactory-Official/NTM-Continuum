package com.hbm.compat.computer.fusion;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import dev.golem.core.computer.ComputerMethod;

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

    @ComputerMethod
    public final Object[] getPlasmaEnergy() {
        return read(s -> new Object[]{s.longAt(PLASMA)});
    }

    @ComputerMethod
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.intAt(FILL_0), s.intAt(MAX_0), s.intAt(FILL_1), s.intAt(MAX_1)});
    }

    @ComputerMethod
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(PLASMA), s.intAt(FILL_0), s.intAt(MAX_0), s.intAt(FILL_1),
                s.intAt(MAX_1)});
    }
}
