package com.hbm.compat.computer.machine;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.tileentity.machine.pile.TileEntityPileLoader;
import dev.golem.core.computer.ComputerMethod;

/** 1.7 {@code TileEntityPileLoader}'s component {@code ntm_pile_loader}; rod types are 1.7's metadata numbers. */
public final class PileLoaderPeripheral extends SnapshotPeripheral<TileEntityPileLoader> {

    private static final int TEMP = 0;
    private static final int DEPLETION = 1;
    private static final int LIFETIME = 2;
    private static final int TYPE = 3;
    private static final int LOADING_TYPE = 4;
    private static final int LOADING = 5;

    public PileLoaderPeripheral(TileEntityPileLoader machine) {
        super(machine, "ntm_pile_loader", 6, 0);
    }

    @Override
    protected void capture(TileEntityPileLoader machine) {
        var stats = ItemPileRodMK2.stats(machine.channelStack);
        put(TEMP, machine.channelTemp);
        put(DEPLETION, machine.channelStack.isEmpty() ? 0D : machine.channelDepletion);
        put(LIFETIME, stats == null ? 0D : stats.life());
        put(TYPE, metadata(machine.channelStack.isEmpty() ? null : ItemPileRodMK2.typeOf(machine.channelStack)));
        put(LOADING_TYPE, metadata(machine.stack.isEmpty() ? null : ItemPileRodMK2.typeOf(machine.stack)));
        put(LOADING, machine.loading);
    }

    private static long metadata(EnumPileRod rod) {
        return rod == null ? -1 : rod.ordinal();
    }

    @ComputerMethod
    public final Object[] getTemp() {
        return read(s -> new Object[]{s.doubleAt(TEMP)});
    }

    @ComputerMethod
    public final Object[] getDepletion() {
        return read(s -> new Object[]{s.doubleAt(DEPLETION)});
    }

    @ComputerMethod
    public final Object[] getLifetime() {
        return read(s -> new Object[]{s.doubleAt(LIFETIME)});
    }

    // IPeripheral owns getType(), so the Lua name is given explicitly
    @ComputerMethod(value = "getType")
    public final Object[] getRodType() {
        return read(s -> new Object[]{s.intAt(TYPE)});
    }

    @ComputerMethod
    public final Object[] getLoadingType() {
        return read(s -> new Object[]{s.intAt(LOADING_TYPE)});
    }

    @ComputerMethod
    public final Object[] isLoading() {
        return read(s -> new Object[]{s.booleanAt(LOADING)});
    }

    @ComputerMethod(mainThread = true)
    public final Object[] load() {
        TileEntityPileLoader machine = machine();
        if (machine.delay <= 0 && machine.travel <= 0) machine.loading = true;
        return new Object[]{};
    }
}
