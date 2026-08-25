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

    /** Crater radius of a nuclear missile warhead. The MIRV and doomsday warheads double it. */
    public static int missileRadius = 100;

    /** Crater radius of the N2 mine warhead; the missile uses {@code n2Radius / 12 * 5} of it. */
    public static int n2Radius = 200;

    /** Maximum milliseconds per tick allocated to the mk5 chunk-processing (ray/destruction batching). */
    public static int mk5 = 50;

    /** Radius of the fallout area, as a percentage of the base radius. */
    public static int falloutRange = 100;

    /** Maximum milliseconds per tick allocated for fallout chunk processing (EntityFalloutRain). */
    public static int falloutDelay = 30;

    /** Disables the nuclear (crater + fallout) portion of nukes, leaving only the blast damage. */
    public static boolean disableNuclear = false;

    /**
     * Clears the crater by writing whole chunk sections and repairing heightmaps/lighting once per
     * chunk, instead of calling {@code Level.setBlock} for every one of a Fat Man's millions of blocks.
     *
     * <p>Worth roughly 2-6x on the destruction phase plus the lighting backlog the per-block path
     * leaves behind. Set to false to fall back to the per-block loop -- the escape hatch for a lighting
     * or heightmap problem in the wild, since a broken one leaves a black world and nobody can patch a
     * jar mid-game.
     */
    public static boolean craterSectionCarve = true;

    /**
     * Whether the fallout sweep notifies each converted block's neighbours ({@code updateNeighborsAt}).
     *
     * <p>1.12.2 did this — {@code notifyNeighborsOfStateChange} — so it is on by default and turning it
     * off is a deliberate divergence: redstone, fluids and falling blocks next to a converted cell would
     * not react until something else touched them.
     */
    public static boolean falloutNeighbourUpdates = true;

    /**
     * Whether the fallout sweep runs the <b>shape</b> pass on each converted block's neighbours
     * ({@code updateNeighbourShapes}).
     *
     * <p><b>This is the pass that pops unsupported blocks.</b> A flower, torch, ladder, sign, snow layer
     * or redstone dust sitting on a cell the sweep converts returns AIR from its {@code updateShape},
     * and without this pass it stays floating in mid-air.
     *
     * <p>It was briefly defaulted off, on the theory that it was what made the sweep slow, and on the
     * argument that 1.12.2 has no shape pass so leaving it out was faithful. <b>Both halves of that were
     * wrong.</b> Measured on a real blast, the notify pass was 48,373 ms of `updateNeighborsAt` against
     * <b>23 ms</b> here — it was never the cost. And 1.12.2 popping unsupported blocks from inside each
     * block's own {@code neighborChanged} means upstream <em>did</em> pop them; modern vanilla just moved
     * that logic into {@code updateShape}. Skipping it is not faithful to upstream's behaviour, only to
     * its call graph.
     *
     * <p>Kept as a switch because it is a real cost on a surface full of things — 3–4 µs a cell against
     * 0.3 µs underground ({@code nuke_fallout_notify_cost}) — and someone converting a whole continent
     * may want the trade. It is on by default because floating torches are a bug.
     */
    public static boolean falloutShapeUpdates = true;

    /** Legacy switch that gates whether the nuclear explosion happens at all. */
    public static boolean enableNukeClouds = true;
}
