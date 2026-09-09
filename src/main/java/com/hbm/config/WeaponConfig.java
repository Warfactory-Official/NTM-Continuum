package com.hbm.config;

/** Drop-triggered weapon switches retained from 1.12.2's {@code WeaponConfig}. */
public final class WeaponConfig {
    private WeaponConfig() {}

    /** Old {@code 10.01_dropBHole}: dropped singularities create their configured vortex. */
    public static boolean dropSing = true;

    /** Old {@code 10.00_dropCell}: dropped antimatter cells explode. */
    public static boolean dropCell = true;
}
