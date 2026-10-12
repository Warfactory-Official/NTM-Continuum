package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.armor.ArmorHazmat;
import com.hbm.items.armor.ArmorHazmatMask;
import com.hbm.items.armor.ArmorLiquidator;
import com.hbm.items.armor.ArmorLiquidatorMask;
import com.hbm.items.armor.ItemModAuto;
import com.hbm.items.armor.ItemModBandaid;
import com.hbm.items.armor.ItemModBathwater;
import com.hbm.items.armor.ItemModCard;
import com.hbm.items.armor.ItemModCharm;
import com.hbm.items.armor.ItemModGasmask;
import com.hbm.items.armor.ItemModHealth;
import com.hbm.items.armor.ItemModInk;
import com.hbm.items.armor.ItemModIron;
import com.hbm.items.armor.ItemModKnife;
import com.hbm.items.armor.ItemModLens;
import com.hbm.items.armor.ItemModLodestone;
import com.hbm.items.armor.ItemModMedal;
import com.hbm.items.armor.ItemModMilk;
import com.hbm.items.armor.ItemModMorningGlory;
import com.hbm.items.armor.ItemModNightVision;
import com.hbm.items.armor.ItemModObsidian;
import com.hbm.items.armor.ItemModPads;
import com.hbm.items.armor.ItemModRevive;
import com.hbm.items.armor.ItemModSensor;
import com.hbm.items.armor.ItemModSerum;
import com.hbm.items.armor.ItemModServos;
import com.hbm.items.armor.ItemModShackles;
import com.hbm.items.armor.ItemModShield;
import com.hbm.items.armor.ItemModTesla;
import com.hbm.items.armor.ItemModTwoKick;
import com.hbm.items.armor.ItemModWD40;
import com.hbm.items.armor.MaskOfInfamy;
import com.hbm.sound.ModSounds;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.function.Function;
import net.minecraft.world.item.Item;

/**
 * The remaining armor-table modules of 1.7 {@code ModItems} (the {@code ItemMod*} family beside the
 * inserts, claddings and batteries in {@link ModArmorModules}) and the last three armor sets: the PaA
 * battle hazmat suit, the Liquidator suit and the Mask of Infamy. Behaviour hooks live in
 * {@link ArmorModEvents}; models, recipes and tags in {@code ArmorModDeclarations}.
 */
public final class ModArmorModItems {

    private ModArmorModItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    // ---- materials (1.7 order {helmet, chest, legs, boots}; 1.7 armor had no toughness) ----
    /**
     * Held apart so only the item factories reach them: the materials are registry-backed, and this class also
     * runs at build time (Dejson) without a game.
     */
    public static final class Materials {
        private Materials() {}

        /** 1.7 {@code aMatPaa} ("HBM_PAA", 75, {3, 8, 6, 3}, 25). Its own asset: the hazmat_paa texture pair. */
        public static final GolemArmorMaterial HAZMAT_PAA = new GolemArmorMaterial(
                Constants.MOD_ID, "hazmat_paa", 75, 3, 8, 6, 3, 25, 0.0F, 0.0F, "repairs/paa");
        /** 1.7 {@code aMatLiquidator} ("HBM_LIQUIDATOR", 750, {3, 8, 6, 3}, 10), repaired with lead plates. */
        public static final GolemArmorMaterial LIQUIDATOR = new GolemArmorMaterial(
                Constants.MOD_ID, "liquidator", 750, 3, 8, 6, 3, 10, 0.0F, 0.0F, "repairs/liquidator");
        /** Vanilla 1.7 {@code ArmorMaterial.IRON} (15, {2, 6, 5, 2}, 9) with the mask's own texture. */
        public static final GolemArmorMaterial INFAMY = new GolemArmorMaterial(
                Constants.MOD_ID, "infamy", 15, 2, 6, 5, 2, 9, 0.0F, 0.0F, "repairs/infamy");
    }

    // ---- armor modules, 1.7 ModItems order ----
    public static final RegistryObject<ItemModGasmask> attachment_mask = mod("attachment_mask", p -> new ItemModGasmask(false, p));
    public static final RegistryObject<ItemModGasmask> attachment_mask_mono = mod("attachment_mask_mono", p -> new ItemModGasmask(true, p));
    public static final RegistryObject<ItemModTesla> back_tesla = mod("back_tesla", ItemModTesla::new);
    public static final RegistryObject<ItemModServos> servo_set = mod("servo_set", p -> new ItemModServos(false, p));
    public static final RegistryObject<ItemModServos> servo_set_desh = mod("servo_set_desh", p -> new ItemModServos(true, p));
    public static final RegistryObject<ItemModPads> pads_rubber = mod("pads_rubber", p -> new ItemModPads(0.5F, false, p));
    public static final RegistryObject<ItemModPads> pads_slime = mod("pads_slime", p -> new ItemModPads(0.25F, false, p));
    public static final RegistryObject<ItemModPads> pads_static = mod("pads_static", p -> new ItemModPads(0.75F, true, p));
    public static final RegistryObject<ItemModIron> cladding_iron = mod("cladding_iron", ItemModIron::new);
    public static final RegistryObject<ItemModObsidian> cladding_obsidian = mod("cladding_obsidian", ItemModObsidian::new);
    public static final RegistryObject<ItemModBandaid> bandaid = mod("bandaid", ItemModBandaid::new);
    public static final RegistryObject<ItemModSerum> serum = mod("serum", ItemModSerum::new);
    public static final RegistryObject<ItemModMorningGlory> morning_glory = mod("morning_glory", ItemModMorningGlory::new);
    public static final RegistryObject<ItemModLodestone> lodestone = mod("lodestone", p -> new ItemModLodestone(5, p));
    public static final RegistryObject<ItemModLodestone> horseshoe_magnet = mod("horseshoe_magnet", p -> new ItemModLodestone(8, p));
    public static final RegistryObject<ItemModLodestone> industrial_magnet = mod("industrial_magnet", p -> new ItemModLodestone(12, p));
    public static final RegistryObject<ItemModBathwater> bathwater = mod("bathwater", p -> new ItemModBathwater(false, p));
    public static final RegistryObject<ItemModBathwater> bathwater_mk2 = mod("bathwater_mk2", p -> new ItemModBathwater(true, p));
    public static final RegistryObject<ItemModMilk> spider_milk = mod("spider_milk", ItemModMilk::new);
    public static final RegistryObject<ItemModInk> ink = mod("ink", ItemModInk::new);
    public static final RegistryObject<ItemModHealth> heart_piece = mod("heart_piece", p -> new ItemModHealth(5F, false, p));
    public static final RegistryObject<ItemModHealth> heart_container = mod("heart_container", p -> new ItemModHealth(20F, false, p));
    public static final RegistryObject<ItemModHealth> heart_booster = mod("heart_booster", p -> new ItemModHealth(40F, false, p));
    public static final RegistryObject<ItemModHealth> heart_fab = mod("heart_fab", p -> new ItemModHealth(60F, false, p));
    public static final RegistryObject<ItemModHealth> black_diamond = mod("black_diamond", p -> new ItemModHealth(40F, true, p));
    public static final RegistryObject<ItemModWD40> wd40 = mod("wd40", ItemModWD40::new);
    public static final RegistryObject<ItemModRevive> scrumpy = revive("scrumpy", 1, false);
    public static final RegistryObject<ItemModRevive> wild_p = revive("wild_p", 3, true);
    public static final RegistryObject<ItemModShackles> shackles = mod("shackles", ItemModShackles::new);
    public static final RegistryObject<ItemModAuto> injector_5htp = mod("injector_5htp", ItemModAuto::new);
    public static final RegistryObject<ItemModKnife> injector_knife = mod("injector_knife", ItemModKnife::new);
    public static final RegistryObject<ItemModMedal> medal_liquidator = mod("medal_liquidator", ItemModMedal::new);
    public static final RegistryObject<ItemModCharm> protection_charm = mod("protection_charm", p -> new ItemModCharm(false, p));
    public static final RegistryObject<ItemModCharm> meteor_charm = mod("meteor_charm", p -> new ItemModCharm(true, p));
    public static final RegistryObject<ItemModLens> neutrino_lens = mod("neutrino_lens", ItemModLens::new);
    public static final RegistryObject<ItemModSensor> gas_tester = mod("gas_tester", ItemModSensor::new);
    public static final RegistryObject<ItemModTwoKick> ballistic_gauntlet = mod("ballistic_gauntlet", ItemModTwoKick::new);
    public static final RegistryObject<ItemModNightVision> night_vision = mod("night_vision", ItemModNightVision::new);
    /** Upstream's ItemModCard cleared its creative tab: the cards are loot-only. */
    public static final RegistryObject<ItemModCard> card_aos = REG.item("card_aos", p -> new ItemModCard(false, p), p -> p.stacksTo(1))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemModCard> card_qos = REG.item("card_qos", p -> new ItemModCard(true, p), p -> p.stacksTo(1))
            .transform(Presets.sprite());
    public static final RegistryObject<ItemModShield> australium_iii = mod("australium_iii", p -> new ItemModShield(25F, p));

    // ---- armor ----
    public static final RegistryObject<MaskOfInfamy> mask_of_infamy = armor("mask_of_infamy",
            p -> new MaskOfInfamy(Materials.INFAMY, p));

    public static final RegistryObject<ArmorHazmatMask> hazmat_paa_helmet = armor("hazmat_paa_helmet",
            p -> new ArmorHazmatMask(Materials.HAZMAT_PAA, ArmorSlot.HELMET, p));
    public static final RegistryObject<ArmorHazmat> hazmat_paa_plate = armor("hazmat_paa_plate",
            p -> new ArmorHazmat(Materials.HAZMAT_PAA, ArmorSlot.CHESTPLATE, p));
    public static final RegistryObject<ArmorHazmat> hazmat_paa_legs = armor("hazmat_paa_legs",
            p -> new ArmorHazmat(Materials.HAZMAT_PAA, ArmorSlot.LEGGINGS, p));
    public static final RegistryObject<ArmorHazmat> hazmat_paa_boots = armor("hazmat_paa_boots",
            p -> new ArmorHazmat(Materials.HAZMAT_PAA, ArmorSlot.BOOTS, p));

    public static final RegistryObject<ArmorLiquidatorMask> liquidator_helmet = armor("liquidator_helmet",
            p -> new ArmorLiquidatorMask(Materials.LIQUIDATOR, ArmorSlot.HELMET, p));
    public static final RegistryObject<ArmorLiquidator> liquidator_plate = armor("liquidator_plate",
            p -> new ArmorLiquidator(Materials.LIQUIDATOR, ArmorSlot.CHESTPLATE, p));
    public static final RegistryObject<ArmorLiquidator> liquidator_legs = armor("liquidator_legs",
            p -> new ArmorLiquidator(Materials.LIQUIDATOR, ArmorSlot.LEGGINGS, p));
    public static final RegistryObject<ArmorLiquidator> liquidator_boots = armor("liquidator_boots",
            p -> new ArmorLiquidator(Materials.LIQUIDATOR, ArmorSlot.BOOTS, p));

    private static <T extends Item> RegistryObject<T> mod(String name, Function<Item.Properties, T> factory) {
        RegistryObject<T> ro = REG.item(name, factory, p -> p.stacksTo(1)).transform(Presets.sprite());
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static RegistryObject<ItemModRevive> revive(String name, int durability, boolean whiskey) {
        RegistryObject<ItemModRevive> ro = REG.item(name, p -> new ItemModRevive(durability, whiskey, p), p -> p)
                .transform(Presets.sprite());
        ModTabs.addTo(HbmTab.CONSUMABLES, ro);
        return ro;
    }

    private static <T extends Item> RegistryObject<T> armor(String name, Function<Item.Properties, T> factory) {
        RegistryObject<T> ro = REG.item(name, factory, p -> p.stacksTo(1)).transform(Presets.sprite());
        // 1.7 ItemArmor sat in the combat tab, which the port does not have; ModArmorItems uses CONTROL.
        ModTabs.addTo(HbmTab.CONTROL, ro);
        return ro;
    }

    /**
     * Post-registration configuration, the 1.7 {@code ModItems} builder chains plus the {@code ArmorUtil},
     * {@code ArmorRegistry} and {@code HazmatRegistry} entries. Called from {@code Hazards.init} with the
     * other armor configuration.
     */
    public static void configure() {

        ArmorHazmatMask paa = hazmat_paa_helmet.get();
        paa.setOverlay(dev.golem.core.Ids.of(Constants.MOD_ID, "textures/misc/overlay_hazmat.png"));
        hazmat_paa_plate.get().cloneStats(paa);
        hazmat_paa_legs.get().cloneStats(paa);
        hazmat_paa_boots.get().cloneStats(paa);

        ArmorLiquidatorMask liquidator = liquidator_helmet.get();
        liquidator.setStep(ModSounds.METAL_STEP).setJump(ModSounds.IRON_JUMP).setFall(ModSounds.IRON_LAND);
        liquidator.setOverlay(dev.golem.core.Ids.of(Constants.MOD_ID, "textures/misc/overlay_dark.png"));
        liquidator_plate.get().cloneStats(liquidator);
        liquidator_legs.get().cloneStats(liquidator);
        liquidator_boots.get().cloneStats(liquidator);
    }

    public static void init() {
        ArmorModEvents.init();
    }
}
