package com.hbm.config;

/**
 * The 1.7 config switches the natural/terrain blocks read, with their upstream defaults. Kept
 * feature-local (like {@code GeneralConfig} it is a plain constant holder for now).
 */
public final class TerrainConfig {

    private TerrainConfig() {}

    /** 1.7 {@code 1.01_enableMyceliumSpread}: glowing mycelium spreads under glowing mushrooms. Default off. */
    public static boolean enableMycelium = false;

    /** 1.7 {@code 1.21_enableVirus}: dark crystal eats its neighbours. Default off. */
    public static boolean enableVirus = false;

    /** 1.7 {@code RADWORLD_03_regrow}: dead grass / mycelium decays into dirt on every random tick. Default off. */
    public static boolean cleanupDeadDirt = false;
}
