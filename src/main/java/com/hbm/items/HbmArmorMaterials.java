package com.hbm.items;

import com.hbm.Constants;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * The hazmat armour materials — the FIRST armor of the port. Physical-defense values are copied from
 * 1.12.2's {@code aMatHaz}/{@code aMatHaz2}/{@code aMatHaz3} (all identical: a low-armor suit whose
 * value is radiation resistance, registered separately in {@code HazmatRegistry}).
 *
 * <p>Golem Core owns native material construction. HBM declares each material exactly once.
 */
public final class HbmArmorMaterials {

    private HbmArmorMaterials() {}

    // 1.12.2 {1,4,5,2} in [boots,legs,chest,helmet] order -> helmet 2, chest 5, legs 4, boots 1.
    public static final GolemArmorMaterial HAZMAT = hazmat("hazmat", "hazmat_cloth");
    public static final GolemArmorMaterial HAZMAT_RED = hazmat("hazmat_red", "hazmat_cloth_red");
    public static final GolemArmorMaterial HAZMAT_GREY = hazmat("hazmat_grey", "hazmat_cloth_grey");

    /**
     * T-51b power armour (1.12.2 {@code enumArmorMaterialT51}): durability 150, defense {3, 8, 6, 3} in
     * [boots, legs, chest, helmet] order, enchantability 0, toughness 2.0.
     *
     * <p>The repair tag is the same armour plate the suit is built from. Enchantability is zero because
     * upstream refuses enchantments on power armour outright — see {@code ArmorFSBPowered}.
     */
    public static final GolemArmorMaterial T51 = new GolemArmorMaterial(
            Constants.MOD_ID, "t51", 150, 3, 8, 6, 3, 0, 2.0F, 0.0F, "repairs/t51");

    private static GolemArmorMaterial hazmat(String name, String repairIngredient) {
        return new GolemArmorMaterial(Constants.MOD_ID, name, 15, 2, 5, 4, 1, 5,
                0.0F, 0.0F, "repairs/" + repairIngredient);
    }
}
