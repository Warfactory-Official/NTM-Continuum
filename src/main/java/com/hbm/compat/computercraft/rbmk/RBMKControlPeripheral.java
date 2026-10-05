package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

/**
 * 1.7 {@code TileEntityRBMKControl}'s component {@code rbmk_control_rod}, as the automatic rods inherit it; the manual
 * rod adds its colour ({@link RBMKControlManualPeripheral}).
 */
public class RBMKControlPeripheral<BE extends BlockEntityRBMKControl> extends RBMKColumnPeripheral<BE> {

    protected static final int LEVEL = SLOTS;
    protected static final int TARGET = SLOTS + 1;
    protected static final int CONTROL_SLOTS = SLOTS + 2;

    public RBMKControlPeripheral(BE machine) {
        this(machine, CONTROL_SLOTS);
    }

    protected RBMKControlPeripheral(BE machine, int longs) {
        super(machine, "rbmk_control_rod", longs, 0);
    }

    @Override
    protected void capture(BE machine) {
        super.capture(machine);
        put(LEVEL, machine.getMult());
        put(TARGET, machine.targetLevel);
    }

    @LuaFunction
    public final Object[] getLevel() {
        return read(s -> new Object[]{s.doubleAt(LEVEL) * 100});
    }

    @LuaFunction
    public final Object[] getTargetLevel() {
        return read(s -> new Object[]{s.doubleAt(TARGET) * 100});
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.doubleAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.doubleAt(HEAT), s.doubleAt(LEVEL) * 100, s.doubleAt(TARGET) * 100,
                s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }

    // 1.7 :226 writes targetLevel directly, not through setTarget, so a manual rod keeps its surge start level
    // (ntm-next calls setTarget). An automatic rod recomputes the target on its next tick, as in 1.7.
    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double level) {
        BE machine = machine();
        machine.targetLevel = Mth.clamp(level / 100.0, 0D, 1D);
        machine.setChanged();
        return new Object[]{};
    }
}
//?}
