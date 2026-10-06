package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModWeaponModItems;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;

/** The single common bootstrap boundary for the original-hbm weapon catalogue. */
public final class GunCatalogue {
    public static final int EXPECTED_GUN_ITEMS = 70;
    // 118 before History §19es-itemvariant split ammo_arty (12), ammo_himars (8) and ammo_container (2) into one item each
    public static final int EXPECTED_AMMO_ITEMS = 137;
    // Original-hbm's complete GunFactory chain constructs 180 configs, including secret and helper rounds; 181 was the
    // port's count before §19ek-hooks, which added 1.7's TileEntityBatterySocket.discharge (a static BulletConfig of the
    // battery socket, registered beside the gun configs).
    public static final int EXPECTED_BULLET_CONFIGS = 182;

    private static boolean initialized;

    private GunCatalogue() {}

    /** Declares every weapon register and every factory config before loaders flush registries. */
    public static synchronized void init() {
        if (initialized) return;

        // Load item declarations before any factory config can bind an ammo identity.
        ModAmmoItems.init();
        BallisticGunCatalog.init();
        SpecialGunCatalog.init();
        NtmEnergyGunCatalog.init();

        // Keep the source factory order in one place; gun suppliers are evaluated only after this.
        GunFactory.init();
        ModGunItems.init();
        ModWeaponModItems.init();
        XWeaponModManager.init();
        initialized = true;
    }
}
