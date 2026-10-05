package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityMicrowave;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

/**
 * 1.7 {@code TileEntityMicrowave}'s component {@code microwave}. 1.7's {@code variableget}/{@code variableset} were an
 * OpenComputers property pair ({@code getter = true}/{@code setter = true}); CC has no properties, so both are
 * methods under the same names.
 */
public final class MicrowavePeripheral extends SnapshotPeripheral<TileEntityMicrowave> {

    private static final int SPEED = 0;

    public MicrowavePeripheral(TileEntityMicrowave machine) {
        super(machine, "microwave", 1, 0);
    }

    @Override
    protected void capture(TileEntityMicrowave machine) {
        put(SPEED, machine.speed);
    }

    @LuaFunction
    public final Object[] test() {
        return new Object[]{"This is a testing device for everything OC."};
    }

    @LuaFunction
    public final Object[] variableget() {
        return read(s -> new Object[]{s.intAt(SPEED), "test of the `getter` callback function"});
    }

    // OpenComputers' checkInteger truncates any number, where a CC int parameter rejects a fraction: take a double
    @LuaFunction(mainThread = true)
    public final Object[] variableset(double speed) {
        TileEntityMicrowave machine = machine();
        machine.speed = Mth.clamp((int) speed, 0, 5);
        machine.setChanged();
        return new Object[]{"test of the `setter` callback function"};
    }
}
//?}
