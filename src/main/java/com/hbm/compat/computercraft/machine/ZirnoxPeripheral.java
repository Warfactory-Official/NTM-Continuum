package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

import java.util.Optional;

/**
 * 1.7 {@code TileEntityReactorZirnox}'s component {@code zirnox_reactor}. ntm-next's {@code getFuel} is not in
 * 1.7's component and is not ported.
 */
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

    @LuaFunction
    public final Object[] getTemp() {
        return read(s -> new Object[]{temp(s.intAt(HEAT))});
    }

    @LuaFunction
    public final Object[] getPressure() {
        return read(s -> new Object[]{pressure(s.intAt(PRESSURE))});
    }

    @LuaFunction
    public final Object[] getWater() {
        return read(s -> new Object[]{s.intAt(WATER)});
    }

    @LuaFunction
    public final Object[] getSteam() {
        return read(s -> new Object[]{s.intAt(STEAM)});
    }

    @LuaFunction
    public final Object[] getCarbonDioxide() {
        return read(s -> new Object[]{s.intAt(CO2)});
    }

    @LuaFunction
    public final Object[] isActive() {
        return read(s -> new Object[]{s.booleanAt(ON)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{temp(s.intAt(HEAT)), pressure(s.intAt(PRESSURE)), s.intAt(WATER),
                s.intAt(STEAM), s.intAt(CO2), s.booleanAt(ON)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setActive(boolean active) {
        TileEntityReactorZirnox machine = machine();
        machine.isOn = active;
        machine.setChanged();
        return new Object[]{};
    }

    // 1.7 optInteger(0, 1000): no argument vents 1000 mB, a given one truncates
    @LuaFunction(mainThread = true)
    public final Object[] ventCarbonDioxide(Optional<Double> amount) {
        TileEntityReactorZirnox machine = machine();
        int requested = amount.isPresent() ? (int) amount.get().doubleValue() : 1000;
        int vent = Mth.clamp(requested, 0, machine.carbonDioxide.getMaxFill());
        machine.carbonDioxide.setFill(Math.max(machine.carbonDioxide.getFill() - vent, 0));
        machine.setChanged();
        return new Object[]{};
    }
}
//?}
