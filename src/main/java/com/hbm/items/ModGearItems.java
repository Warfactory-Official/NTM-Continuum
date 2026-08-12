package com.hbm.items;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.items.gear.ItemGasMask;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import java.util.List;
import com.hbm.items.tool.ItemDosimeter;
import com.hbm.items.tool.ItemGeigerCounter;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;

/**
 * Hand-registered gear: the radiation diagnostic tools (geiger counter / dosimeter) and the three hazmat
 * armor sets — the FIRST armor of the port. Kept separate from the generated {@link ModItems} so codegen
 * never clobbers them. {@link #registerHazards()} wires the {@code HazmatRegistry} resistance coefficients
 * + {@code ArmorUtil} full-set membership (called post-registration by {@code Hazards.init}).
 */
public final class ModGearItems {

    private ModGearItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    // ---- diagnostic tools ----
    public static final RegistryObject<ItemGeigerCounter> geiger_counter =
            ITEMS.registerItem("geiger_counter", ItemGeigerCounter::new, p -> p.stacksTo(1));
    public static final RegistryObject<ItemDosimeter> dosimeter =
            ITEMS.registerItem("dosimeter", ItemDosimeter::new, p -> p.stacksTo(1));

    // ---- hazmat suits (yellow / red / grey) ----
    public static final RegistryObject<Item> hazmat_helmet = armor("hazmat_helmet", HbmArmorMaterials.HAZMAT, ArmorType.HELMET);
    public static final RegistryObject<Item> hazmat_plate = armor("hazmat_plate", HbmArmorMaterials.HAZMAT, ArmorType.CHESTPLATE);
    public static final RegistryObject<Item> hazmat_legs = armor("hazmat_legs", HbmArmorMaterials.HAZMAT, ArmorType.LEGGINGS);
    public static final RegistryObject<Item> hazmat_boots = armor("hazmat_boots", HbmArmorMaterials.HAZMAT, ArmorType.BOOTS);

    public static final RegistryObject<Item> hazmat_helmet_red = armor("hazmat_helmet_red", HbmArmorMaterials.HAZMAT_RED, ArmorType.HELMET);
    public static final RegistryObject<Item> hazmat_plate_red = armor("hazmat_plate_red", HbmArmorMaterials.HAZMAT_RED, ArmorType.CHESTPLATE);
    public static final RegistryObject<Item> hazmat_legs_red = armor("hazmat_legs_red", HbmArmorMaterials.HAZMAT_RED, ArmorType.LEGGINGS);
    public static final RegistryObject<Item> hazmat_boots_red = armor("hazmat_boots_red", HbmArmorMaterials.HAZMAT_RED, ArmorType.BOOTS);

    public static final RegistryObject<Item> hazmat_helmet_grey = armor("hazmat_helmet_grey", HbmArmorMaterials.HAZMAT_GREY, ArmorType.HELMET);
    public static final RegistryObject<Item> hazmat_plate_grey = armor("hazmat_plate_grey", HbmArmorMaterials.HAZMAT_GREY, ArmorType.CHESTPLATE);
    public static final RegistryObject<Item> hazmat_legs_grey = armor("hazmat_legs_grey", HbmArmorMaterials.HAZMAT_GREY, ArmorType.LEGGINGS);
    public static final RegistryObject<Item> hazmat_boots_grey = armor("hazmat_boots_grey", HbmArmorMaterials.HAZMAT_GREY, ArmorType.BOOTS);

    // ---- gas masks and replaceable filters ----
    public static final RegistryObject<ItemGasMask> gas_mask = gasMask("gas_mask", List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));
    public static final RegistryObject<ItemGasMask> gas_mask_m65 = gasMask("gas_mask_m65", List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));
    public static final RegistryObject<ItemGasMask> gas_mask_mono = gasMask("gas_mask_mono", List.of(HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT, HazardClass.BACTERIA));
    public static final RegistryObject<ItemGasMask> gas_mask_olde = gasMask("gas_mask_olde", List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));
    public static final RegistryObject<Item> gas_mask_filter_rag = filter("gas_mask_filter_rag", 4_000);
    public static final RegistryObject<Item> gas_mask_filter_piss = filter("gas_mask_filter_piss", 4_000);
    public static final RegistryObject<Item> gas_mask_filter_mono = filter("gas_mask_filter_mono", 12_000);
    public static final RegistryObject<Item> gas_mask_filter = filter("gas_mask_filter", 18_000);
    public static final RegistryObject<Item> gas_mask_filter_combo = filter("gas_mask_filter_combo", 24_000);

    private static RegistryObject<Item> armor(String name, ArmorMaterial mat, ArmorType type) {
        RegistryObject<Item> ro = ITEMS.registerItem(name, Item::new, (UnaryOperator<Item.Properties>) p -> p.humanoidArmor(mat, type));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<ItemGasMask> gasMask(String name, List<HazardClass> blacklist) {
        RegistryObject<ItemGasMask> ro = ITEMS.registerItem(name, p -> new ItemGasMask(p, blacklist),
                p -> p.humanoidArmor(HbmArmorMaterials.HAZMAT, ArmorType.HELMET).stacksTo(1));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<Item> filter(String name, int durability) {
        RegistryObject<Item> ro = ITEMS.registerSimpleItem(name, p -> p.stacksTo(1).durability(durability));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, geiger_counter);
        ModTabs.addTo(HbmTab.CONSUMABLES, dosimeter);
    }

    /** Wires the radiation-resistance coefficients + full-set membership (post-registration). */
    public static void registerHazards() {
        registerSet(HazmatRegistry.hazYellow, hazmat_helmet, hazmat_plate, hazmat_legs, hazmat_boots);
        registerSet(HazmatRegistry.hazRed, hazmat_helmet_red, hazmat_plate_red, hazmat_legs_red, hazmat_boots_red);
        registerSet(HazmatRegistry.hazGray, hazmat_helmet_grey, hazmat_plate_grey, hazmat_legs_grey, hazmat_boots_grey);
        ArmorRegistry.registerHazard(gas_mask_filter_rag.get(), HazardClass.PARTICLE_COARSE);
        ArmorRegistry.registerHazard(gas_mask_filter_piss.get(), HazardClass.PARTICLE_COARSE, HazardClass.GAS_LUNG);
        ArmorRegistry.registerHazard(gas_mask_filter_mono.get(), HazardClass.PARTICLE_COARSE, HazardClass.GAS_MONOXIDE);
        ArmorRegistry.registerHazard(gas_mask_filter.get(), HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE,
                HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA);
        ArmorRegistry.registerHazard(gas_mask_filter_combo.get(), HazardClass.PARTICLE_COARSE, HazardClass.PARTICLE_FINE,
                HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.BACTERIA, HazardClass.GAS_MONOXIDE);
        ArmorRegistry.registerHazard(gas_mask.get(), HazardClass.SAND, HazardClass.LIGHT);
        ArmorRegistry.registerHazard(gas_mask_m65.get(), HazardClass.SAND);
    }

    private static void registerSet(double koeff, RegistryObject<Item> helmet, RegistryObject<Item> chest,
                                    RegistryObject<Item> legs, RegistryObject<Item> boots) {
        HazmatRegistry.registerHazmat(helmet.get(), koeff * HazmatRegistry.helmet);
        HazmatRegistry.registerHazmat(chest.get(), koeff * HazmatRegistry.chest);
        HazmatRegistry.registerHazmat(legs.get(), koeff * HazmatRegistry.legs);
        HazmatRegistry.registerHazmat(boots.get(), koeff * HazmatRegistry.boots);
        ArmorUtil.registerHazmatSet(helmet.get(), chest.get(), legs.get(), boots.get());
    }

    public static void init() {}
}
