package com.hbm.compat.computercraft.storage;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The {@code ntm_fluid_tank} component 1.7's barrel and fluid tank each declare with the same four callbacks. */
public abstract class FluidTankPeripheral<BE extends BlockEntity> extends SnapshotPeripheral<BE> {

    private static final int FILL = 0;
    private static final int MAX = 1;
    private static final int TYPE = 0;

    /** The machine's tank, a final field on every machine here; read only in {@link #capture}. */
    private final FluidTankNTM tank;

    protected FluidTankPeripheral(BE machine, FluidTankNTM tank) {
        super(machine, "ntm_fluid_tank", 2, 1);
        this.tank = tank;
    }

    @Override
    protected final void capture(BE machine) {
        put(FILL, tank.getFill());
        put(MAX, tank.getMaxFill());
        putRef(TYPE, tank.getTankType().getName());
    }

    @LuaFunction
    public final Object[] getFluidStored() {
        return read(s -> new Object[]{s.intAt(FILL)});
    }

    @LuaFunction
    public final Object[] getMaxStored() {
        return read(s -> new Object[]{s.intAt(MAX)});
    }

    @LuaFunction
    public final Object[] getTypeStored() {
        return read(s -> new Object[]{s.refAt(TYPE)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.intAt(FILL), s.intAt(MAX), s.refAt(TYPE)});
    }
}
//?}
