package com.hbm.config;

/**
 * Bomb / explosion tunables, ported from 1.12.2's {@code BombConfig}. The full version read these from
 * a Forge config file; here they are plain constants (a cross-loader config layer can be added later).
 * Only the values the ported bombs actually use are present — grow this as more bombs land.
 */
public final class BombConfig {

    private BombConfig() {}

    /** Crater radius of the Fat Man ({@code nuke_man}). */
    public static int manRadius = 175;

    /** Maximum milliseconds per tick allocated to the mk5 chunk-processing (ray/destruction batching). */
    public static int mk5 = 50;

    /** Radius of the fallout area, as a percentage of the base radius. */
    public static int falloutRange = 100;

    /** Maximum milliseconds per tick allocated for fallout chunk processing (EntityFalloutRain). */
    public static int falloutDelay = 30;

    /** Disables the nuclear (crater + fallout) portion of nukes, leaving only the blast damage. */
    public static boolean disableNuclear = false;

    /** Legacy switch that gates whether the nuclear explosion happens at all. */
    public static boolean enableNukeClouds = true;
}
