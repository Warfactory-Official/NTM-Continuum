package com.hbm.config;

/**
 * General switches, ported from 1.12.2's {@code GeneralConfig} (plain constants like {@link BombConfig}).
 * Only the radiation-relevant switches are present so far — grow as more systems land.
 */
public final class GeneralConfig {

    private GeneralConfig() {}

    /** GENERAL SWITCH: enables the radiation system. */
    public static boolean enableRads = true;

    /** Enables the advanced (pocket-based) chunk radiation system. This fork has no simple fallback. */
    public static boolean advancedRadiation = true;

    /** Matches the old default: uranium ores emit radon unless a pack disables it. */
    public static boolean enableRadon = true;

    /** Old {@code 1.26_enableAsbestosDust}. */
    public static boolean enableAsbestosDust = true;

    /** Old {@code 1.26_enableCarbonMonoxide}. */
    public static boolean enableCarbonMonoxide = true;

    /** One-in-N random ticks before radon dissipates; dense radon deposits fallout when it expires. */
    public static int radonDecayChance = 20;

    /** Allows dense radon to leave a fallout layer when it dissipates. */
    public static boolean denseRadonFallout = true;

    // ---------------------------------------------------------------- worldgen switches (see WorldConfig)

    /**
     * Old {@code 1.02_enablePlutoniumNetherOre}. <b>Off by default upstream</b>, so nether plutonium
     * ore does not generate unless a pack turns it on.
     */
    public static boolean enablePlutoniumOre = false;

    /** Old {@code 1.26_enableFlammableGas}. */
    public static boolean enableFlammableGas = true;

    /**
     * Old {@code 1.26_enableExplosiveGas}. (Upstream really did give both gas switches the same
     * {@code 1.26_} key prefix — a config-file collision in 1.12.2, irrelevant here.)
     */
    public static boolean enableExplosiveGas = true;

    /** Old {@code X528_enableColtanSpawning} — the plain scattered coltan vein. Off by default. */
    public static boolean enable528ColtanSpawn = false;

    /** Old {@code X528_enableColtanDepsoit} (sic) — the gaussian coltan deposit field. */
    public static boolean enable528ColtanDeposit = true;

    /** Old {@code X528_oreColtanFrequency} — vein attempts per chunk for the scattered coltan. */
    public static int coltanRate = 2;
}
