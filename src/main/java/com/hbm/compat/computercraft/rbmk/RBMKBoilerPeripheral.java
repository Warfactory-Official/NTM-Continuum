package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.SteamTypes;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import dan200.computercraft.api.lua.LuaFunction;

/** 1.7 {@code TileEntityRBMKBoiler}'s component {@code rbmk_boiler}; steam types are compression levels 0..3. */
public final class RBMKBoilerPeripheral extends RBMKColumnPeripheral<BlockEntityRBMKBoiler> {

    private static final int STEAM = SLOTS;
    private static final int STEAM_MAX = SLOTS + 1;
    private static final int FEED = SLOTS + 2;
    private static final int FEED_MAX = SLOTS + 3;
    private static final int STEAM_TYPE = SLOTS + 4;

    public RBMKBoilerPeripheral(BlockEntityRBMKBoiler machine) {
        super(machine, "rbmk_boiler", SLOTS + 5, 0);
    }

    @Override
    protected void capture(BlockEntityRBMKBoiler machine) {
        super.capture(machine);
        put(STEAM, machine.steam.getFill());
        put(STEAM_MAX, machine.steam.getMaxFill());
        put(FEED, machine.feed.getFill());
        put(FEED_MAX, machine.feed.getMaxFill());
        put(STEAM_TYPE, SteamTypes.toInt(machine.steam.getTankType()));
    }

    @LuaFunction
    public final Object[] getHeat() {
        return read(s -> new Object[]{s.doubleAt(HEAT)});
    }

    @LuaFunction
    public final Object[] getSteam() {
        return read(s -> new Object[]{s.intAt(STEAM)});
    }

    @LuaFunction
    public final Object[] getSteamMax() {
        return read(s -> new Object[]{s.intAt(STEAM_MAX)});
    }

    @LuaFunction
    public final Object[] getWater() {
        return read(s -> new Object[]{s.intAt(FEED)});
    }

    @LuaFunction
    public final Object[] getWaterMax() {
        return read(s -> new Object[]{s.intAt(FEED_MAX)});
    }

    @LuaFunction
    public final Object[] getInfo() {
        return read(s -> new Object[]{s.doubleAt(HEAT), s.intAt(STEAM), s.intAt(STEAM_MAX), s.intAt(FEED),
                s.intAt(FEED_MAX), s.intAt(STEAM_TYPE), s.intAt(X), s.intAt(Y), s.intAt(Z)});
    }

    @LuaFunction
    public final Object[] getSteamType() {
        return read(s -> new Object[]{s.intAt(STEAM_TYPE)});
    }

    // 1.7 only retypes the tank; unlike the compressor button it does not rescale the steam already held
    @LuaFunction(mainThread = true)
    public final Object[] setSteamType(double type) {
        BlockEntityRBMKBoiler machine = machine();
        machine.steam.setTankType(SteamTypes.fromInt((int) type));
        machine.setChanged();
        return new Object[]{true};
    }
}
//?}
