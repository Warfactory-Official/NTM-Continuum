package com.hbm.items;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.JukeboxSongs;
import dev.golem.core.dejson.Models;

import static dev.golem.core.dejson.Crafting.compress;
import static dev.golem.core.dejson.Crafting.decompress;
import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.smelt;

/**
 * Dejson declarations for {@link ModFinalConsumableItems} (whose item models are stated on its registrations,
 * Reg): the four jukebox songs, and every crafting and smelting recipe of 1.7 {@code CraftingManager}, {@code ConsumableRecipes},
 * {@code ToolRecipes}, {@code PowderRecipes}, {@code ArmorRecipes}, {@code WeaponRecipes},
 * {@code MineralRecipes} and {@code SmeltingRecipes} that makes or uses these items and has all its parts in
 * the port. Touches no Minecraft class.
 *
 * <p>{@code chocolate_milk}, {@code bdcl}, {@code gun_kit_1}, {@code gun_kit_2} (filled fluid containers,
 * {@code Fluids.X.getDict(1000)}) and {@code mucho_mango} (the water bottle, {@code Items.potionitem} meta 0)
 * are declared in {@link com.hbm.inventory.recipes.ModContainerRecipes}, whose component shapeless recipe
 * can say those ingredients. Not declared here: the plushies and {@code radiobox} have no ported output.
 */
public final class FinalConsumableDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private FinalConsumableDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        songs();
        recipes();
    }

    /** 1.7 ItemModRecord.getRecordResource; the descriptions are 1.7's item.record.X.desc lines. */
    private static void songs() {
        JukeboxSongs.declare(NS, "lc", "hbm:music.record_lambda_core", "item.hbm.record_lc.desc", 104.54F, 15);
        JukeboxSongs.declare(NS, "ss", "hbm:music.record_sector_sweep", "item.hbm.record_ss.desc", 166.50F, 15);
        JukeboxSongs.declare(NS, "vc", "hbm:music.record_vortal_combat", "item.hbm.record_vc.desc", 194.72F, 15);
        JukeboxSongs.declare(NS, "glass", "hbm:music.transmission", "item.hbm.record_glass.desc", 62.23F, 15);
        JukeboxSongs.discs("hbm:record_lc", "hbm:record_ss", "hbm:record_vc", "hbm:record_glass");
    }

    private static void recipes() {
        // --- 1.7 ConsumableRecipes ---
        shaped(NS, "bomb_waffle", "hbm:bomb_waffle", rows("WEW", "MPM", "WEW"),
                "W", "minecraft:wheat", "E", "minecraft:egg", "M", "minecraft:milk_bucket", "P", "hbm:man_core");
        shaped(NS, "schnitzel_vegan", "hbm:schnitzel_vegan", 3, rows("RWR", "WPW", "RWR"),
                "W", "hbm:nuclear_waste", "R", "minecraft:sugar_cane", "P", "minecraft:pumpkin_seeds");
        shaped(NS, "cotton_candy", "hbm:cotton_candy", 2, rows(" S ", "SPS", " H "),
                "P", "#c:nuggets/pu239", "S", "minecraft:sugar", "H", "minecraft:stick");
        apples("apple_schrabidium", "schrabidium");
        apples("apple_lead", "lead");
        shaped(NS, "apple_euphemium", "hbm:apple_euphemium", rows("EEE", "EAE", "EEE"),
                "E", "#c:nuggets/euphemium", "A", "minecraft:apple");
        shapeless(NS, "tem_flakes", "hbm:tem_flakes", "#c:nuggets/gold", "minecraft:paper");
        shapeless(NS, "tem_flakes_normal", "hbm:tem_flakes_normal",
                "#c:nuggets/gold", "#c:nuggets/gold", "#c:nuggets/gold", "minecraft:paper");
        shapeless(NS, "tem_flakes_expensive", "hbm:tem_flakes_expensive",
                "#c:ingots/gold", "#c:ingots/gold", "#c:nuggets/gold", "#c:nuggets/gold", "minecraft:paper");
        shapeless(NS, "glowing_stew", "hbm:glowing_stew", "minecraft:bowl", "hbm:mush", "hbm:mush");
        shapeless(NS, "balefire_scrambled", "hbm:balefire_scrambled", "minecraft:bowl", "hbm:egg_balefire");
        shapeless(NS, "balefire_and_ham", "hbm:balefire_and_ham", "hbm:balefire_scrambled", "minecraft:cooked_beef");
        shapeless(NS, "pancake", "hbm:pancake", "#c:dusts/redstone", "#c:dusts/diamond", "minecraft:wheat",
                "#c:bolts/steel", "#c:wires/copper", "#c:plates/steel");
        shapeless(NS, "pancake_1", "hbm:pancake", "#c:dusts/redstone", "#c:dusts/emerald", "minecraft:wheat",
                "#c:bolts/steel", "#c:wires/copper", "#c:plates/steel");
        // marshmallow meta 1 is the roasted marshmallow, dye meta 3 cocoa beans
        shapeless(NS, "ingot_smore", "hbm:ingot_smore", "minecraft:wheat", "hbm:marshmallow_roasted",
                "minecraft:cocoa_beans");
        // 1.7 really does register canned recursion as crafting into itself
        shapeless(NS, "canned_conserve_recursion", "hbm:canned_conserve_recursion", "hbm:canned_conserve_recursion");
        shaped(NS, "peas", "hbm:peas", rows(" S ", "SNS", " S "), "S", "minecraft:wheat_seeds", "N", "#c:nuggets/gold");
        shaped(NS, "canteen_vodka", "hbm:canteen_vodka", rows("O", "P"), "O", "minecraft:potato", "P", "#c:plates/steel");
        shapeless(NS, "five_htp", "hbm:five_htp", "#c:dusts/coal", "#c:dusts/euphemium", "hbm:canteen_vodka");
        shapeless(NS, "cigarette", "hbm:cigarette", 16, "#c:ingots/asbestos", "#c:tars", "#c:nuggets/polonium",
                "hbm:plant_item_tobacco");
        shapeless(NS, "crackpipe", "hbm:crackpipe", "hbm:catalytic_converter");

        // --- 1.7 PowderRecipes / SmeltingRecipes / MineralRecipes ---
        shapeless(NS, "powder_fertilizer", "hbm:powder_fertilizer", 4,
                "#c:dusts/calcium", "#c:dusts/red_phosphorus", "#c:dusts/niter", "#c:dusts/sulfur");
        shapeless(NS, "powder_fertilizer_1", "hbm:powder_fertilizer", 4,
                "#c:dusts/ash", "#c:dusts/red_phosphorus", "#c:dusts/niter", "#c:dusts/sulfur");
        smelt(NS, "coke_coal_from_briquette", "hbm:briquette_coal", "hbm:coke_coal", 1.0);
        smelt(NS, "coke_lignite_from_briquette", "hbm:briquette_lignite", "hbm:coke_lignite", 1.0);
        smelt(NS, "charcoal_from_briquette", "hbm:briquette_wood", "minecraft:charcoal", 1.0);
        smelt(NS, "rag_from_rag_damp", "hbm:rag_damp", "hbm:rag", 0.1);
        smelt(NS, "rag_from_rag_piss", "hbm:rag_piss", "hbm:rag", 0.1);
        compress(NS, "block_smore", "hbm:ingot_smore", "hbm:block_smore");
        decompress(NS, "ingot_smore_from_block", "hbm:block_smore", "hbm:ingot_smore");

        // --- 1.7 ToolRecipes ---
        shaped(NS, "matchstick", "hbm:matchstick", 16, rows("I", "S"), "I", "#c:dusts/sulfur", "S", "minecraft:stick");
        shaped(NS, "matchstick_1", "hbm:matchstick", 24, rows("I", "S"), "I", "#c:dusts/red_phosphorus",
                "S", "minecraft:stick");
        shaped(NS, "chemistry_set", "hbm:chemistry_set", rows("GIG", "GCG"),
                "G", "#c:glass_blocks", "I", "#c:ingots/iron", "C", "#c:ingots/copper");
        shaped(NS, "chemistry_set_boron", "hbm:chemistry_set_boron", rows("GIG", "GCG"),
                "G", "hbm:glass_boron", "I", "#c:ingots/steel", "C", "#c:ingots/cobalt");

        // --- 1.7 ArmorRecipes (the rag robes; the RPA set on tier-2 legendary parts) ---
        shaped(NS, "robes_helmet", "hbm:robes_helmet", rows("EEE", "E E"), "E", "hbm:rag");
        shaped(NS, "robes_plate", "hbm:robes_plate", rows("E E", "EEE", "EEE"), "E", "hbm:rag");
        shaped(NS, "robes_legs", "hbm:robes_legs", rows("EEE", "E E", "E E"), "E", "hbm:rag");
        shaped(NS, "robes_boots", "hbm:robes_boots", rows("R R", "P P"), "R", "hbm:rag", "P", "#c:ingots/any_rubber");
        String l = "hbm:parts_legendary_tier2";
        shaped(NS, "rpa_helmet", "hbm:rpa_helmet", rows("KPK", "PLP", " F "),
                "L", l, "K", "hbm:plate_kevlar", "P", "hbm:plate_armor_ajr", "F", "hbm:gas_mask_filter_combo");
        shaped(NS, "rpa_plate", "hbm:rpa_plate", rows("P P", "MLM", "PKP"),
                "L", l, "K", "hbm:plate_kevlar", "P", "hbm:plate_armor_ajr", "M", "hbm:motor_desh");
        shaped(NS, "rpa_legs", "hbm:rpa_legs", rows("MPM", "KLK", "P P"),
                "L", l, "K", "hbm:plate_kevlar", "P", "hbm:plate_armor_ajr", "M", "hbm:motor_desh");
        shaped(NS, "rpa_boots", "hbm:rpa_boots", rows("KLK", "P P"),
                "L", l, "K", "hbm:plate_kevlar", "P", "hbm:plate_armor_ajr");

        // --- 1.7 WeaponRecipes ---
        shapeless(NS, "gun_double_barrel_sacred_dragon", "hbm:gun_double_barrel_sacred_dragon",
                "hbm:gun_double_barrel", "hbm:item_secret_selenium_steel");

        // --- 1.7 CraftingManager ---
        shaped(NS, "upgrade_muffler", "hbm:upgrade_muffler", 16, rows("III", "IWI", "III"),
                "I", "#c:ingots/any_rubber", "W", "#minecraft:wool");
        shaped(NS, "laser_crystal_dnt", "hbm:laser_crystal_dnt", rows("QDQ", "SBS", "QDQ"),
                "Q", "hbm:glass_quartz", "D", "#c:ingots/dineutronium", "B", "hbm:egg_balefire",
                "S", "hbm:powder_spark_mix");
        shaped(NS, "laser_crystal_digamma", "hbm:laser_crystal_digamma", rows("QUQ", "UEU", "QUQ"),
                "Q", "hbm:glass_quartz", "U", "hbm:undefined", "E", "hbm:ingot_electronium");
        // a crafted potato battery is ItemBattery.getFullBattery: an absent charge reads as full
        shapeless(NS, "battery_potato", "hbm:battery_potato", "minecraft:potato", "#c:wires/aluminium",
                "#c:wires/copper");
        shapeless(NS, "battery_potatos", "hbm:battery_potatos", "hbm:battery_potato", "hbm:turret_chip",
                "#c:dusts/redstone");
        shaped(NS, "record_lc", "hbm:record_lc", rows(" S ", "SDS", " S "),
                "S", "#c:ingots/any_plastic", "D", "#c:dusts/lapis");
        shaped(NS, "record_ss", "hbm:record_ss", rows(" S ", "SDS", " S "),
                "S", "#c:ingots/any_plastic", "D", "#c:dusts/red_copper");
        shaped(NS, "record_vc", "hbm:record_vc", rows(" S ", "SDS", " S "),
                "S", "#c:ingots/any_plastic", "D", "hbm:powder_combine_steel");
        shaped(NS, "rag", "hbm:rag", 4, rows("SW", "WS"), "S", "minecraft:string", "W", "#minecraft:wool");
        shapeless(NS, "parts_legendary_tier1", "hbm:parts_legendary_tier1",
                "hbm:ingot_chainsteel", "#c:ingots/asbestos", "hbm:gem_alexandrite");
        shapeless(NS, "parts_legendary_tier1_from_tier2", "hbm:parts_legendary_tier1", 3, "hbm:parts_legendary_tier2");
        shapeless(NS, "parts_legendary_tier2", "hbm:parts_legendary_tier2",
                "hbm:ingot_chainsteel", "hbm:ingot_bismuth", "hbm:gem_alexandrite", "hbm:gem_alexandrite");
        shapeless(NS, "parts_legendary_tier2_from_tier3", "hbm:parts_legendary_tier2", 3, "hbm:parts_legendary_tier3");
        shapeless(NS, "parts_legendary_tier3", "hbm:parts_legendary_tier3", "hbm:ingot_chainsteel",
                "hbm:ingot_smore", "hbm:gem_alexandrite", "hbm:gem_alexandrite", "hbm:gem_alexandrite");
        shapeless(NS, "upgrade_5g", "hbm:upgrade_5g", "hbm:upgrade_template", "hbm:gem_alexandrite");
        shapeless(NS, "book_of_", "hbm:book_of_", "hbm:page_of_page1", "hbm:page_of_page2", "hbm:page_of_page3",
                "hbm:page_of_page4", "hbm:page_of_page5", "hbm:page_of_page6", "hbm:page_of_page7",
                "hbm:page_of_page8", "hbm:egg_balefire");

        // 1.7 CraftingManager:1004-1043, the StarControl circuit from its pieces
        String p = "hbm:circuit_star_piece_";
        String c = "hbm:circuit_star_component_";
        shapeless(NS, "circuit_star_component_chipset", c + "chipset", p + "bridge_bios", p + "bridge_bus",
                p + "bridge_chipset", p + "bridge_cmos", p + "bridge_io", p + "bridge_north", p + "bridge_south");
        shapeless(NS, "circuit_star_component_cpu", c + "cpu", p + "cpu_cache", p + "cpu_clock", p + "cpu_ext",
                p + "cpu_logic", p + "cpu_register", p + "cpu_socket");
        shapeless(NS, "circuit_star_component_ram", c + "ram", p + "mem_socket", p + "mem_16k_a", p + "mem_16k_b",
                p + "mem_16k_c", p + "mem_16k_d");
        shapeless(NS, "circuit_star_component_card", c + "card", p + "card_board", p + "card_processor");
        shapeless(NS, "circuit_star", "hbm:circuit_star", c + "chipset", c + "cpu", c + "ram", c + "card",
                p + "board_transistor", p + "board_converter", p + "board_blank");
    }

    /** 1.7 ConsumableRecipes:42-47: an apple ringed by nuggets, ingots or blocks gives meta 0, 1 or 2. */
    private static void apples(String id, String material) {
        shaped(NS, id, "hbm:" + id, rows("SSS", "SAS", "SSS"), "S", "#c:nuggets/" + material, "A", "minecraft:apple");
        shaped(NS, id + "_ingot", "hbm:" + id + "_ingot", rows("SSS", "SAS", "SSS"),
                "S", "#c:ingots/" + material, "A", "minecraft:apple");
        shaped(NS, id + "_block", "hbm:" + id + "_block", rows("SSS", "SAS", "SSS"),
                "S", "#c:storage_blocks/" + material, "A", "minecraft:apple");
    }
}
