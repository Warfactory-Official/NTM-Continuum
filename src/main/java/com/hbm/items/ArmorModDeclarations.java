package com.hbm.items;

import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Tags;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;

/**
 * Dejson declarations for {@link ModArmorModItems} and {@code insert_era}: the new repair tags and the recipes
 * of 1.7 {@code ConsumableRecipes} and {@code ArmorRecipes}. Touches no Minecraft class. The sprite item models
 * are stated on the registrations (Reg).
 */
public final class ArmorModDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private ArmorModDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // aMatLiquidator.customCraftingMaterial = plate_lead; the mask's material is vanilla iron.
        Tags.item(NS, "repairs/liquidator", ItemIds.plate_lead);
        Tags.item(NS, "repairs/infamy", VanillaIds.iron_ingot);
        // 1.7 red_flower with a wildcard meta: every small flower except the dandelion.
        Tags.item(NS, "red_flowers", VanillaIds.poppy, VanillaIds.blue_orchid, VanillaIds.allium,
                VanillaIds.azure_bluet, VanillaIds.red_tulip, VanillaIds.orange_tulip, VanillaIds.white_tulip,
                VanillaIds.pink_tulip, VanillaIds.oxeye_daisy);

        consumables();
        armor();
    }

    /** 1.7 {@code ConsumableRecipes}, in source order. */
    private static void consumables() {
        final String tape = ItemIds.ducttape;
        shaped(NS, "cladding_obsidian", "hbm:cladding_obsidian", rows("OOO", "PDP", "OOO"),
                "O", VanillaIds.obsidian, "P", TagIds.plates_steel, "D", tape);
        shaped(NS, "cladding_iron", "hbm:cladding_iron", rows("OOO", "PDP", "OOO"),
                "O", TagIds.plates_iron, "P", ItemIds.plate_polymer, "D", tape);
        shaped(NS, "insert_era", "hbm:insert_era", rows("DPD", "PSP", "DPD"),
                "D", tape, "P", TagIds.plates_iron, "S", ItemIds.ingot_semtex);
        shaped(NS, "australium_iii", "hbm:australium_iii", rows("WSW", "PAP", "SPS"),
                "S", TagIds.plates_welded_steel, "P", TagIds.ingots_any_plastic, "A", TagIds.ingots_australium,
                "W", TagIds.wires_dense_gold);
        shaped(NS, "servo_set", "hbm:servo_set", rows("MBM", "PBP", "MBM"),
                "M", ItemIds.motor, "B", TagIds.bolts_steel, "P", TagIds.plates_iron);
        shaped(NS, "servo_set_desh", "hbm:servo_set_desh", rows("MBM", "PSP", "MBM"),
                "M", ItemIds.motor_desh, "B", TagIds.bolts_dura_steel, "P", ItemIds.plate_desh, "S", "hbm:servo_set");
        shaped(NS, "attachment_mask", "hbm:attachment_mask", rows("DID", "IGI", " F "),
                "D", tape, "I", TagIds.ingots_any_rubber, "G", TagIds.glass_panes, "F", TagIds.plates_iron);
        shaped(NS, "attachment_mask_mono", "hbm:attachment_mask_mono", rows(" D ", "DID", " F "),
                "D", tape, "I", TagIds.ingots_any_rubber, "F", TagIds.plates_iron);
        shaped(NS, "pads_rubber", "hbm:pads_rubber", rows("P P", "IDI", "P P"),
                "P", TagIds.ingots_any_rubber, "I", TagIds.plates_iron, "D", tape);
        shaped(NS, "pads_slime", "hbm:pads_slime", rows("SPS", "DSD", "SPS"),
                "S", TagIds.slime_balls, "P", "hbm:pads_rubber", "D", tape);
        shaped(NS, "pads_static", "hbm:pads_static", rows("CDC", "ISI", "CDC"),
                "C", TagIds.ingots_copper, "D", tape, "I", TagIds.ingots_any_rubber, "S", "hbm:pads_slime");
        shaped(NS, "horseshoe_magnet", "hbm:horseshoe_magnet", rows("L L", "I I", "ILI"),
                "L", "hbm:lodestone", "I", TagIds.ingots_iron);
        shaped(NS, "industrial_magnet", "hbm:industrial_magnet", rows("SMS", " B ", "SMS"),
                "S", TagIds.ingots_steel, "M", "hbm:horseshoe_magnet", "B", TagIds.wires_dense_red_copper);
        shaped(NS, "heart_container", "hbm:heart_container", rows("HAH", "ACA", "HAH"),
                "H", "hbm:heart_piece", "A", TagIds.ingots_aluminium, "C", ItemIds.coin_creeper);
        shaped(NS, "heart_booster", "hbm:heart_booster", rows("GHG", "MCM", "GHG"),
                "G", TagIds.ingots_gold, "H", "hbm:heart_container", "M", "hbm:morning_glory", "C", ItemIds.coin_maskman);
        shaped(NS, "heart_fab", "hbm:heart_fab", rows("GHG", "MCM", "GHG"),
                "G", TagIds.billets_polonium, "H", "hbm:heart_booster", "M", TagIds.cokes, "C", ItemIds.coin_worm);
        shaped(NS, "ink", "hbm:ink", rows("FPF", "PIP", "FPF"),
                "F", "#hbm:red_flowers", "P", ItemIds.armor_polish, "I", TagIds.dyes_black);
        shaped(NS, "bathwater_mk2", "hbm:bathwater_mk2", rows("MWM", "WBW", "MWM"),
                "M", ItemIds.bottle_mercury, "W", ItemIds.nuclear_waste, "B", "hbm:bathwater");
        shaped(NS, "back_tesla", "hbm:back_tesla", rows("DGD", "GTG", "DGD"),
                "D", tape, "G", TagIds.wires_gold, "T", ItemIds.tesla);
        shaped(NS, "medal_liquidator", "hbm:medal_liquidator", rows("GBG", "BFB", "GBG"),
                "G", TagIds.nuggets_au198, "B", TagIds.ingots_boron, "F", ItemIds.debris_fuel);
        shapeless(NS, "injector_5htp", "hbm:injector_5htp",
                ItemIds.five_htp, ItemIds.circuit_basic, TagIds.plates_saturnite);
        shapeless(NS, "injector_knife", "hbm:injector_knife", "hbm:injector_5htp", VanillaIds.iron_sword);
        shaped(NS, "shackles", "hbm:shackles", rows("CIC", "C C", "I I"),
                "I", ItemIds.ingot_chainsteel, "C", "hbm:dungeon_chain");
        shaped(NS, "black_diamond", "hbm:black_diamond", rows("NIN", "IGI", "NIN"),
                "N", TagIds.nuggets_au198, "I", "hbm:ink", "G", TagIds.gems_volcanic);
        shaped(NS, "protection_charm", "hbm:protection_charm", rows(" M ", "MDM", " M "),
                "M", ItemIds.fragment_meteorite, "D", TagIds.gems_diamond);
        shaped(NS, "meteor_charm", "hbm:meteor_charm", rows(" M ", "MDM", " M "),
                "M", ItemIds.fragment_meteorite, "D", TagIds.gems_volcanic);
        shaped(NS, "neutrino_lens", "hbm:neutrino_lens", rows("PSP", "SCS", "PSP"),
                "P", TagIds.ingots_any_plastic, "S", TagIds.ingots_starmetal, "C", ItemIds.circuit_bismoid);
        shaped(NS, "gas_tester", "hbm:gas_tester", rows("G", "C", "I"),
                "G", TagIds.plates_gold, "C", ItemIds.circuit_vacuum_tube, "I", TagIds.plates_iron);
        shaped(NS, "ballistic_gauntlet", "hbm:ballistic_gauntlet", rows(" WS", "WRS", " RS"),
                "W", TagIds.wires_copper, "R", ItemIds.ring_starmetal, "S", TagIds.plates_steel);
        shaped(NS, "night_vision", "hbm:night_vision", rows("P P", "GCG"),
                "P", TagIds.ingots_any_plastic, "G", TagIds.glass_blocks, "C", ItemIds.circuit_basic);
    }

    /** 1.7 {@code ArmorRecipes}. */
    private static void armor() {
        shaped(NS, "hazmat_paa_helmet", "hbm:hazmat_paa_helmet", rows("EEE", "IEI", " P "),
                "E", ItemIds.plate_paa, "I", TagIds.glass_panes, "P", TagIds.plates_iron);
        shaped(NS, "hazmat_paa_plate", "hbm:hazmat_paa_plate", rows("E E", "EEE", "EEE"), "E", ItemIds.plate_paa);
        shaped(NS, "hazmat_paa_legs", "hbm:hazmat_paa_legs", rows("EEE", "E E", "E E"), "E", ItemIds.plate_paa);
        shaped(NS, "hazmat_paa_boots", "hbm:hazmat_paa_boots", rows("E E", "E E"), "E", ItemIds.plate_paa);

        shaped(NS, "liquidator_helmet", "hbm:liquidator_helmet", rows("III", "CBC", "III"),
                "I", TagIds.ingots_any_rubber, "C", ItemIds.cladding_lead, "B", ItemIds.hazmat_helmet_grey);
        shaped(NS, "liquidator_plate", "hbm:liquidator_plate", rows("ICI", "TBT", "ICI"),
                "I", TagIds.ingots_any_rubber, "C", ItemIds.cladding_lead, "B", ItemIds.hazmat_plate_grey,
                "T", ItemIds.gas_empty);
        shaped(NS, "liquidator_legs", "hbm:liquidator_legs", rows("III", "CBC", "I I"),
                "I", TagIds.ingots_any_rubber, "C", ItemIds.cladding_lead, "B", ItemIds.hazmat_legs_grey);
        shaped(NS, "liquidator_boots", "hbm:liquidator_boots", rows("ICI", "IBI"),
                "I", TagIds.ingots_any_rubber, "C", ItemIds.cladding_lead, "B", ItemIds.hazmat_boots_grey);

        shaped(NS, "mask_of_infamy", "hbm:mask_of_infamy", rows("III", "III", " I "), "I", TagIds.plates_iron);
    }
}
