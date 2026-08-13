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

//? if >=26.1 {
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
//?} else {
/*import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;
*///?}

/**
 * The hazmat armour materials — the FIRST armor of the port. Physical-defense values are copied from
 * 1.12.2's {@code aMatHaz}/{@code aMatHaz2}/{@code aMatHaz3} (all identical: a low-armor suit whose
 * value is radiation resistance, registered separately in {@code HazmatRegistry}).
 *
 * <p>This class is version-split rather than routed through a Golem facade because the two armour
 * APIs differ structurally, not cosmetically:
 * <ul>
 *   <li><b>26.1</b> — {@code ArmorMaterial} is a plain value referencing a {@code ResourceKey<EquipmentAsset>};
 *       worn visuals come from {@code assets/hbm/equipment/&lt;asset&gt;.json}, and armour items are
 *       ordinary {@code Item}s configured with {@code Properties.humanoidArmor(...)}.</li>
 *   <li><b>pre-26.1</b> — {@code ArmorMaterial} carries its texture {@code Layer}s inline, must be
 *       <i>registered</i> to obtain a {@code Holder}, and armour items are {@code ArmorItem} subclasses
 *       taking that holder.</li>
 * </ul>
 * A facade hiding that would be larger than the two implementations and would leak either way.
 * The defense values, repair tags and asset names are identical across both branches.
 */
public final class HbmArmorMaterials {

    private HbmArmorMaterials() {}

    //? if >=26.1 {
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
    //?} else {
    /*private static final Map<ArmorItem.Type, Integer> HAZMAT_DEFENSE = defense(2, 5, 4, 1);

    /^*
     * Deferred registry for armour materials. Registering eagerly in a static initialiser throws
     * "Registry is already frozen" — vanilla registries are closed long before mod class-loading —
     * so these flow through the same deferred flush as every other registered object. The flush order
     * puts ARMOR_MATERIAL before ITEM so armour item factories can resolve their holder.
     ^/
    private static final dev.golem.core.registry.DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            dev.golem.core.registry.DeferredRegister.create(
                    net.minecraft.core.registries.Registries.ARMOR_MATERIAL, Constants.MOD_ID);


    public static final dev.golem.core.registry.RegistryObject<ArmorMaterial> HAZMAT = hazmat("hazmat", repairTag("hazmat_cloth"));
    public static final dev.golem.core.registry.RegistryObject<ArmorMaterial> HAZMAT_RED = hazmat("hazmat_red", repairTag("hazmat_cloth_red"));
    public static final dev.golem.core.registry.RegistryObject<ArmorMaterial> HAZMAT_GREY = hazmat("hazmat_grey", repairTag("hazmat_cloth_grey"));

    private static dev.golem.core.registry.RegistryObject<ArmorMaterial> hazmat(String name, TagKey<Item> repair) {
        // Pre-26.1 armour textures are addressed by layer name rather than an equipment asset json:
        // assets/hbm/textures/models/armor/<name>_layer_1.png (and _2 for leggings).
        return ARMOR_MATERIALS.register(name, () -> new ArmorMaterial(
                HAZMAT_DEFENSE, 5, SoundEvents.ARMOR_EQUIP_GENERIC,
                () -> Ingredient.of(BuiltInRegistries.ITEM.getTag(repair)
                        .map(h -> h.stream().map(e -> new net.minecraft.world.item.ItemStack(e))
                                .toArray(net.minecraft.world.item.ItemStack[]::new))
                        .orElse(new net.minecraft.world.item.ItemStack[0])),
                List.of(new ArmorMaterial.Layer(Identifier.fromNamespaceAndPath(Constants.MOD_ID, name))),
                0.0F, 0.0F));
    }

    /^* The registered material as a Holder, resolved after the ARMOR_MATERIAL flush. ^/
    public static Holder<ArmorMaterial> holder(dev.golem.core.registry.RegistryObject<ArmorMaterial> material) {
        return BuiltInRegistries.ARMOR_MATERIAL.wrapAsHolder(material.get());
    }

    private static Map<ArmorItem.Type, Integer> defense(int helmet, int chest, int legs, int boots) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.CHESTPLATE, chest);
        map.put(ArmorItem.Type.LEGGINGS, legs);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.BODY, chest);
        return map;
    }
    *///?}

    private static TagKey<Item> repairTag(String clothId) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Constants.MOD_ID, "repairs/" + clothId));
    }
}
