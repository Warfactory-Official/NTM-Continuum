package com.hbm.config;

/**
 * 1.7 {@code BombConfig}, read from {@code hbm.cfg} by {@link #loadFromConfig}. The fields after {@code falloutRange}
 * ({@code falloutClaimWindow}, {@code disableNuclear}, the crater/fallout update switches, {@code enableNukeClouds}) are
 * port-only constants with no {@code hbm.cfg} key.
 */
public final class BombConfig {

    private BombConfig() {}

    /** Crater radius of the Fat Man ({@code nuke_man}). */
    public static int manRadius = 175;

    /** Crater radius of a fully assembled Ivy Mike. Its under-filled test configuration uses manRadius. */
    public static int mikeRadius = 250;

    /** Crater radius of the Gadget. */
    public static int gadgetRadius = 150;

    /** Crater radius of Little Boy. */
    public static int boyRadius = 120;

    /** Crater radius of the Tsar Bomba. */
    public static int tsarRadius = 500;

    /** Radius used by {@code ExplosionNukeSmall.PARAMS_HIGH}, including the micro missile. */
    public static int fatmanRadius = 35;

    /** Crater radius of a nuclear missile warhead. The MIRV and doomsday warheads double it. */
    public static int missileRadius = 100;

    /** Crater radius of the N2 mine warhead; the missile uses {@code n2Radius / 12 * 5} of it. */
    public static int n2Radius = 200;

    /** Maximum milliseconds per tick allocated to the mk5 chunk-processing (ray/destruction batching). */
    /** 1.12.2 {@code 3.11_aSchrabRadius}: the anti-schrabidium (small fleija) missile warhead. */
    public static int aSchrabRadius = 20;
    /** 1.12.2 {@code 3.06_fleijaRadius}. */
    public static int fleijaRadius = 50;
    /** 1.12.2 {@code 3.07_soliniumRadius}. */
    public static int soliniumRadius = 150;
    /** 1.12.2 {@code 3.00_prototypeRadius}. */
    public static int prototypeRadius = 150;
    /** 1.7 {@code 3.08_mirvRadius}: written by 1.7, read by nothing there either (the MIRV uses twice missileRadius). */
    public static int mirvRadius = 100;
    /** 1.7 {@code 3.10_nukaRadius}: written by 1.7, read by nothing there either. */
    public static int nukaRadius = 25;
    /** 1.12.2 {@code 3.12_blastSpeed}: blocks the MK3/MK4 blast processes per tick. */
    public static int blastSpeed = 1024;
    /** 1.12.2 {@code 3.13_explosionLifespan}: 0 = unlimited MK3 blast lifetime. */
    public static int limitExplosionLifespan = 0;
    public static int mk5 = 50;

    /** Radius of the fallout area, as a percentage of the base radius. */
    public static int falloutRange = 100;

    /** Port-only and unread; not 1.7's {@code 6.04_falloutDelay}, which is {@link #fDelay}. */
    public static int falloutDelay = 30;

    /**
     * 1.7 {@code 6.04_falloutDelay} ({@code fDelay}): ticks the fallout sweep waits between two {@link #mk5}-ms apply
     * bursts. 1.7 counts {@code tickDelay} down from this and applies when it reaches 0 (then resets and decrements in
     * the same tick), so the default 4 applies on tick 5 and then every fourth tick.
     */
    public static int fDelay = 4;

    /**
     * On-demand fallout chunk requests retained per scan worker.
     *
     * <p>A deep pipeline hides generation latency, but a window of 48 saturated worldgen during a Tsar sweep
     * (1,983 unique neighbourhood chunks requested) and produced 10-second tick gaps. Sixteen still keeps hundreds
     * of chunks in flight. Each fallout entity snapshots this value when created and clamps it to 1..256, so
     * changing it affects new sweeps only.
     */
    public static int falloutClaimWindow = 16;

    /** Disables the nuclear (crater + fallout) portion of nukes, leaving only the blast damage. */
    public static boolean disableNuclear = false;

    /**
     * Clears the crater by writing whole chunk sections and repairing heightmaps/lighting once per
     * chunk, instead of calling {@code Level.setBlock} for every one of a Fat Man's millions of blocks.
     *
     * <p>Roughly 2-6x faster on the destruction phase plus the lighting backlog the per-block path leaves behind.
     * Set to false to fall back to the per-block loop if a lighting or heightmap problem shows up.
     */
    public static boolean craterSectionCarve = true;

    /**
     * Whether the fallout sweep notifies each converted block's neighbours ({@code updateNeighborsAt}).
     *
     * <p>1.12.2 does this with {@code notifyNeighborsOfStateChange}, so it is on by default. Turning it off
     * diverges: redstone, fluids and falling blocks next to a converted cell do not react until something else
     * touches them.
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
     * <p>1.12.2 has no separate shape pass, but it popped unsupported blocks from inside each block's own
     * {@code neighborChanged}; modern vanilla moved that logic into {@code updateShape}. Skipping the pass would
     * match 1.12.2's call graph, not its behaviour, and the pass is not the sweep's cost (23 ms against 48,373 ms
     * for the neighbour notifications on a real blast).
     *
     * <p>Kept as a switch because it costs 3-4 µs a cell on a surface full of things against 0.3 µs underground
     * ({@code nuke_fallout_notify_cost}). On by default because floating torches are a bug.
     */
    public static boolean falloutShapeUpdates = true;

    /** Legacy switch that gates whether the nuclear explosion happens at all. */
    public static boolean enableNukeClouds = true;

    /**
     * 1.7 {@code 6.05_enableChunkLoading} and {@code 6.06_explosionAlgorithm}: written by 1.7 but only read by
     * {@code ExplosionNukeRayParallelized}, which 1.7's mk5 never constructed (the line is commented out); the port, like
     * 1.7's effective path, uses the batched ray explosion.
     */
    public static boolean chunkloading = true;
    public static int explosionAlgorithm = 2;

    /** 1.7 {@code loadFromConfig}, categories {@code 03_nukes} and {@code 06_explosions}. */
    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_NUKES = CommonConfig.CATEGORY_NUKES;
        Configuration.Property propGadget = config.get(CATEGORY_NUKES, "3.00_gadgetRadius", 150);
        propGadget.comment = "Radius of the Gadget";
        gadgetRadius = propGadget.getInt();
        Configuration.Property propBoy = config.get(CATEGORY_NUKES, "3.01_boyRadius", 120);
        propBoy.comment = "Radius of Little Boy";
        boyRadius = propBoy.getInt();
        Configuration.Property propMan = config.get(CATEGORY_NUKES, "3.02_manRadius", 175);
        propMan.comment = "Radius of Fat Man";
        manRadius = propMan.getInt();
        Configuration.Property propMike = config.get(CATEGORY_NUKES, "3.03_mikeRadius", 250);
        propMike.comment = "Radius of Ivy Mike";
        mikeRadius = propMike.getInt();
        Configuration.Property propTsar = config.get(CATEGORY_NUKES, "3.04_tsarRadius", 500);
        propTsar.comment = "Radius of the Tsar Bomba";
        tsarRadius = propTsar.getInt();
        Configuration.Property propPrototype = config.get(CATEGORY_NUKES, "3.05_prototypeRadius", 150);
        propPrototype.comment = "Radius of the Prototype";
        prototypeRadius = propPrototype.getInt();
        Configuration.Property propFleija = config.get(CATEGORY_NUKES, "3.06_fleijaRadius", 50);
        propFleija.comment = "Radius of F.L.E.I.J.A.";
        fleijaRadius = propFleija.getInt();
        Configuration.Property propMissile = config.get(CATEGORY_NUKES, "3.07_missileRadius", 100);
        propMissile.comment = "Radius of the nuclear missile";
        missileRadius = propMissile.getInt();
        Configuration.Property propMirv = config.get(CATEGORY_NUKES, "3.08_mirvRadius", 100);
        propMirv.comment = "Radius of a MIRV";
        mirvRadius = propMirv.getInt();
        Configuration.Property propFatman = config.get(CATEGORY_NUKES, "3.09_fatmanRadius", 35);
        propFatman.comment = "Radius of the Fatman Launcher";
        fatmanRadius = propFatman.getInt();
        Configuration.Property propNuka = config.get(CATEGORY_NUKES, "3.10_nukaRadius", 25);
        propNuka.comment = "Radius of the nuka grenade";
        nukaRadius = propNuka.getInt();
        Configuration.Property propASchrab = config.get(CATEGORY_NUKES, "3.11_aSchrabRadius", 20);
        propASchrab.comment = "Radius of dropped anti schrabidium";
        aSchrabRadius = propASchrab.getInt();
        Configuration.Property propSolinium = config.get(CATEGORY_NUKES, "3.12_soliniumRadius", 150);
        propSolinium.comment = "Radius of the blue rinse";
        soliniumRadius = propSolinium.getInt();
        Configuration.Property propN2 = config.get(CATEGORY_NUKES, "3.13_n2Radius", 200);
        propN2.comment = "Radius of the N2 mine";
        n2Radius = propN2.getInt();

        final String CATEGORY_NUKE = CommonConfig.CATEGORY_EXPLOSIONS;
        Configuration.Property propLimitExplosionLifespan = config.get(CATEGORY_NUKE, "6.00_limitExplosionLifespan", 0);
        propLimitExplosionLifespan.comment = "How long an explosion can be unloaded until it dies in seconds. Based of system time. 0 disables the effect";
        limitExplosionLifespan = propLimitExplosionLifespan.getInt();
        // explosion speed
        Configuration.Property propBlastSpeed = config.get(CATEGORY_NUKE, "6.01_blastSpeed", 1024);
        propBlastSpeed.comment = "Base speed of MK3 system (old and schrabidium) detonations (Blocks / tick)";
        blastSpeed = propBlastSpeed.getInt();
        // new explosion speed
        Configuration.Property propFalloutRange = config.get(CATEGORY_NUKE, "6.02_mk5BlastTime", 50);
        propFalloutRange.comment = "Minimum amount of milliseconds per tick allocated for mk5 chunk processing";
        mk5 = propFalloutRange.getInt();
        // fallout range
        Configuration.Property falloutRangeProp = config.get(CATEGORY_NUKE, "6.03_falloutRange", 100);
        falloutRangeProp.comment = "Radius of fallout area (base radius * value in percent)";
        falloutRange = falloutRangeProp.getInt();
        Configuration.Property falloutDelayProp = config.get(CATEGORY_NUKE, "6.04_falloutDelay", 4);
        falloutDelayProp.comment = "How many ticks to wait for the next fallout chunk computation";
        fDelay = falloutDelayProp.getInt();

        chunkloading = CommonConfig.createConfigBool(config, CATEGORY_NUKE, "6.05_enableChunkLoading", "Allows all types of procedural explosions to keep the central chunk loaded and to generate new chunks.", true);
        explosionAlgorithm = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "6.06_explosionAlgorithm", "Configures the algorithm of mk5 explosion. \n0 = Legacy, 1 = Threaded DDA, 2 = Threaded DDA with damage accumulation.", 2);
    }
}
