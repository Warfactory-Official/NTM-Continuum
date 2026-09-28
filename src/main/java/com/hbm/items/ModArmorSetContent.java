package com.hbm.items;

/**
 * One bootstrap hook for the remaining full armour sets of 1.7 {@code ModItemsArmor} (part A): the items
 * and their declarations. Set bonuses are configured post-registration from {@code Hazards.init}, the damage
 * resistance profiles from {@code DamageResistanceHandler.init}.
 */
public final class ModArmorSetContent {

    private ModArmorSetContent() {}

    public static void init() {
        ModArmorSetItems.init();
        ArmorSetDeclarations.declare();
    }
}
