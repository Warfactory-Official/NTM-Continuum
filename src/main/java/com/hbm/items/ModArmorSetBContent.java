package com.hbm.items;

import com.hbm.blocks.ModArmorSetBBlocks;
import com.hbm.tileentity.ModArmorSetBBlockEntities;

/**
 * One bootstrap hook for the final armour wave B: the remaining 1.7 {@code ModItemsArmor} ids, the IV bags and the
 * {@code statue_elb_f} block the euphemium kit hands out. Set bonuses are configured post-registration from
 * {@code Hazards.init}, resistances and worn-armour damage handlers from {@code DamageResistanceHandler.init}.
 */
public final class ModArmorSetBContent {

    private ModArmorSetBContent() {}

    public static void init() {
        ModArmorSetBItems.init();
        ModArmorSetBBlocks.init();
        ModArmorSetBBlockEntities.init();
        ArmorSetBDeclarations.declare();
    }
}
