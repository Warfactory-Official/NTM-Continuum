package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityWatz;
import dan200.computercraft.api.lua.LuaFunction;

/**
 * 1.7 {@code TileEntityWatz}'s component {@code watz_reactor}. 1.7's {@code methods()} also listed
 * {@code getComponentName}, which its {@code invoke} rejected with {@code NoSuchMethodException}; it is not a method
 * here either.
 */
public final class WatzPeripheral extends SnapshotPeripheral<TileEntityWatz> {

    /** Fill and max fill of tanks 0..2, interleaved. */
    private static final int TANKS = 0;
    private static final int HEAT = 6;
    private static final int FLUX = 7;
    private static final int ON = 8;

    public WatzPeripheral(TileEntityWatz machine) {
        super(machine, "watz_reactor", 9, 0);
    }

    @Override
    protected void capture(TileEntityWatz machine) {
        for (int i = 0; i < 3; i++) {
            put(TANKS + 2 * i, machine.tanks[i].getFill());
            put(TANKS + 2 * i + 1, machine.tanks[i].getMaxFill());
        }
        put(HEAT, machine.heat);
        put(FLUX, machine.getFluxLast());
        put(ON, machine.isOn);
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.intAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getFlux() {
        return read(s -> new Object[]{s.doubleAt(FLUX)});
    }

    @LuaFunction
    public final Object[] getCoolantInfo() {
        return read(s -> new Object[]{s.intAt(TANKS), s.intAt(TANKS + 1), s.intAt(TANKS + 2), s.intAt(TANKS + 3)});
    }

    @LuaFunction
    public final Object[] getWasteInfo() {
        return read(s -> new Object[]{s.intAt(TANKS + 4), s.intAt(TANKS + 5)});
    }

    @LuaFunction
    public final Object[] isOn() {
        return read(s -> new Object[]{s.booleanAt(ON)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.intAt(TANKS), s.intAt(TANKS + 1), s.intAt(TANKS + 2), s.intAt(TANKS + 3),
                s.intAt(TANKS + 4), s.intAt(TANKS + 5), s.intAt(HEAT), s.doubleAt(FLUX), s.booleanAt(ON)});
    }
}
//?}
