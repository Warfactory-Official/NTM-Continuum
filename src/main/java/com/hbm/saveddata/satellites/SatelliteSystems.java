package com.hbm.saveddata.satellites;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Ages out the two emission logs satellites read from; 1.7 {@code ModEventHandler} world tick (START phase):
 * the detector's bursts every tick, the ray scanner's events once a second on tick 10. Satellites themselves
 * do not tick in 1.7. Registered once for every loader through Core's {@code Lifecycle.onServerTick}.
 */
public final class SatelliteSystems {

    private SatelliteSystems() {}

    public static void tickServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            SatelliteDetector.updateSystem(level);
            if (level.getGameTime() % 20 == 10) SatelliteRayScan.updateSystem(level);
        }
    }
}
