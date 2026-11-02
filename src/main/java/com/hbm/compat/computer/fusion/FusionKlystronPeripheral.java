package com.hbm.compat.computer.fusion;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.tileentity.machine.fusion.TileEntityFusionKlystron;
import dev.golem.core.computer.ComputerMethod;
import net.minecraft.util.Mth;

/** 1.7 {@code TileEntityFusionKlystron}'s component {@code ntm_fusion_klystron}. */
public final class FusionKlystronPeripheral extends SnapshotPeripheral<TileEntityFusionKlystron> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int AIR = 2;
    private static final int AIR_MAX = 3;
    private static final int OUTPUT = 4;
    private static final int TARGET = 5;

    public FusionKlystronPeripheral(TileEntityFusionKlystron machine) {
        super(machine, "ntm_fusion_klystron", 6, 0);
    }

    @Override
    protected void capture(TileEntityFusionKlystron machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(AIR, machine.compair.getFill());
        put(AIR_MAX, machine.compair.getMaxFill());
        put(OUTPUT, machine.output);
        put(TARGET, machine.outputTarget);
    }

    @ComputerMethod
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @ComputerMethod
    public final Object[] getAir() {
        return read(s -> new Object[]{s.intAt(AIR), s.intAt(AIR_MAX)});
    }

    @ComputerMethod
    public final Object[] getOutput() {
        return read(s -> new Object[]{s.longAt(OUTPUT), s.longAt(TARGET)});
    }

    @ComputerMethod(mainThread = true)
    public final Object[] setOutput(double output) {
        TileEntityFusionKlystron machine = machine();
        machine.outputTarget = (long) Mth.clamp(output, 0.0D, (double) TileEntityFusionKlystron.MAX_OUTPUT);
        machine.setChanged();
        return new Object[]{};
    }

    @ComputerMethod
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.intAt(AIR), s.intAt(AIR_MAX),
                s.longAt(OUTPUT), s.longAt(TARGET)});
    }
}
