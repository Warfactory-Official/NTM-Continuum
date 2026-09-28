package com.hbm.config;

/** Drop-triggered weapon switches retained from 1.12.2's {@code WeaponConfig}. */
public final class WeaponConfig {
    private WeaponConfig() {}

    /** Old {@code 7.03_ciwsAccuracy}: hit chance for the Howard CIWS in percent. */
    public static int ciwsHitrate = 50;

    /** Old {@code 10.01_dropBHole}: dropped singularities create their configured vortex. */
    public static boolean dropSing = true;

    /** Old {@code 10.00_dropCell}: dropped antimatter cells explode. */
    public static boolean dropCell = true;

    /** Old {@code 10.02_dropStar}: rigged star blaster cells explode when dropped. */
    public static boolean dropStar = true;
}
