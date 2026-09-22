package com.hbm.config;

/**
 * Radiation / hazard tunables, ported from 1.12.2's {@code RadiationConfig}. Plain constants like
 * {@link BombConfig} (a cross-loader config layer can be added later). Defaults copied verbatim from
 * the old config; only the values the ported systems actually read are present.
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
    /** 1:n chance of fog spawning every second. */
    public static int fogCh = 50;

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
    /** The amount of soot required to buff naturally spawning mobs. */
    public static double buffMobThreshold = 15D;

    // ---- chunk radiation engine (RadiationSystemNT) ----
    /** How many ticks between each radiation system update. 1 = once per tick. */
    public static int radTickRate = 1;
    /** The half life of chunk radiation in seconds. */
    public static double radHalfLifeSeconds = 120D;
    /** The diffusivity of chunk radiation. */
    public static double radDiffusivity = 10.0;
}
