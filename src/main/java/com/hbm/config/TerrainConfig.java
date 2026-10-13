package com.hbm.config;

/**
 * The 1.7 config switches the natural/terrain blocks read, with their upstream defaults. {@code enableMycelium} and
 * {@code enableVirus} are {@code hbm.cfg} keys read by {@code GeneralConfig.loadFromConfig}; {@code cleanupDeadDirt}
 * ({@code RADWORLD_03_regrow}) by {@code RadiationConfig.loadFromConfig}, read by waste earth's random tick
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
