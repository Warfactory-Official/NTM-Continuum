package com.hbm.api.entity;

/**
 * What a radar can see, how it blips and whether it drives the redstone output. Ported from
 * 1.12.2's {@code api.hbm.entity.IRadarDetectableNT}.
 */
public interface IRadarDetectableNT {

    int TIER0 = 0;
    int TIER1 = 1;
    int TIER2 = 2;
    int TIER3 = 3;
    int TIER4 = 4;
    int TIER10 = 5;
    int TIER10_15 = 6;
    int TIER15 = 7;
    int TIER15_20 = 8;
    int TIER20 = 9;
    int TIER_AB = 10;
    int PLAYER = 11;
    int ARTY = 12;
    /** Reserved type that shows a unique purple blip. Used for when nothing else applies. */
    int SPECIAL = 13;

    /** Name used for radar display; looked up as a translation key. */
    String getRadarName();

    /** The type of dot to show on the radar, and the redstone level in tier mode. */
    int getBlipLevel();

    /** Whether the object can be seen by this type of radar. */
    boolean canBeSeenBy(Object radar);

    /** Whether the object is currently visible, and whether the radar's settings pick this up. */
    boolean paramsApplicable(RadarScanParams params);

    /** Whether this radar entry counts toward the redstone output. */
    boolean suppliesRedstone(RadarScanParams params);

    class RadarScanParams {

        public boolean scanMissiles = true;
        public boolean scanShells = true;
        public boolean scanPlayers = true;
        public boolean smartMode = true;

        public RadarScanParams(boolean scanMissiles, boolean scanShells, boolean scanPlayers, boolean smartMode) {
            this.scanMissiles = scanMissiles;
            this.scanShells = scanShells;
            this.scanPlayers = scanPlayers;
            this.smartMode = smartMode;
        }
    }
}
