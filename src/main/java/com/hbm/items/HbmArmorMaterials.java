package com.hbm.items;

import com.hbm.Constants;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * HBM armour materials. Physical-defense values and material properties are copied from the matching
 * 1.12.2 {@code ArmorMaterial} declarations; radiation resistance is registered separately because it
 * is not a vanilla armour-material property.
 *
 * <p>Golem Core owns native material construction. HBM declares each material exactly once.
 */
public final class HbmArmorMaterials {

    private HbmArmorMaterials() {}

    // 1.12.2 {1,4,5,2} in [boots,legs,chest,helmet] order -> helmet 2, chest 5, legs 4, boots 1.
    public static final GolemArmorMaterial HAZMAT = hazmat("hazmat", "hazmat_cloth");
    public static final GolemArmorMaterial HAZMAT_RED = hazmat("hazmat_red", "hazmat_cloth_red");
    public static final GolemArmorMaterial HAZMAT_GREY = hazmat("hazmat_grey", "hazmat_cloth_grey");

    /** 1.12.2 {@code aMatSteel}: multiplier 30, defense {3, 6, 8, 3}, enchantability 5. */
    public static final GolemArmorMaterial STEEL = new GolemArmorMaterial(
            Constants.MOD_ID, "steel", 30, 3, 8, 6, 3, 5, 0.0F, 0.0F, "repairs/steel");

    /** Legacy robes use steel's numbers but have their own armor texture path. */
    public static final GolemArmorMaterial ROBES = new GolemArmorMaterial(
            Constants.MOD_ID, "robes", 30, 3, 8, 6, 3, 5, 0.0F, 0.0F, "repairs/steel");

    /** 1.12.2 {@code aMatTitan}: multiplier 25, defense {3, 6, 8, 3}, enchantability 9. */
    public static final GolemArmorMaterial TITANIUM = new GolemArmorMaterial(
            Constants.MOD_ID, "titanium", 25, 3, 8, 6, 3, 9, 2.0F, 0.0F, "repairs/titanium");

    /**
     * T-51b power armour (1.12.2 {@code enumArmorMaterialT51}): durability 150, defense {3, 8, 6, 3} in
     * [boots, legs, chest, helmet] order, enchantability 0, toughness 2.0.
     *
     * <p>The repair tag is the same armour plate the suit is built from. Enchantability is zero because
     * upstream refuses enchantments on power armour outright — see {@code ArmorFSBPowered}.
     */
    public static final GolemArmorMaterial T51 = new GolemArmorMaterial(
            Constants.MOD_ID, "t51", 150, 3, 8, 6, 3, 0, 2.0F, 0.0F, "repairs/t51");

    /** Legacy Blackjack armor material: the separate identity is required for full-set detection. */
    public static final GolemArmorMaterial BJ = new GolemArmorMaterial(
            Constants.MOD_ID, "bj", 150, 3, 8, 6, 3, 0, 2.0F, 0.0F, "repairs/bj");

    /** Legacy DNT nanosuit material: the separate identity is required for full-set detection. */
    public static final GolemArmorMaterial DNS = new GolemArmorMaterial(
            Constants.MOD_ID, "dns", 150, 3, 8, 6, 3, 0, 2.0F, 0.0F, "repairs/dns");

    /** Jetpack modules were not armor themselves in 1.12; this material keeps direct wear non-protective. */
    public static final GolemArmorMaterial JETPACK = new GolemArmorMaterial(
            Constants.MOD_ID, "jetpack", 1, 0, 0, 0, 0, 0, 0.0F, 0.0F, "repairs/steel");

    /** 1.12.2 {@code aMatCobalt}: multiplier 70, defense {3, 6, 8, 3}, enchantability 25. */
    public static final GolemArmorMaterial COBALT = new GolemArmorMaterial(
            Constants.MOD_ID, "cobalt", 70, 3, 8, 6, 3, 25, 2.0F, 0.0F, "repairs/cobalt");

    /** 1.12.2 {@code aMatStarmetal}: multiplier 150, defense {3, 6, 8, 3}, enchantability 100. */
    public static final GolemArmorMaterial STARMETAL = new GolemArmorMaterial(
            Constants.MOD_ID, "starmetal", 150, 3, 8, 6, 3, 100, 2.0F, 0.0F, "repairs/starmetal");

    /** 1.12.2 {@code aMatSchrab}: multiplier 100, defense {3, 6, 8, 3}, enchantability 50. */
    public static final GolemArmorMaterial SCHRABIDIUM = new GolemArmorMaterial(
            Constants.MOD_ID, "schrabidium", 100, 3, 8, 6, 3, 50, 2.0F, 0.0F, "repairs/schrabidium");

    /** 1.12.2 {@code aMatSecurity}: multiplier 100, defense {3, 6, 8, 3}, enchantability 15. */
    public static final GolemArmorMaterial SECURITY = new GolemArmorMaterial(
            Constants.MOD_ID, "security", 100, 3, 8, 6, 3, 15, 2.0F, 0.0F, "repairs/security");

    /** 1.12.2 {@code aMatAsbestos}: multiplier 20, defense {1, 3, 4, 1}, enchantability 5. */
    public static final GolemArmorMaterial ASBESTOS = new GolemArmorMaterial(
            Constants.MOD_ID, "asbestos", 20, 1, 4, 3, 1, 5, 0.0F, 0.0F, "repairs/asbestos");

    /** 1.12.2 {@code aMatPaa}: multiplier 75, defense {3, 6, 8, 3}, enchantability 25. */
    public static final GolemArmorMaterial PAA = new GolemArmorMaterial(
            Constants.MOD_ID, "paa", 75, 3, 8, 6, 3, 25, 2.0F, 0.0F, "repairs/paa");

    private static GolemArmorMaterial hazmat(String name, String repairIngredient) {
        return new GolemArmorMaterial(Constants.MOD_ID, name, 15, 2, 5, 4, 1, 5,
                0.0F, 0.0F, "repairs/" + repairIngredient);
    }
}
