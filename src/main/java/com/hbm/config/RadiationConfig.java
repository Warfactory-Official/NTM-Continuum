package com.hbm.config;

/**
 * 1.7 {@code RadiationConfig}, read from {@code hbm.cfg} by {@link #loadFromConfig}. The field initialisers are 1.7's
 * effective defaults. The radiation-engine fields ({@code radTickRate}, {@code radHalfLifeSeconds},
 * {@code radDiffusivity}), {@code hazardRate}, the neutron-activation and on-ground switches, {@code rain}/{@code cont},
 * {@code disableCold}/{@code disableToxic} and {@code itemHazardDropTickRate} (a ServerConfig key) have no
 * {@code hbm.cfg} key: they are port-side constants.
 */
public final class RadiationConfig {

    private RadiationConfig() {}

    // Fallout rain (deferred systems keep their knobs so the constants stay documented)
    /** Duration of the thunderstorm after fallout in ticks (only large explosions). */
    public static int rain = 0;
    /** Radiation in 100th RADs created by fallout rain. */
    public static int cont = 0;

    // Rad fog
    /** Radiation in RADs required for fog to spawn. */
    public static int fogRad = 100;
    /** 1:n chance of fog spawning every second. 1.7 {@code FOG_01_threshold}: 20 (the 50 was 1.12.2's). */
    public static int fogCh = 20;
    /** 1.7 {@code AMBIENT_00_nether}: RAD/s in the nether, a floor under the chunk radiation there. */
    public static double hellRad = 0.1D;

    // World radiation effects (block decay)
    /** How many block operations radiation can perform per tick. */
    public static int worldRad = 10;
    /** The least amount of RADs required for block modification to happen. (Kept for documentation:
     * the fork's NT engine never read this either. Its live threshold is the hardcoded 5.0 RAD /
     * 1-in-100 roll in the decay pass, which the port matches.) */
    public static int worldRadThreshold = 20;
    /** Whether high radiation levels should perform changes in the world. */
    public static boolean worldRadEffects = true;

    // Entity contamination
    /** Toggles player contamination (and negative effects from radiation poisoning). */
    public static boolean enableContamination = true;
    /** Toggles contamination for items being on-ground. */
    public static boolean enableContaminationOnGround = false;

    // Neutron activation (off by default: laggy, and matches the old default)
    /** Whether high radiation levels should irradiate items in the inventory. */
    public static boolean neutronActivation = false;
    /** Minimum received RAD/s threshold at which items get irradiated. */
    public static int neutronActivationThreshold = 15;

    // Hazard system
    /** Ticks between application of effects for the hazards. */
    public static int hazardRate = 5;
    /** Ticks between dropped-item hazard updates; the old server-config default was 2. */
    public static int itemHazardDropTickRate = 2;
    public static boolean disableAsbestos = false;
    public static boolean disableBlinding = false;
    public static boolean disableCoal = false;
    public static boolean disableExplosive = false;
    public static boolean disableHydro = false;
    public static boolean disableHot = false;
    public static boolean disableCold = false;
    public static boolean disableToxic = false;

    // Pollution (PollutionHandler)
    /** If disabled, none of the pollution related things will work. */
    public static boolean enablePollution = true;
    /** 1.7 POL_02_enableLeadPoisoning: whether being in a heavy metal polluted area will poison the player. */
    public static boolean enableLeadPoisoning = true;
    /** 1.7 POL_04_enablePoison: whether being in a poisoned area will affect the player. */
    public static boolean enablePoison = true;
    /** The amount of soot required to buff naturally spawning mobs. */
    public static double buffMobThreshold = 15D;

    // Chunk radiation engine (RadiationSystemNT)
    /** How many ticks between each radiation system update. 1 = once per tick. */
    public static int radTickRate = 1;
    /** The half life of chunk radiation in seconds. */
    public static double radHalfLifeSeconds = 120D;
    /** The diffusivity of chunk radiation. */
    public static double radDiffusivity = 10.0;

    // 1.7 keys added with hbm.cfg
    /** 1.7 {@code RADIATION_01_enableChunkRads}: the world (chunk) radiation system; false stops its server tick. */
    public static boolean enableChunkRads = true;
    /** 1.7 {@code HAZ_06_disableFibrosis}: written by 1.7, read by nothing (there is no fibrosis hazard). */
    public static boolean disableFibrosis = false;
    /** 1.7 {@code POL_08_smokeStackSootMult}: written by 1.7, read by nothing (the chimneys use fixed factors). */
    public static double smokeStackSootMult = 0.8;
    /** 1.7 {@code POL_01_enableLeadFromBlocks}: breaking blocks in heavy-metal pollution poisons (ModEventHandlerHooks). */
    public static boolean enableLeadFromBlocks = true;
    /** 1.7 {@code POL_03..POL_07}: the client smog (client {@code SmogFog}); rampant mode scales the threshold. */
    public static boolean enableSootFog = true;
    public static double sootFogThreshold = 35D;
    public static double sootFogDivisor = 120D;

    /**
     * 1.7 {@code loadFromConfig}, categories {@code 13_radiation}, {@code 14_hazard} and {@code 16_pollution}. Not
     * written: {@code RADIATION_99_enablePRISM} (the PRISM engine is not ported; the radiation engine stays
     * RadiationSystemNT). {@code RADWORLD_03_regrow}, {@code POL_01} and the smog keys {@code POL_03/06/07} are written to hbm.cfg.
     */
    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_NUKE = CommonConfig.CATEGORY_RADIATION;

        fogRad = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "FOG_00_threshold", "Radiation in RADs required for fog to spawn", 100);
        fogCh = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "FOG_01_threshold", "1:n chance of fog spawning every second", 20);
        hellRad = CommonConfig.createConfigDouble(config, CATEGORY_NUKE, "AMBIENT_00_nether", "RAD/s in the nether", 0.1D);
        worldRadEffects = CommonConfig.createConfigBool(config, CATEGORY_NUKE, "RADWORLD_00_toggle", "Whether high radiation levels should perform changes in the world", true);
        worldRad = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "RADWORLD_01_amount", "How many block operations radiation can perform per tick", 10);
        worldRadThreshold = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "RADWORLD_02_minimum", "The least amount of RADs required for block modification to happen", 20);
        TerrainConfig.cleanupDeadDirt = CommonConfig.createConfigBool(config, CATEGORY_NUKE, "RADWORLD_03_regrow", "Whether dead grass and mycelium should decay into dirt", false);

        enableContamination = CommonConfig.createConfigBool(config, CATEGORY_NUKE, "RADIATION_00_enableContamination", "Toggles player contamination (and negative effects from radiation poisoning)", true);
        enableChunkRads = CommonConfig.createConfigBool(config, CATEGORY_NUKE, "RADIATION_01_enableChunkRads", "Toggles the world radiation system (chunk radiation only, some blocks use an AoE!)", true);

        fogCh = CommonConfig.setDef(fogCh, 20);

        final String CATEGORY_HAZ = CommonConfig.CATEGORY_HAZARD;

        disableAsbestos = CommonConfig.createConfigBool(config, CATEGORY_HAZ, "HAZ_00_disableAsbestos", "When turned off, all asbestos hazards are disabled", false);
        disableCoal = CommonConfig.createConfigBool(config, CATEGORY_HAZ, "HAZ_01_disableCoaldust", "When turned off, all coal dust hazards are disabled", false);
        disableHot = CommonConfig.createConfigBool(config, CATEGORY_HAZ, "HAZ_02_disableHot", "When turned off, all hot hazards are disabled", false);
        disableExplosive = CommonConfig.createConfigBool(config, CATEGORY_HAZ, "HAZ_03_disableExplosive", "When turned off, all explosive hazards are disabled", false);
        disableHydro = CommonConfig.createConfigBool(config, CATEGORY_HAZ, "HAZ_04_disableHydroactive", "When turned off, all hydroactive hazards are disabled", false);
        disableBlinding = CommonConfig.createConfigBool(config, CATEGORY_HAZ, "HAZ_05_disableBlinding", "When turned off, all blinding hazards are disabled", false);
        disableFibrosis = CommonConfig.createConfigBool(config, CATEGORY_HAZ, "HAZ_06_disableFibrosis", "When turned off, all fibrosis hazards are disabled", false);

        final String CATEGORY_POL = CommonConfig.CATEGORY_POLLUTION;
        enablePollution = CommonConfig.createConfigBool(config, CATEGORY_POL, "POL_00_enablePollution", "If disabled, none of the polltuion related things will work", true);
        enableLeadFromBlocks = CommonConfig.createConfigBool(config, CATEGORY_POL, "POL_01_enableLeadFromBlocks", "Whether breaking blocks in heavy metal polluted areas will poison the player", true);
        enableLeadPoisoning = CommonConfig.createConfigBool(config, CATEGORY_POL, "POL_02_enableLeadPoisoning", "Whether being in a heavy metal polluted area will poison the player", true);
        enableSootFog = CommonConfig.createConfigBool(config, CATEGORY_POL, "POL_03_enableSootFog", "Whether smog should be visible", true);
        enablePoison = CommonConfig.createConfigBool(config, CATEGORY_POL, "POL_04_enablePoison", "Whether being in a poisoned area will affect the player", true);
        buffMobThreshold = CommonConfig.createConfigDouble(config, CATEGORY_POL, "POL_05_buffMobThreshold", "The amount of soot required to buff naturally spawning mobs", 15D);
        sootFogThreshold = CommonConfig.createConfigDouble(config, CATEGORY_POL, "POL_06_sootFogThreshold", "How much soot is required for smog to become visible", 35D);
        sootFogDivisor = CommonConfig.createConfigDouble(config, CATEGORY_POL, "POL_07_sootFogDivisor", "The divisor for smog, higher numbers will require more soot for the same smog density", 120D);
        smokeStackSootMult = CommonConfig.createConfigDouble(config, CATEGORY_POL, "POL_08_smokeStackSootMult", "How much does smokestack multiply soot by, with decimal values reducing the soot", 0.8);
    }
}
