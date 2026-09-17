package com.hbm.saveddata.satellites;

import com.hbm.saveddata.SatelliteSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/**
 * One satellite in orbit; 1.7 {@code com.hbm.saveddata.satellites.Satellite}. Instances live in
 * {@link SatelliteSavedData} on the server and are rebuilt from {@link SatelliteType} on the client for the
 * sat interface, so a subclass mutating state owes {@code SavedData.setDirty()} on the server side.
 */
public abstract class Satellite {

    public final Set<InterfaceActions> ifaceAcs;
    public final Set<CoordActions> coordAcs;
    public final Interfaces satIface;

    protected Satellite(Interfaces satIface, Set<InterfaceActions> ifaceAcs, Set<CoordActions> coordAcs) {
        this.satIface = satIface;
        this.ifaceAcs = ifaceAcs;
        this.coordAcs = coordAcs;
    }

    /**
     * 1.7 {@code Satellite.orbit}.
     */
    public static void orbit(ServerLevel level, SatelliteType type, int freq, double x, double y, double z) {
        Satellite sat = type.create();
        SatelliteSavedData data = SatelliteSavedData.get(level);
        data.put(freq, sat);
        sat.onOrbit(level, x, y, z);
        data.setDirty();
    }

    public abstract SatelliteType type();

    public void onOrbit(ServerLevel level, double x, double y, double z) {
    }

    public void onClick(ServerLevel level, int x, int z) {
    }

    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
    }

    public enum InterfaceActions {
        HAS_MAP,
        CAN_CLICK,
        SHOW_COORDS,
        HAS_RADAR,
        HAS_ORES
    }

    public enum CoordActions {
        HAS_Y
    }

    public enum Interfaces {
        NONE,
        SAT_PANEL,
        SAT_COORD
    }
}
