package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

/**
 * 1.7 {@code TileEntityReactorResearch}'s component {@code research_reactor}. 1.7's {@code level} is
 * {@code rodLevel} here.
 */
public final class ResearchReactorPeripheral extends SnapshotPeripheral<TileEntityReactorResearch> {

    private static final int HEAT = 0;
    private static final int LEVEL = 1;
    private static final int TARGET = 2;
    private static final int FLUX = 3;

    public ResearchReactorPeripheral(TileEntityReactorResearch machine) {
        super(machine, "research_reactor", 4, 0);
    }

    @Override
    protected void capture(TileEntityReactorResearch machine) {
        put(HEAT, machine.heat);
        put(LEVEL, machine.rodLevel);
        put(TARGET, machine.targetLevel);
        put(FLUX, machine.totalFlux);
    }

    @LuaFunction
    public final Object[] getTemp() {
        return read(s -> new Object[]{s.intAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getLevel() {
        return read(s -> new Object[]{s.doubleAt(LEVEL) * 100});
    }

    @LuaFunction
    public final Object[] getTargetLevel() {
        return read(s -> new Object[]{s.doubleAt(TARGET)});
    }

    @LuaFunction
    public final Object[] getFlux() {
        return read(s -> new Object[]{s.intAt(FLUX)});
    }

    // 1.7: the level here is the 0..1 fraction, not getLevel's percent
    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.intAt(HEAT), s.doubleAt(LEVEL), s.doubleAt(TARGET), s.intAt(FLUX)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double level) {
        TileEntityReactorResearch machine = machine();
        machine.targetLevel = Mth.clamp(level / 100.0, 0, 1.0);
        machine.setChanged();
        return new Object[]{};
    }
}
//?}
