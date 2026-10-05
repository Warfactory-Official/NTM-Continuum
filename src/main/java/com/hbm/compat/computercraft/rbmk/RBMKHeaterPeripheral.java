package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import dan200.computercraft.api.lua.LuaFunction;

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

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.doubleAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getFill() {
        return read(s -> new Object[]{s.intAt(FEED)});
    }

    @LuaFunction
    public final Object[] getFillMax() {
        return read(s -> new Object[]{s.intAt(FEED_MAX)});
    }

    @LuaFunction
    public final Object[] getExport() {
        return read(s -> new Object[]{s.intAt(EXPORT)});
    }

    @LuaFunction
    public final Object[] getExportMax() {
        return read(s -> new Object[]{s.intAt(EXPORT_MAX)});
    }

    @LuaFunction
    public final Object[] getFillType() {
        return read(s -> new Object[]{s.refAt(FEED_TYPE)});
    }

    @LuaFunction
    public final Object[] getExportType() {
        return read(s -> new Object[]{s.refAt(EXPORT_TYPE)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.doubleAt(HEAT), s.intAt(FEED), s.intAt(FEED_MAX), s.intAt(EXPORT),
                s.intAt(EXPORT_MAX), s.refAt(FEED_TYPE), s.refAt(EXPORT_TYPE), s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
//?}
