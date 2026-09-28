package com.hbm.config;

/**
 * General switches, ported from 1.12.2's {@code GeneralConfig} (plain constants like {@link BombConfig}).
 * Only the switches needed by ported systems are present so far — grow as more systems land.
 */
public final class GeneralConfig {

    /** 1.12.2 {@code 1.14_enableBomberShortMode}: bombers spawn closer to the target for small render distances. */
    public static boolean enableBomberShortMode = false;
    /** 1.12.2 {@code 1.18_enableExtendedLogging}: logs detonator use, nuclear explosions, launches, grenades. */
    public static boolean enableExtendedLogging = false;

    private GeneralConfig() {}

    /** GENERAL SWITCH: enables the radiation system. */
    public static boolean enableRads = true;
    /** 1.7 {@code 1.06_enableRadHotspotSpawn}: lets the Sellafield crater ("radiation hotspot") generate. */
    public static boolean enableRad = true;

    /** Enables the advanced (pocket-based) chunk radiation system. This fork has no simple fallback. */
    public static boolean advancedRadiation = true;

    /** Matches the old default: uranium ores emit radon unless a pack disables it. */
    public static boolean enableRadon = true;

    /** Old {@code 1.26_enableAsbestosDust}. */
    public static boolean enableAsbestosDust = true;

    /** Old {@code 1.26_enableCarbonMonoxide}. */
    public static boolean enableCarbonMonoxide = true;

    /** Old {@code 1.99_enableExpensiveMode}; selects the alternate Exposure Chamber catalyst. */
    public static boolean enableExpensiveMode = false;

    /** The legacy central 528-mode toggle; precision-assembler recipes remain disabled by default. */
    public static boolean enable528 = false;

    /**
     * The HE side of the HE:FE exchange ratio. With {@link #energyRatioFe} it says {@code energyRatioHe}
     * HE is worth exactly {@code energyRatioFe} FE, in both directions. The default is 1.7's own
     * HE&rarr;RF converter rate: {@code TileEntityConverterHeRf.heInput} is 5 against an RF output of 1.
     */
    public static int energyRatioHe = 5;

    /** The FE side of the ratio. See {@link #energyRatioHe}. */
    public static int energyRatioFe = 1;

    /**
     * The fraction of a power net's supply that foreign (FE) machines may take at normal priority,
     * ahead of the leftover surplus they receive anyway. Zero keeps NTM machines strictly first.
     */
    public static double foreignExportReserve = 0D;

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
