package com.hbm.lib;

import dev.golem.core.dejson.Tags;

/**
 * hbm's own gameplay tags and its additions to vanilla's, declared through Golem Core's {@code dejson}
 * vocabulary instead of written as JSON. Touches no Minecraft class.
 *
 * <p>Material ({@code c:}) tags are {@link com.hbm.inventory.material.ModMaterialTags}; tool-mining tags are
 * stated on each block's registration ({@code .tag(BlockTag.X)}, {@link com.hbm.registration.Reg}).
 *
 * <p>Block lists that name other machines or another mod's blocks use {@link Tags.Tag#optional} members, so a
 * block that is not ported yet leaves the tag loadable instead of failing it. The armour repair tags of sets
 * whose 1.7 repair material is not ported ({@code cobalt}, {@code starmetal}, {@code t51}) are declared empty:
 * the armour material names the tag, so it has to exist.
 */
public final class ModTagDeclarations {

    private static boolean declared;

    private ModTagDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        lists();
        repairs();
    }

    private static void lists() {
        Tags.of("block", "hbm", "fbi_breakable")
                .values("#minecraft:wooden_doors", "minecraft:iron_door", "#minecraft:wooden_trapdoors", "minecraft:chest",
                        "minecraft:trapped_chest")
                .optional("hbm:machine_press", "hbm:machine_chemical_plant", "hbm:machine_chemical_factory",
                        "hbm:machine_purex", "hbm:machine_crystallizer", "hbm:machine_cyclotron", "hbm:machine_epress",
                        "hbm:machine_diesel", "hbm:machine_turbine", "hbm:machine_industrial_turbine", "hbm:machine_chungus",
                        "hbm:machine_rtg_grey", "hbm:crate_iron", "hbm:crate_steel")
                .register();
        Tags.of("block", "hbm", "fleija_immune")
                .register();
        Tags.of("block", "hbm", "meteorite_replaceable")
                .optional("hbm:block_meteor_molten", "hbm:ore_meteor_iron", "hbm:ore_meteor_copper",
                        "hbm:ore_meteor_aluminium", "hbm:ore_meteor_rareearth", "hbm:ore_meteor_cobalt", "hbm:block_meteor",
                        "hbm:block_meteor_cobble", "hbm:block_meteor_broken", "hbm:block_meteor_treasure")
                .register();
        Tags.block("hbm", "shatters_when_shot",
                "#minecraft:impermeable", "minecraft:glass_pane", "minecraft:white_stained_glass_pane",
                "minecraft:orange_stained_glass_pane", "minecraft:magenta_stained_glass_pane",
                "minecraft:light_blue_stained_glass_pane", "minecraft:yellow_stained_glass_pane",
                "minecraft:lime_stained_glass_pane", "minecraft:pink_stained_glass_pane",
                "minecraft:gray_stained_glass_pane", "minecraft:light_gray_stained_glass_pane",
                "minecraft:cyan_stained_glass_pane", "minecraft:purple_stained_glass_pane",
                "minecraft:blue_stained_glass_pane", "minecraft:brown_stained_glass_pane",
                "minecraft:green_stained_glass_pane", "minecraft:red_stained_glass_pane",
                "minecraft:black_stained_glass_pane");
        Tags.block("hbm", "uranium_ores",
                "#c:ores/uranium");
        Tags.entityType("hbm", "bosses",
                "hbm:entity_elder_one", "hbm:entity_mob_mask_man", "hbm:entity_ufo", "hbm:entity_balls_o_tron",
                "hbm:entity_mob_hunter_chopper");
        // every ScrapType of 1.7 scrap_plastic (the wildcard smelting input); literal ids, because this class runs in
        // DejsonMain's plain JVM, where the item classes must not initialise
        Tags.item("hbm", "scrap_plastic",
                "hbm:scrap_plastic_board_blank", "hbm:scrap_plastic_board_transistor",
                "hbm:scrap_plastic_board_converter", "hbm:scrap_plastic_bridge_north",
                "hbm:scrap_plastic_bridge_south", "hbm:scrap_plastic_bridge_io", "hbm:scrap_plastic_bridge_bus",
                "hbm:scrap_plastic_bridge_chipset", "hbm:scrap_plastic_bridge_cmos", "hbm:scrap_plastic_bridge_bios",
                "hbm:scrap_plastic_cpu_register", "hbm:scrap_plastic_cpu_clock", "hbm:scrap_plastic_cpu_logic",
                "hbm:scrap_plastic_cpu_cache", "hbm:scrap_plastic_cpu_ext", "hbm:scrap_plastic_cpu_socket",
                "hbm:scrap_plastic_mem_socket", "hbm:scrap_plastic_mem_16k_a", "hbm:scrap_plastic_mem_16k_b",
                "hbm:scrap_plastic_mem_16k_c", "hbm:scrap_plastic_mem_16k_d", "hbm:scrap_plastic_card_board",
                "hbm:scrap_plastic_card_processor");
        // 1.7 Items.fish with the wildcard meta: raw cod, salmon, clownfish and pufferfish, not the cooked fish
        // (Mixer FISHOIL, Liquefaction FISHOIL). Vanilla's fishes tag also holds the cooked ones.
        Tags.item("hbm", "raw_fishes",
                "minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish", "minecraft:pufferfish");
        Tags.item("hbm", "zirnox_rods",
                "hbm:rod_zirnox_natural_uranium_fuel", "hbm:rod_zirnox_uranium_fuel", "hbm:rod_zirnox_th232_fuel",
                "hbm:rod_zirnox_thorium_fuel", "hbm:rod_zirnox_mox_fuel", "hbm:rod_zirnox_plutonium_fuel",
                "hbm:rod_zirnox_u233_fuel", "hbm:rod_zirnox_u235_fuel", "hbm:rod_zirnox_les_fuel",
                "hbm:rod_zirnox_lithium_fuel", "hbm:rod_zirnox_zfb_mox_fuel");
        Tags.item("hbm", "zirnox_depleted_rods",
                "hbm:rod_zirnox_depleted_natural_uranium_fuel", "hbm:rod_zirnox_depleted_uranium_fuel",
                "hbm:rod_zirnox_depleted_thorium_fuel", "hbm:rod_zirnox_depleted_mox_fuel",
                "hbm:rod_zirnox_depleted_plutonium_fuel", "hbm:rod_zirnox_depleted_u233_fuel",
                "hbm:rod_zirnox_depleted_u235_fuel", "hbm:rod_zirnox_depleted_les_fuel",
                "hbm:rod_zirnox_depleted_zfb_mox_fuel");
        Tags.block("minecraft", "beacon_base_blocks",
                "hbm:block_actinium", "hbm:block_bakelite", "hbm:block_bismuth", "hbm:block_cadmium",
                "hbm:block_coltan", "hbm:block_lanthanium", "hbm:block_niobium", "hbm:block_polymer",
                "hbm:block_rubber", "hbm:block_tantalium", "hbm:block_zirconium", "hbm:block_tcalloy",
                "hbm:block_cdalloy", "hbm:block_australium", "hbm:block_lithium", "hbm:block_waste",
                "hbm:block_waste_painted", "hbm:block_waste_vitrified", "hbm:block_yellowcake");
        Tags.entityType("minecraft", "arthropod",
                "hbm:entity_glyphid", "hbm:entity_glyphid_behemoth", "hbm:entity_glyphid_blaster",
                "hbm:entity_glyphid_bombardier", "hbm:entity_glyphid_brawler", "hbm:entity_glyphid_brenda",
                "hbm:entity_glyphid_digger", "hbm:entity_glyphid_nuclear", "hbm:entity_glyphid_scout",
                "hbm:entity_parasite_maggot");
        Tags.entityType("minecraft", "undead",
                "hbm:entity_undead_soldier");
        Tags.item("minecraft", "dyes",
                "hbm:chemical_dye_black", "hbm:chemical_dye_blue", "hbm:chemical_dye_brown", "hbm:chemical_dye_cyan",
                "hbm:chemical_dye_gray", "hbm:chemical_dye_green", "hbm:chemical_dye_lightblue",
                "hbm:chemical_dye_lime", "hbm:chemical_dye_magenta", "hbm:chemical_dye_orange",
                "hbm:chemical_dye_pink", "hbm:chemical_dye_purple", "hbm:chemical_dye_red",
                "hbm:chemical_dye_silver", "hbm:chemical_dye_white", "hbm:chemical_dye_yellow", "hbm:crayon_black",
                "hbm:crayon_blue", "hbm:crayon_brown", "hbm:crayon_cyan", "hbm:crayon_gray", "hbm:crayon_green",
                "hbm:crayon_lightblue", "hbm:crayon_lime", "hbm:crayon_magenta", "hbm:crayon_orange",
                "hbm:crayon_pink", "hbm:crayon_purple", "hbm:crayon_red", "hbm:crayon_silver", "hbm:crayon_white",
                "hbm:crayon_yellow", "hbm:cinnabar", "hbm:fluorite", "hbm:oil_tar_coal", "hbm:oil_tar_crack",
                "hbm:oil_tar_crude", "hbm:oil_tar_paraffin", "hbm:oil_tar_wax", "hbm:oil_tar_wood",
                "hbm:powder_ash_coal", "hbm:powder_ash_fly", "hbm:powder_ash_fullerene", "hbm:powder_ash_misc",
                "hbm:powder_ash_soot", "hbm:powder_ash_wood", "hbm:powder_cadmium", "hbm:powder_coal",
                "hbm:powder_lapis", "hbm:powder_lignite", "hbm:powder_titanium", "hbm:sulfur");
        Tags.item("minecraft", "wolf_food",
                "hbm:ingot_semtex", "hbm:glyphid_meat", "hbm:glyphid_meat_grilled");
    }

    /** One tag per armour set, naming what repairs it on the anvil (1.7 {@code ArmorMaterial.customCraftingMaterial}). */
    private static void repairs() {
        repair("asbestos", "hbm:asbestos_cloth");
        repair("bj", "hbm:plate_armor_lunar");
        repair("cobalt");
        repair("dns", "hbm:plate_armor_dnt");
        repair("hazmat_cloth", "hbm:hazmat_cloth");
        repair("hazmat_cloth_grey", "hbm:hazmat_cloth_grey");
        repair("hazmat_cloth_red", "hbm:hazmat_cloth_red");
        repair("paa", "hbm:plate_paa");
        repair("schrabidium", "hbm:ingot_schrabidium");
        repair("security", "hbm:plate_kevlar");
        repair("starmetal");
        repair("steel", "hbm:ingot_steel");
        repair("t51");
        repair("taurun", "hbm:plate_iron");
        repair("titanium", "hbm:ingot_titanium");
    }

    private static void repair(String set, String... materials) {
        Tags.of("item", "hbm", "repairs/" + set).values(materials).register();
    }
}
