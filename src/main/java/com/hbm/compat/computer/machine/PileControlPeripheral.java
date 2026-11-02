package com.hbm.compat.computer.machine;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.tileentity.machine.pile.TileEntityPileControl;
import dev.golem.core.computer.ComputerMethod;
import net.minecraft.util.Mth;

/** 1.7 {@code TileEntityPileControl}'s component {@code ntm_pile_control}: the rod's extraction, set in percent. */
public final class PileControlPeripheral extends SnapshotPeripheral<TileEntityPileControl> {

    private static final int LEVEL = 0;

    public PileControlPeripheral(TileEntityPileControl machine) {
        super(machine, "ntm_pile_control", 1, 0);
    }

    @Override
    protected void capture(TileEntityPileControl machine) {
        put(LEVEL, machine.extraction);
    }

    @ComputerMethod
    public final Object[] getLevel() {
        return read(s -> new Object[]{s.doubleAt(LEVEL)});
    }

    @ComputerMethod(mainThread = true)
    public final Object[] setLevel(double target) {
        TileEntityPileControl machine = machine();
        machine.setTarget(Mth.clamp(target / 100D, 0D, 1D));
        machine.setChanged();
        return new Object[]{};
    }
}
