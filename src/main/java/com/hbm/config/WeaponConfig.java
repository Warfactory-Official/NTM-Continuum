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
    /** 1.7 {@code 10.04_dropCrys}: a xen crystal landing on the ground lifts the blocks around it (ItemDrop). */
    public static boolean dropCrys = true;
    /** 1.7 {@code 10.05_dropDead}: the dead man's explosive goes off when dropped. */
    public static boolean dropDead = true;
    /** 1.7 {@code 18.00_linearAnimations}: written by 1.7, read by nothing there either. */
    public static boolean linearAnimations = false;

    /**
     * 1.7 {@code loadFromConfig}, categories {@code 07_missile_machines}, {@code 10_dangerous_drops} and
     * {@code 18_weapons}.
     */
    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_MISSILE = CommonConfig.CATEGORY_MISSILE;
        Configuration.Property propCiwsHitrate = config.get(CATEGORY_MISSILE, "7.03_ciwsAccuracy", 50);
        propCiwsHitrate.comment = "Additional modifier for CIWS accuracy";
        ciwsHitrate = propCiwsHitrate.getInt();

        final String CATEGORY_DROPS = CommonConfig.CATEGORY_DROPS;
        dropCell = CommonConfig.createConfigBool(config, CATEGORY_DROPS, "10.00_dropCell", "Whether antimatter cells should explode when dropped", true);
        dropSing = CommonConfig.createConfigBool(config, CATEGORY_DROPS, "10.01_dropBHole", "Whether singularities and black holes should spawn when dropped", true);
        dropStar = CommonConfig.createConfigBool(config, CATEGORY_DROPS, "10.02_dropStar", "Whether rigged star blaster cells should explode when dropped", true);
        dropCrys = CommonConfig.createConfigBool(config, CATEGORY_DROPS, "10.04_dropCrys", "Whether xen crystals should move blocks when dropped", true);
        dropDead = CommonConfig.createConfigBool(config, CATEGORY_DROPS, "10.05_dropDead", "Whether dead man's explosives should explode when dropped", true);

        final String CATEGORY_WEAPONS = CommonConfig.CATEGORY_WEAPONS;
        linearAnimations = CommonConfig.createConfigBool(config, CATEGORY_WEAPONS, "18.00_linearAnimations", "Should heavily stylised weapon animations be replaced with more conventional ones?", false);
    }
}
