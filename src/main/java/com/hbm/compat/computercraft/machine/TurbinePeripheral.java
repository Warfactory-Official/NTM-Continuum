package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The {@code ntm_turbine} readout the four 1.7 steam turbines share: input tank, output tank, steam type, power.
 * {@code getType} is a Java {@code steamType()} because {@code IPeripheral.getType()} is the peripheral type.
 */
public abstract class TurbinePeripheral<BE extends BlockEntity> extends SnapshotPeripheral<BE> {

    protected static final int IN_FILL = 0;
    protected static final int IN_MAX = 1;
    protected static final int OUT_FILL = 2;
    protected static final int OUT_MAX = 3;
    protected static final int STEAM = 4;
    protected static final int POWER = 5;
    protected static final int SLOTS = 6;

    protected TurbinePeripheral(BE machine, int longs) {
        super(machine, "ntm_turbine", longs, 0);
    }

    protected abstract FluidTankNTM input(BE machine);

    protected abstract FluidTankNTM output(BE machine);

    protected abstract long power(BE machine);

    @Override
    protected void capture(BE machine) {
        put(IN_FILL, input(machine).getFill());
        put(IN_MAX, input(machine).getMaxFill());
        put(OUT_FILL, output(machine).getFill());
        put(OUT_MAX, output(machine).getMaxFill());
        put(STEAM, SteamTypes.toInt(input(machine).getTankType()));
        put(POWER, power(machine));
    }

    @LuaFunction
    public final Object[] getFluid() {
        return read(s -> new Object[]{s.intAt(IN_FILL), s.intAt(IN_MAX), s.intAt(OUT_FILL), s.intAt(OUT_MAX)});
    }

    @LuaFunction("getType")
    public final Object[] steamType() {
        return read(s -> new Object[]{s.intAt(STEAM)});
    }

    @LuaFunction
    public final Object[] getPower() {
        return read(s -> new Object[]{s.longAt(POWER)});
    }

    /** 1.7 {@code getInfo} of the three turbines without a flywheel. */
    protected final Object[] info() {
        return read(s -> new Object[]{s.intAt(IN_FILL), s.intAt(IN_MAX), s.intAt(OUT_FILL), s.intAt(OUT_MAX),
                s.intAt(STEAM), s.longAt(POWER)});
    }
}
//?}
