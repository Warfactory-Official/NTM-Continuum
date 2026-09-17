package com.hbm.saveddata.satellites;

import com.hbm.saveddata.SatelliteSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Ages out the two emission logs satellites read from, and ticks whatever is in orbit. Upstream does this
 * from its world-tick handler; here it hangs off the same server-tick hook the power and radiation engines
 * use, so all the world-scoped systems advance in one place.
 */
public final class SatelliteSystems {

    private SatelliteSystems() {}

    public static void tickServer(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            SatelliteRayScan.updateSystem(level);
            SatelliteDetector.updateSystem(level);

            for (Satellite sat : SatelliteSavedData.get(level).sats.values()) {
                sat.onUpdateTick(level);
            }
        }
    }
}
