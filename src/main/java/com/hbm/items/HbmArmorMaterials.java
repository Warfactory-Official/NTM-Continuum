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

    private static GolemArmorMaterial hazmat(String name, String repairIngredient) {
        return new GolemArmorMaterial(Constants.MOD_ID, name, 15, 2, 5, 4, 1, 5,
                0.0F, 0.0F, "repairs/" + repairIngredient);
    }
}
