package com.hbm.compat.computercraft.albion;

//? if computercraft {
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityPARFC}'s component {@code ntm_pa_rfc}. */
public final class PARFCPeripheral extends CooledPeripheral<TileEntityPARFC> {

    public PARFCPeripheral(TileEntityPARFC machine) {
        super(machine, "ntm_pa_rfc", SLOTS, 0);
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.intAt(COOLANT), s.intAt(COOLANT_MAX),
                s.intAt(HOT), s.intAt(HOT_MAX)});
    }
}
//?}
