package com.hbm.saveddata.satellites;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1.7 {@code World.getHeightValue(x, z)}: the y above the column's highest light-blocking block. The nearest
 * modern height map is {@code MOTION_BLOCKING}, which this port uses for the same call everywhere else.
 */
final class Heights {

    private Heights() {}

    static int heightValue(Level level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }
}
