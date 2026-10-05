package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKCooler;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityRBMKCooler}'s component {@code rbmk_cooler}: 1.7 {@code tanks[0]} is the cold tank. */
public final class RBMKCoolerPeripheral extends RBMKColumnPeripheral<BlockEntityRBMKCooler> {

    private static final int COLD = SLOTS;
    private static final int COLD_MAX = SLOTS + 1;
    private static final int HOT = SLOTS + 2;
    private static final int HOT_MAX = SLOTS + 3;

    public RBMKCoolerPeripheral(BlockEntityRBMKCooler machine) {
        super(machine, "rbmk_cooler", SLOTS + 4, 0);
    }

    @Override
    protected void capture(BlockEntityRBMKCooler machine) {
        super.capture(machine);
        put(COLD, machine.cold.getFill());
        put(COLD_MAX, machine.cold.getMaxFill());
        put(HOT, machine.hot.getFill());
        put(HOT_MAX, machine.hot.getMaxFill());
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.doubleAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getCoolant() {
        return read(s -> new Object[]{s.intAt(COLD), s.intAt(COLD_MAX), s.intAt(HOT), s.intAt(HOT_MAX)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.doubleAt(HEAT), s.intAt(COLD), s.intAt(COLD_MAX), s.intAt(HOT),
                s.intAt(HOT_MAX), s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
//?}
