package com.hbm.compat.computercraft.albion;

//? if computercraft {
import com.hbm.tileentity.machine.albion.TileEntityPADipole;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.util.Mth;

/**
 * 1.7 {@code TileEntityPADipole}'s component {@code ntm_pa_dipole}. Directions travel as 1.7's names (0 north,
 * 1 east, 2 south, 3 west); an unknown name leaves the direction unchanged.
 */
public final class PADipolePeripheral extends CooledPeripheral<TileEntityPADipole> {

    private static final int THRESHOLD = SLOTS;
    private static final int LOWER = 0;
    private static final int UPPER = 1;
    private static final int REDSTONE = 2;

    public PADipolePeripheral(TileEntityPADipole machine) {
        super(machine, "ntm_pa_dipole", SLOTS + 1, 3);
    }

    static String dirToName(int dir) {
        if (dir == 1) return "east";
        if (dir == 2) return "south";
        if (dir == 3) return "west";
        return "north";
    }

    static int nameToDir(String name) {
        if (name.equals("north")) return 0;
        if (name.equals("east")) return 1;
        if (name.equals("south")) return 2;
        if (name.equals("west")) return 3;
        return -1;
    }

    @Override
    protected void capture(TileEntityPADipole machine) {
        super.capture(machine);
        put(THRESHOLD, machine.threshold);
        putRef(LOWER, dirToName(machine.dirLower));
        putRef(UPPER, dirToName(machine.dirUpper));
        putRef(REDSTONE, dirToName(machine.dirRedstone));
    }

    @LuaFunction
    public final Object[] getDirLower() {
        return read(s -> new Object[]{s.refAt(LOWER)});
    }

    @LuaFunction
    public final Object[] getDirUpper() {
        return read(s -> new Object[]{s.refAt(UPPER)});
    }

    @LuaFunction
    public final Object[] getDirRedstone() {
        return read(s -> new Object[]{s.refAt(REDSTONE)});
    }

    @LuaFunction
    public final Object[] getThreshold() {
        return read(s -> new Object[]{s.intAt(THRESHOLD)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDirLower(String name) {
        int dir = nameToDir(name);
        TileEntityPADipole machine = machine();
        if (dir >= 0) machine.dirLower = dir;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDirUpper(String name) {
        int dir = nameToDir(name);
        TileEntityPADipole machine = machine();
        if (dir >= 0) machine.dirUpper = dir;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setDirRedstone(String name) {
        int dir = nameToDir(name);
        TileEntityPADipole machine = machine();
        if (dir >= 0) machine.dirRedstone = dir;
        machine.setChanged();
        return new Object[]{};
    }

    // OpenComputers' checkInteger truncates any number
    @LuaFunction(mainThread = true)
    public final Object[] setThreshold(double threshold) {
        TileEntityPADipole machine = machine();
        machine.threshold = Mth.clamp((int) threshold, 0, 999_999_999);
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER), s.intAt(COOLANT), s.intAt(COOLANT_MAX),
                s.intAt(HOT), s.intAt(HOT_MAX), s.refAt(LOWER), s.refAt(UPPER), s.refAt(REDSTONE),
                s.intAt(THRESHOLD)});
    }
}
//?}
