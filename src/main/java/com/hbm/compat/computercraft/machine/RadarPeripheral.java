package com.hbm.compat.computercraft.machine;

//? if computercraft {
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.RadarEntry;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import dan200.computercraft.api.lua.LuaFunction;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code TileEntityMachineRadarNT}'s component {@code ntm_radar}, inherited by the large radar. The entry
 * methods read the live entry list, so they run on the server thread.
 */
public final class RadarPeripheral extends SnapshotPeripheral<TileEntityMachineRadarNT> {

    private static final int MISSILES = 0;
    private static final int SHELLS = 1;
    private static final int PLAYERS = 2;
    private static final int SMART = 3;
    private static final int RANGE = 4;
    private static final int POWER = 5;
    private static final int MAX_POWER = 6;
    private static final int JAMMED = 7;
    private static final int AMOUNT = 8;
    private static final int X = 9;
    private static final int Y = 10;
    private static final int Z = 11;

    public RadarPeripheral(TileEntityMachineRadarNT machine) {
        super(machine, "ntm_radar", 12, 0);
    }

    @Override
    protected void capture(TileEntityMachineRadarNT machine) {
        put(MISSILES, machine.scanMissiles);
        put(SHELLS, machine.scanShells);
        put(PLAYERS, machine.scanPlayers);
        put(SMART, machine.smartMode);
        put(RANGE, machine.getRange());
        put(POWER, machine.getPower());
        put(MAX_POWER, machine.getMaxPower());
        put(JAMMED, machine.jammed);
        put(AMOUNT, machine.entries.size());
        put(X, machine.getBlockPos().getX());
        put(Y, machine.getBlockPos().getY());
        put(Z, machine.getBlockPos().getZ());
    }

    @LuaFunction
    public final Object[] getSettings() {
        return read(s -> new Object[]{s.booleanAt(MISSILES), s.booleanAt(SHELLS), s.booleanAt(PLAYERS),
                s.booleanAt(SMART)});
    }

    @LuaFunction
    public final Object[] getRange() {
        return read(s -> new Object[]{s.intAt(RANGE)});
    }

    @LuaFunction(mainThread = true)
    public final Object[] setSettings(boolean missiles, boolean shells, boolean players, boolean smart) {
        TileEntityMachineRadarNT machine = machine();
        machine.scanMissiles = missiles;
        machine.scanShells = shells;
        machine.scanPlayers = players;
        machine.smartMode = smart;
        machine.setChanged();
        return new Object[]{};
    }

    @LuaFunction
    public final Object[] getEnergyInfo() {
        return read(s -> new Object[]{s.longAt(POWER), s.longAt(MAX_POWER)});
    }

    @LuaFunction
    public final Object[] isJammed() {
        return read(s -> new Object[]{s.booleanAt(JAMMED)});
    }

    @LuaFunction
    public final Object[] getAmount() {
        return read(s -> new Object[]{s.intAt(AMOUNT)});
    }

    private static Object[] noEntity() {
        return new Object[]{null, "No entity exists at that index."};
    }

    // 1.7 refused only index > size, so index == size threw inside ArrayList.get; as ntm-next, that case answers
    // the bound's own message instead of the exception's
    private @Nullable RadarEntry entry(double luaIndex) {
        int index = (int) luaIndex - 1;
        List<RadarEntry> entries = machine().entries;
        return index < 0 || index >= entries.size() ? null : entries.get(index);
    }

    @LuaFunction(mainThread = true)
    public final Object[] isIndexPlayer(double index) {
        RadarEntry e = entry(index);
        return e == null ? noEntity() : new Object[]{e.blipLevel == IRadarDetectableNT.PLAYER};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getIndexType(double index) {
        RadarEntry e = entry(index);
        return e == null ? noEntity() : new Object[]{e.blipLevel};
    }

    @LuaFunction(mainThread = true)
    public final Object[] getEntityAtIndex(double index) {
        RadarEntry e = entry(index);
        if (e == null) return noEntity();
        if (e.blipLevel == IRadarDetectableNT.PLAYER) {
            return new Object[]{true, e.posX, e.posY, e.posZ, e.blipLevel, e.unlocalizedName};
        }
        return new Object[]{false, e.posX, e.posY, e.posZ, e.blipLevel};
    }

    @LuaFunction
    public final Object[] getPos() {
        return read(s -> new Object[]{s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }
}
//?}
