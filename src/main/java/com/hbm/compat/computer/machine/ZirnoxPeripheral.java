package com.hbm.compat.computer.machine;

import com.hbm.compat.computer.SnapshotPeripheral;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import dev.golem.core.computer.ComputerMethod;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** 1.7 {@code TileEntityReactorZirnox}'s component {@code zirnox_reactor}. */
public final class ZirnoxPeripheral extends SnapshotPeripheral<TileEntityReactorZirnox> {

    private static final int HEAT = 0;
    private static final int PRESSURE = 1;
    private static final int WATER = 2;
    private static final int STEAM = 3;
    private static final int CO2 = 4;
    private static final int ON = 5;

    public ZirnoxPeripheral(TileEntityReactorZirnox machine) {
        super(machine, "zirnox_reactor", 6, 0);
    }

    @Override
    protected void capture(TileEntityReactorZirnox machine) {
        put(HEAT, machine.heat);
        put(PRESSURE, machine.pressure);
        put(WATER, machine.water.getFill());
        put(STEAM, machine.steam.getFill());
        put(CO2, machine.carbonDioxide.getFill());
        put(ON, machine.isOn);
    }

    private static long temp(int heat) {
        return Math.round(heat * 1.0E-5D * 780.0D + 20.0D);
    }

    private static long pressure(int pressure) {
        return Math.round(pressure * 1.0E-5D * 30.0D);
    }

    @ComputerMethod
    public final Object[] getTemp() {
        return read(s -> new Object[]{temp(s.intAt(HEAT))});
    }

    @ComputerMethod
    public final Object[] getPressure() {
        return read(s -> new Object[]{pressure(s.intAt(PRESSURE))});
    }

    @ComputerMethod
    public final Object[] getWater() {
        return read(s -> new Object[]{s.intAt(WATER)});
    }

    @ComputerMethod
    public final Object[] getSteam() {
        return read(s -> new Object[]{s.intAt(STEAM)});
    }

    @ComputerMethod
    public final Object[] getCarbonDioxide() {
        return read(s -> new Object[]{s.intAt(CO2)});
    }

    // 1.7 checkInteger: a fraction truncates
    @ComputerMethod(mainThread = true)
    public final Object[] getFuel(double index) {
        int slot = (int) index;
        TileEntityReactorZirnox machine = machine();
        ItemStack rod = slot >= 0 && slot < 24 ? machine.fuelRod(slot) : ItemStack.EMPTY;
        if (rod.isEmpty()) return new Object[]{"", 0, 0};
        ItemZirnoxRod item = (ItemZirnoxRod) rod.getItem();
        return new Object[]{item.fuel().legacyName(), item.getLife(rod), item.stats().maxLife()};
    }

    @ComputerMethod
    public final Object[] isActive() {
        return read(s -> new Object[]{s.booleanAt(ON)});
    }

    @ComputerMethod
    public final Object[] getInfo() {
        return read(s -> new Object[]{temp(s.intAt(HEAT)), pressure(s.intAt(PRESSURE)), s.intAt(WATER),
                s.intAt(STEAM), s.intAt(CO2), s.booleanAt(ON)});
    }

    @ComputerMethod(mainThread = true)
    public final Object[] setActive(boolean active) {
        TileEntityReactorZirnox machine = machine();
        machine.isOn = active;
        machine.setChanged();
        return new Object[]{};
    }

    // 1.7 optInteger(0, 1000): no argument vents 1000 mB, a given one truncates
    @ComputerMethod(mainThread = true)
    public final Object[] ventCarbonDioxide(Optional<Double> amount) {
        TileEntityReactorZirnox machine = machine();
        int requested = amount.isPresent() ? (int) amount.get().doubleValue() : 1000;
        int vent = Mth.clamp(requested, 0, machine.carbonDioxide.getMaxFill());
        machine.carbonDioxide.setFill(Math.max(machine.carbonDioxide.getFill() - vent, 0));
        machine.setChanged();
        return new Object[]{};
    }
}
