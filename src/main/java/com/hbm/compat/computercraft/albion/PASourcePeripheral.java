package com.hbm.compat.computercraft.albion;

//? if computercraft {
import com.hbm.tileentity.machine.albion.TileEntityPASource;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityPASource}'s component {@code ntm_pa_source}. */
public final class PASourcePeripheral extends CooledPeripheral<TileEntityPASource> {

    private static final int SPEED = SLOTS + 4;
    private static final int STATE = 4;

    public PASourcePeripheral(TileEntityPASource machine) {
        super(machine, "ntm_pa_source", SLOTS + 5, 5);
    }

    @Override
    protected void capture(TileEntityPASource machine) {
        super.capture(machine);
        captureCrafting(machine, SLOTS, 0);
        put(SPEED, machine.lastSpeed);
        putRef(STATE, machine.state.name());
    }

    @LuaFunction
    public final Object[] getMomentum() {
        return read(s -> new Object[]{s.intAt(SPEED)});
    }

    @LuaFunction
    public final Object[] getState() {
        return read(s -> new Object[]{s.refAt(STATE)});
    }

    @LuaFunction
    public final Object[] getCrafting() {
        return read(s -> crafting(s, SLOTS, 0));
    }

    @LuaFunction(mainThread = true)
    public final Object[] cancelOperation() {
        TileEntityPASource machine = machine();
        machine.particle = null;
        machine.state = TileEntityPASource.PAState.IDLE;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> {
            Object[] items = crafting(s, SLOTS, 0);
            return new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.intAt(COOLANT), s.intAt(COOLANT_MAX),
                    s.intAt(HOT), s.intAt(HOT_MAX), items[0], items[1], items[2], items[3], items[4], items[5],
                    items[6], items[7], s.intAt(SPEED), s.refAt(STATE)};
        });
    }
}
//?}
