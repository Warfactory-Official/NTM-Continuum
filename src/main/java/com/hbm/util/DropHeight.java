package com.hbm.util;

import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1.7's build ceiling was 256, so every absolute spawn altitude it authored meant "above the world"; the
 * number was incidental. A dimension taller than that (height-extending worldgen reaches y~500) puts the
 * literal INSIDE terrain, so the drop is lifted clear of the column instead.
 *
 * <p>The 1.7 value is the floor, which keeps every dimension 1.7 could represent bit-identical.
 */
public final class DropHeight {

    private static final int CLEARANCE = 32;

    private DropHeight() {
    }

    public static double clearOfTerrain(LevelReader level, double legacy, double x, double z) {
        return Math.max(legacy, level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(x), (int) Math.floor(z)) + CLEARANCE);
    }
}
