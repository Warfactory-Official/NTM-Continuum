package com.hbm.config;

/**
 * 1.7 {@code RadiationConfig}, read from {@code hbm.cfg} by {@link #loadFromConfig}. The field initialisers are 1.7's
 * effective defaults. The radiation-engine fields ({@code radTickRate}, {@code radHalfLifeSeconds},
 * {@code radDiffusivity}), {@code hazardRate}, the neutron-activation and on-ground switches, {@code rain}/{@code cont},
 * {@code disableCold}/{@code disableToxic} and {@code itemHazardDropTickRate} (a ServerConfig key) have no
 * {@code hbm.cfg} key: they are port-side constants (History §19ej-8).
 */
public final class RadiationConfig {

    private RadiationConfig() {}

    // ---- fallout rain (deferred systems keep their knobs so the constants stay documented) ----
    /** Duration of the thunderstorm after fallout in ticks (only large explosions). */
    public static int rain = 0;
    /** Radiation in 100th RADs created by fallout rain. */
    public static int cont = 0;

    // ---- rad fog ----
    /** Radiation in RADs required for fog to spawn. */
    public static int fogRad = 100;
    /** 1:n chance of fog spawning every second. 1.7 {@code FOG_01_threshold}: 20 (the 50 was 1.12.2's). */
    public static int fogCh = 20;
    /** 1.7 {@code AMBIENT_00_nether}: RAD/s in the nether, a floor under the chunk radiation there. */
    public static double hellRad = 0.1D;

    // ---- world radiation effects (block decay) ----
    /** How many block operations radiation can perform per tick. */
    public static int worldRad = 10;
    /** The least amount of RADs required for block modification to happen. (Kept for documentation:
     * the fork's NT engine never read this either — its live threshold is the hardcoded 5.0 RAD /
     * 1-in-100 roll in the decay pass, which the port matches.) */
    public static int worldRadThreshold = 20;
    /** Whether high radiation levels should perform changes in the world. */
    public static boolean worldRadEffects = true;

    // ---- entity contamination ----
    /** Toggles player contamination (and negative effects from radiation poisoning). */
    public static boolean enableContamination = true;
    /** Toggles contamination for items being on-ground. */
    public static boolean enableContaminationOnGround = false;

    // ---- neutron activation (kept off — laggy, faithful to old default) ----
    /** Whether high radiation levels should irradiate items in the inventory. */
    public static boolean neutronActivation = false;
    /** Minimum received RAD/s threshold at which items get irradiated. */
    public static int neutronActivationThreshold = 15;

    // ---- hazard system ----
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

    // ---- pollution (PollutionHandler) ----
    /** If disabled, none of the pollution related things will work. */
    public static boolean enablePollution = true;
    /** 1.7 POL_02_enableLeadPoisoning: whether being in a heavy metal polluted area will poison the player. */
    public static boolean enableLeadPoisoning = true;
    /** 1.7 POL_04_enablePoison: whether being in a poisoned area will affect the player. */
    public static boolean enablePoison = true;
    /** The amount of soot required to buff naturally spawning mobs. */
    public static double buffMobThreshold = 15D;

    // ---- chunk radiation engine (RadiationSystemNT) ----
    /** How many ticks between each radiation system update. 1 = once per tick. */
    public static int radTickRate = 1;
    /** The half life of chunk radiation in seconds. */
    public static double radHalfLifeSeconds = 120D;
    /** The diffusivity of chunk radiation. */
    public static double radDiffusivity = 10.0;

    // ---- 1.7 keys added with hbm.cfg (History §19ej-8) ----
    /** 1.7 {@code RADIATION_01_enableChunkRads}: the world (chunk) radiation system; false stops its server tick. */
    public static boolean enableChunkRads = true;
    /** 1.7 {@code HAZ_06_disableFibrosis}: written by 1.7, read by nothing (there is no fibrosis hazard). */
    public static boolean disableFibrosis = false;
    /** 1.7 {@code POL_08_smokeStackSootMult}: written by 1.7, read by nothing (the chimneys use fixed factors). */
    public static double smokeStackSootMult = 0.8;
    /*
     * 1.7 POL_03..POL_07 smog: the client soot fog is not ported (no soot sync to the client), so the keys are not
     * written. The fields stay because 1.7's rampant mode scales sootFogThreshold (MobConfig.loadFromConfig).
     */
    public static boolean enableSootFog = true;
    public static double sootFogThreshold = 35D;
    public static double sootFogDivisor = 120D;

    /**
     * 1.7 {@code loadFromConfig}, categories {@code 13_radiation}, {@code 14_hazard} and {@code 16_pollution}. Not
     * written: {@code RADIATION_99_enablePRISM} (the PRISM engine is not ported; the radiation engine stays
     * RadiationSystemNT), {@code RADWORLD_03_regrow} (waste earth has no decay tick yet), {@code POL_01_enableLeadFromBlocks}
     * (no block-break hook yet) and the smog keys {@code POL_03}, {@code POL_06}, {@code POL_07}.
     */
    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_NUKE = CommonConfig.CATEGORY_RADIATION;

        fogRad = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "FOG_00_threshold", "Radiation in RADs required for fog to spawn", 100);
        fogCh = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "FOG_01_threshold", "1:n chance of fog spawning every second", 20);
        hellRad = CommonConfig.createConfigDouble(config, CATEGORY_NUKE, "AMBIENT_00_nether", "RAD/s in the nether", 0.1D);
        worldRadEffects = CommonConfig.createConfigBool(config, CATEGORY_NUKE, "RADWORLD_00_toggle", "Whether high radiation levels should perform changes in the world", true);
        worldRad = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "RADWORLD_01_amount", "How many block operations radiation can perform per tick", 10);
        worldRadThreshold = CommonConfig.createConfigInt(config, CATEGORY_NUKE, "RADWORLD_02_minimum", "The least amount of RADs required for block modification to happen", 20);

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
        enableLeadPoisoning = CommonConfig.createConfigBool(config, CATEGORY_POL, "POL_02_enableLeadPoisoning", "Whether being in a heavy metal polluted area will poison the player", true);
        enablePoison = CommonConfig.createConfigBool(config, CATEGORY_POL, "POL_04_enablePoison", "Whether being in a poisoned area will affect the player", true);
        buffMobThreshold = CommonConfig.createConfigDouble(config, CATEGORY_POL, "POL_05_buffMobThreshold", "The amount of soot required to buff naturally spawning mobs", 15D);
        smokeStackSootMult = CommonConfig.createConfigDouble(config, CATEGORY_POL, "POL_08_smokeStackSootMult", "How much does smokestack multiply soot by, with decimal values reducing the soot", 0.8);
        // the unwritten smog keys at their 1.7 defaults, so rampant mode's *= pollutionMult starts from 35 on every load
        enableSootFog = true;
        sootFogThreshold = 35D;
        sootFogDivisor = 120D;
    }
}
