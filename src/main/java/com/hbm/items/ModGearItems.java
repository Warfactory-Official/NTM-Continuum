package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import java.util.function.Supplier;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.items.gear.ItemGasMask;
import com.hbm.items.machine.ItemGear;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import java.util.List;
import java.util.stream.IntStream;
import com.hbm.items.tool.ItemDosimeter;
import com.hbm.items.tool.ItemGeigerCounter;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatArmorItem;
import dev.golem.core.world.GolemArmorMaterial;

/**
 * Hand-registered gear: the radiation diagnostic tools (geiger counter / dosimeter) and the three hazmat
 * armor sets — the FIRST armor of the port. Kept separate from the generated {@link ModItems} so codegen
 * never clobbers them. {@link #registerHazards()} wires the {@code HazmatRegistry} resistance coefficients
 * + {@code ArmorUtil} full-set membership (called post-registration by {@code Hazards.init}).
 */
public final class ModGearItems {

    private ModGearItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    // ---- diagnostic tools ----
    public static final RegistryObject<ItemGeigerCounter> geiger_counter =
            REG.item("geiger_counter", ItemGeigerCounter::new, p -> p.stacksTo(1)).transform(Presets.sprite());
    public static final RegistryObject<ItemDosimeter> dosimeter =
            REG.item("dosimeter", ItemDosimeter::new, p -> p.stacksTo(1)).transform(Presets.sprite());
    public static final RegistryObject<ItemGear> gear_large =
            REG.item("gear_large", ItemGear::new);

    // ---- hazmat suits (yellow / red / grey) ----
    public static final RegistryObject<Item> hazmat_helmet = armor("hazmat_helmet", () -> HbmArmorMaterials.HAZMAT, ArmorSlot.HELMET);
    public static final RegistryObject<Item> hazmat_plate = armor("hazmat_plate", () -> HbmArmorMaterials.HAZMAT, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<Item> hazmat_legs = armor("hazmat_legs", () -> HbmArmorMaterials.HAZMAT, ArmorSlot.LEGGINGS);
    public static final RegistryObject<Item> hazmat_boots = armor("hazmat_boots", () -> HbmArmorMaterials.HAZMAT, ArmorSlot.BOOTS);

    public static final RegistryObject<Item> hazmat_helmet_red = armor("hazmat_helmet_red", () -> HbmArmorMaterials.HAZMAT_RED_HOOD, ArmorSlot.HELMET);
    public static final RegistryObject<Item> hazmat_plate_red = armor("hazmat_plate_red", () -> HbmArmorMaterials.HAZMAT_RED, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<Item> hazmat_legs_red = armor("hazmat_legs_red", () -> HbmArmorMaterials.HAZMAT_RED, ArmorSlot.LEGGINGS);
    public static final RegistryObject<Item> hazmat_boots_red = armor("hazmat_boots_red", () -> HbmArmorMaterials.HAZMAT_RED, ArmorSlot.BOOTS);

    public static final RegistryObject<Item> hazmat_helmet_grey = armor("hazmat_helmet_grey", () -> HbmArmorMaterials.HAZMAT_GREY_HOOD, ArmorSlot.HELMET);
    public static final RegistryObject<Item> hazmat_plate_grey = armor("hazmat_plate_grey", () -> HbmArmorMaterials.HAZMAT_GREY, ArmorSlot.CHESTPLATE);
    public static final RegistryObject<Item> hazmat_legs_grey = armor("hazmat_legs_grey", () -> HbmArmorMaterials.HAZMAT_GREY, ArmorSlot.LEGGINGS);
    public static final RegistryObject<Item> hazmat_boots_grey = armor("hazmat_boots_grey", () -> HbmArmorMaterials.HAZMAT_GREY, ArmorSlot.BOOTS);

    // ---- gas masks and replaceable filters ----
    public static final RegistryObject<ItemGasMask> gas_mask = gasMask("gas_mask", "hbm:items/gas_mask", List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));
    public static final RegistryObject<ItemGasMask> gas_mask_m65 = gasMask("gas_mask_m65", "hbm:items/gas_mask_m65", List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));
    public static final RegistryObject<ItemGasMask> gas_mask_mono = gasMask("gas_mask_mono", "hbm:items/gas_mask_mono", List.of(HazardClass.GAS_LUNG, HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT, HazardClass.BACTERIA));
    public static final RegistryObject<ItemGasMask> gas_mask_olde = gasMask("gas_mask_olde", "hbm:items/gas_mask_olde", List.of(HazardClass.GAS_BLISTERING, HazardClass.NERVE_AGENT));
    public static final RegistryObject<Item> gas_mask_filter_rag = filter("gas_mask_filter_rag", "hbm:items/gas_mask_filter_rag", 4_000);
    public static final RegistryObject<Item> gas_mask_filter_piss = filter("gas_mask_filter_piss", "hbm:items/gas_mask_filter_piss", 4_000);
    public static final RegistryObject<Item> gas_mask_filter_mono = filter("gas_mask_filter_mono", "hbm:items/gas_mask_filter_mono", 12_000);
    public static final RegistryObject<Item> gas_mask_filter = filter("gas_mask_filter", "hbm:items/gas_mask_filter", 18_000);
    public static final RegistryObject<Item> gas_mask_filter_combo = filter("gas_mask_filter_combo", "hbm:items/gas_mask_filter_combo", 24_000);

    /** The material is read when the item is built: the materials are registry-backed (see {@link ModArmorItems}). */
    private static RegistryObject<Item> armor(String name, Supplier<GolemArmorMaterial> material, ArmorSlot slot) {
        RegistryObject<Item> ro = REG.<Item>item(name, p -> new CompatArmorItem(material.get(), slot, p), p -> p)
                .transform(Presets.sprite());
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<ItemGasMask> gasMask(String name, String sprite, List<HazardClass> blacklist) {
        RegistryObject<ItemGasMask> ro = REG.item(name,
                p -> new ItemGasMask(p, blacklist), p -> p.stacksTo(1)).transform(Presets.sprite(sprite));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<Item> filter(String name, String sprite, int durability) {
        RegistryObject<Item> ro = REG.simpleItem(name, (Item.Properties p) -> p.stacksTo(1).durability(durability))
                .transform(Presets.sprite(sprite));
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, geiger_counter);
        ModTabs.addTo(HbmTab.CONSUMABLES, dosimeter);
        ModTabs.addStacksTo(HbmTab.MATERIALS, () -> IntStream.range(0, 2)
                .mapToObj(gear_large.get()::of).toList());
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
        // 1.7 ArmorUtil.register: the three hazmat hoods keep sand out (and nothing else without a filter)
        ArmorRegistry.registerHazard(hazmat_helmet.get(), HazardClass.SAND);
        ArmorRegistry.registerHazard(hazmat_helmet_red.get(), HazardClass.SAND);
        ArmorRegistry.registerHazard(hazmat_helmet_grey.get(), HazardClass.SAND);

        // 1.7 HazmatRegistry.initDefault: the two gas masks and vanilla iron/gold armour (5% each set)
        HazmatRegistry.registerHazmat(gas_mask.get(), 0.07);
        HazmatRegistry.registerHazmat(gas_mask_m65.get(), 0.095);
        HazmatRegistry.registerHazmat(net.minecraft.world.item.Items.IRON_HELMET, HazmatRegistry.iron * HazmatRegistry.helmet);
        HazmatRegistry.registerHazmat(net.minecraft.world.item.Items.IRON_CHESTPLATE, HazmatRegistry.iron * HazmatRegistry.chest);
        HazmatRegistry.registerHazmat(net.minecraft.world.item.Items.IRON_LEGGINGS, HazmatRegistry.iron * HazmatRegistry.legs);
        HazmatRegistry.registerHazmat(net.minecraft.world.item.Items.IRON_BOOTS, HazmatRegistry.iron * HazmatRegistry.boots);
        HazmatRegistry.registerHazmat(net.minecraft.world.item.Items.GOLDEN_HELMET, HazmatRegistry.gold * HazmatRegistry.helmet);
        HazmatRegistry.registerHazmat(net.minecraft.world.item.Items.GOLDEN_CHESTPLATE, HazmatRegistry.gold * HazmatRegistry.chest);
        HazmatRegistry.registerHazmat(net.minecraft.world.item.Items.GOLDEN_LEGGINGS, HazmatRegistry.gold * HazmatRegistry.legs);
        HazmatRegistry.registerHazmat(net.minecraft.world.item.Items.GOLDEN_BOOTS, HazmatRegistry.gold * HazmatRegistry.boots);
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
