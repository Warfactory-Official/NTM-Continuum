package com.hbm.items;

import java.util.EnumMap;
import java.util.Map;

import com.hbm.Constants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * The hazmat {@link ArmorMaterial}s — the FIRST armor of the port. Physical-defense values are copied
 * from 1.12.2's {@code aMatHaz}/{@code aMatHaz2}/{@code aMatHaz3} (all identical: a low-armor suit whose
 * value is radiation resistance, registered separately in {@code HazmatRegistry}). Worn visuals come
 * from {@code assets/hbm/equipment/<asset>.json} (see {@code #assetKey}); the repair ingredient is the
 * matching hazmat cloth (tagged in {@code data/hbm/tags/item/repairs/*}).
 */
public final class HbmArmorMaterials {

    private HbmArmorMaterials() {}

    // 1.12.2 {1,4,5,2} in [boots,legs,chest,helmet] order → helmet 2, chest 5, legs 4, boots 1.
    private static final Map<ArmorType, Integer> HAZMAT_DEFENSE = defense(2, 5, 4, 1);

    public static final ResourceKey<EquipmentAsset> ASSET_HAZMAT = assetKey("hazmat");
    public static final ResourceKey<EquipmentAsset> ASSET_HAZMAT_RED = assetKey("hazmat_red");
    public static final ResourceKey<EquipmentAsset> ASSET_HAZMAT_GREY = assetKey("hazmat_grey");

    public static final ArmorMaterial HAZMAT = hazmat(ASSET_HAZMAT, repairTag("hazmat_cloth"));
    public static final ArmorMaterial HAZMAT_RED = hazmat(ASSET_HAZMAT_RED, repairTag("hazmat_cloth_red"));
    public static final ArmorMaterial HAZMAT_GREY = hazmat(ASSET_HAZMAT_GREY, repairTag("hazmat_cloth_grey"));

    private static ArmorMaterial hazmat(ResourceKey<EquipmentAsset> asset, TagKey<Item> repair) {
        return new ArmorMaterial(15, HAZMAT_DEFENSE, 5, SoundEvents.ARMOR_EQUIP_GENERIC, 0.0F, 0.0F, repair, asset);
    }

    private static Map<ArmorType, Integer> defense(int helmet, int chest, int legs, int boots) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.CHESTPLATE, chest);
        map.put(ArmorType.LEGGINGS, legs);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.BODY, chest);
        return map;
    }

    private static ResourceKey<EquipmentAsset> assetKey(String name) {
        return ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Constants.MOD_ID, name));
    }

    private static TagKey<Item> repairTag(String clothId) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "repairs/" + clothId));
    }
}
