package com.hbm.compat.computer.machine;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import dev.golem.core.computer.ComputerMethod;

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

    @ComputerMethod
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(JOULES), s.longAt(POWER)});
    }

    @ComputerMethod
    public final Object[] getCryogel() {
        return read(s -> new Object[]{s.intAt(CRYOGEL)});
    }

    @ComputerMethod
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(JOULES), s.longAt(POWER), s.intAt(CRYOGEL)});
    }
}
