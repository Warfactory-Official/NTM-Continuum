package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

/** 1.7 {@code TileEntityCoreEmitter}'s component {@code dfc_emitter}. */
public final class CoreEmitterPeripheral extends SnapshotPeripheral<TileEntityCoreEmitter> {

    private static final int POWER = 0;
    private static final int MAX_POWER = 1;
    private static final int CRYOGEL = 2;
    private static final int WATTS = 3;
    private static final int ON = 4;

    public CoreEmitterPeripheral(TileEntityCoreEmitter machine) {
        super(machine, "dfc_emitter", 5, 0);
    }

    @Override
    protected void capture(TileEntityCoreEmitter machine) {
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(CRYOGEL, machine.tank.getFill());
        put(WATTS, machine.watts);
        put(ON, machine.isOn);
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] getCryogel() {
        return read(s -> new Object[]{s.intAt(CRYOGEL)});
    }

    @LuaFunction
    public final Object[] getInput() {
        return read(s -> new Object[]{s.intAt(WATTS)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.intAt(CRYOGEL), s.intAt(WATTS),
                s.booleanAt(ON)});
    }

    @LuaFunction
    public final Object[] isActive() {
        return read(s -> new Object[]{s.booleanAt(ON)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setActive(boolean active) {
        TileEntityCoreEmitter machine = machine();
        machine.isOn = active;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setInput(double input) {
        TileEntityCoreEmitter machine = machine();
        machine.watts = Mth.clamp((int) input, 0, 100);
        machine.setChanged();
        return new Object[]{};
    }
}
//?}
