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
}
