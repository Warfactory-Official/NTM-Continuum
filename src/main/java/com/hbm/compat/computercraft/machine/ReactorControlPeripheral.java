package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityReactorControl;
import com.hbm.tileentity.machine.TileEntityReactorControl.RodFunction;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * 1.7 {@code TileEntityReactorControl}'s component {@code reactor_control}. 1.7's {@code getReactor} returned
 * {@code getDisplayData()}'s {@code int[]}, which OpenComputers handed Lua as a table; CC converts a list.
 */
public final class ReactorControlPeripheral extends SnapshotPeripheral<TileEntityReactorControl> {

    private static final int LINKED = 0;
    private static final int FUNCTION = 1;
    private static final int HEAT_UPPER = 2;
    private static final int HEAT_LOWER = 3;
    private static final int LEVEL_UPPER = 4;
    private static final int LEVEL_LOWER = 5;
    private static final int DISPLAY = 0;

    public ReactorControlPeripheral(TileEntityReactorControl machine) {
        super(machine, "reactor_control", 6, 1);
    }

    @Override
    protected void capture(TileEntityReactorControl machine) {
        put(LINKED, machine.isLinked);
        put(FUNCTION, machine.function.ordinal());
        put(HEAT_UPPER, machine.heatUpper);
        put(HEAT_LOWER, machine.heatLower);
        put(LEVEL_UPPER, machine.levelUpper);
        put(LEVEL_LOWER, machine.levelLower);
        int[] data = machine.getDisplayData();
        putRef(DISPLAY, List.of(data[0], data[1], data[2]));
    }

    @LuaFunction
    public final Object[] isLinked() {
        return read(s -> new Object[]{s.booleanAt(LINKED)});
    }

    @LuaFunction
    public final Object[] getReactor() {
        return read(s -> new Object[]{s.refAt(DISPLAY)});
    }

    // 1.7 checkInteger truncates the function number; the other four are checkDouble
    @LuaFunction(mainThread = true)
    public final Object[] setParams(double function, double maxHeat, double minHeat, double maxLevel,
                                    double minLevel) {
        TileEntityReactorControl machine = machine();
        machine.function = RodFunction.values()[Mth.clamp((int) function, 0, 2)];
        machine.heatUpper = Mth.clamp(maxHeat, 0, 9999);
        machine.heatLower = Mth.clamp(minHeat, 0, 9999);
        machine.levelUpper = Mth.clamp(maxLevel / 100.0, 0, 1);
        machine.levelLower = Mth.clamp(minLevel / 100.0, 0, 1);
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction
    public final Object[] getParams() {
        return read(s -> new Object[]{s.intAt(FUNCTION), s.doubleAt(HEAT_UPPER), s.doubleAt(HEAT_LOWER),
                s.doubleAt(LEVEL_UPPER), s.doubleAt(LEVEL_LOWER)});
    }
}
//?}
