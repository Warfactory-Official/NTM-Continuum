package com.hbm.compat.computer.rbmk;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import dev.golem.core.computer.ComputerMethod;

/** 1.7 {@code TileEntityRBMKHeater}'s component {@code rbmk_heater}; fluid types are 1.7's internal names. */
public final class RBMKHeaterPeripheral extends RBMKColumnPeripheral<BlockEntityRBMKHeater> {

    private static final int FEED = SLOTS;
    private static final int FEED_MAX = SLOTS + 1;
    private static final int EXPORT = SLOTS + 2;
    private static final int EXPORT_MAX = SLOTS + 3;
    private static final int FEED_TYPE = 0;
    private static final int EXPORT_TYPE = 1;

    public RBMKHeaterPeripheral(BlockEntityRBMKHeater machine) {
        super(machine, "rbmk_heater", SLOTS + 4, 2);
    }

    @Override
    protected void capture(BlockEntityRBMKHeater machine) {
        super.capture(machine);
        put(FEED, machine.feed.getFill());
        put(FEED_MAX, machine.feed.getMaxFill());
        put(EXPORT, machine.steam.getFill());
        put(EXPORT_MAX, machine.steam.getMaxFill());
        putRef(FEED_TYPE, machine.feed.getTankType().getName());
        putRef(EXPORT_TYPE, machine.steam.getTankType().getName());
    }

    @ComputerMethod
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.doubleAt(HEAT)});
    }

    @ComputerMethod
    public final Object[] getFill() {
        return read(s -> new Object[]{s.intAt(FEED)});
    }

    @ComputerMethod
    public final Object[] getFillMax() {
        return read(s -> new Object[]{s.intAt(FEED_MAX)});
    }

    @ComputerMethod
    public final Object[] getExport() {
        return read(s -> new Object[]{s.intAt(EXPORT)});
    }

    @ComputerMethod
    public final Object[] getExportMax() {
        return read(s -> new Object[]{s.intAt(EXPORT_MAX)});
    }

    @ComputerMethod
    public final Object[] getFillType() {
        return read(s -> new Object[]{s.refAt(FEED_TYPE)});
    }

    @ComputerMethod
    public final Object[] getExportType() {
        return read(s -> new Object[]{s.refAt(EXPORT_TYPE)});
    }

    @ComputerMethod
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.doubleAt(HEAT), s.intAt(FEED), s.intAt(FEED_MAX), s.intAt(EXPORT),
                s.intAt(EXPORT_MAX), s.refAt(FEED_TYPE), s.refAt(EXPORT_TYPE), s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
