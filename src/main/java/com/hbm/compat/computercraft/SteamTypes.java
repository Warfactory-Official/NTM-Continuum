package com.hbm.compat.computercraft;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code CompatHandler.steamTypeToInt}/{@code intToSteamType}: steam by compression level, as the turbine and
 * boiler components report and accept it.
 */
public final class SteamTypes {

    private SteamTypes() {}

    public static int toInt(@Nullable FluidType type) {
        if (type == Fluids.HOTSTEAM) return 1;
        if (type == Fluids.SUPERHOTSTEAM) return 2;
        if (type == Fluids.ULTRAHOTSTEAM) return 3;
        return 0;
    }

    public static FluidType fromInt(int level) {
        switch (level) {
            case 1: return Fluids.HOTSTEAM;
            case 2: return Fluids.SUPERHOTSTEAM;
            case 3: return Fluids.ULTRAHOTSTEAM;
            default: return Fluids.STEAM;
        }
    }
}
