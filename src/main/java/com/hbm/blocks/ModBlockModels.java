package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;

/**
 * Every block model and blockstate hbm can state in code, declared through Golem Core's
 * {@code dejson} vocabulary instead of written as JSON.
 *
 * <p>The {@value #FILES_REPLACED} asset files these 570 declarations stood in for have been
 * deleted. Nothing here was a new decision: every declaration was derived from the files it
 * replaces, and each was held against its file and matched exactly — twice, by two separate
 * implementations — before that file was removed. {@code dejsonGenerate} emits them again at build
 * time, so what ships is byte-for-byte what shipped before.
 *
 * <p>A file still beats a declaration. {@code dejsonGenerate} skips any path that exists under
 * {@code src/main/resources}, so writing one back is how a block gets an exception, and a resource
 * pack overrides a generated file exactly as it overrode a hand-written one.
 *
 * <p>What is deliberately <em>not</em> here: blocks whose model carries hand-built {@code elements},
 * the two multipart blockstates (a cable and a fluid duct), and everything drawn from an OBJ, which
 * belongs to the instanced renderer rather than to a model file.
 */
public final class ModBlockModels {

    private static final String NS = "hbm";

    /** How many asset files the declarations below stand in for; quoted in this class's own javadoc. */
    static final int FILES_REPLACED = 1709;

    private static boolean declared;

    private ModBlockModels() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        cubes();
        slabs();
        stairs();
        renderedMachines();
        handBuilt();
        everythingElse();
    }

    /** Ladders, which are rungs and a rail: hand-built {@code elements} rotated four ways. */
    private static final String[] LADDERS = {
            "ladder_aluminium", "ladder_cobalt", "ladder_copper", "ladder_gold", "ladder_iron",
            "ladder_lead", "ladder_red", "ladder_red_top", "ladder_steel", "ladder_sturdy",
            "ladder_titanium", "ladder_tungsten",
    };

    private static final String[] SINGLE_STATE = {
            "fallout", "foundry_mold", "gas_asbestos", "gas_meltdown", "gas_monoxide",
            "gas_radon", "gas_radon_dense", "gas_radon_tomb",
    };

    /**
     * Blocks whose blockstate is regular but whose model is hand-built geometry. Only the
     * boilerplate around the model is declared; the model itself stays a file.
     */
    private static void handBuilt() {
        for (String name : LADDERS) {
            Dejson.block(NS, name).statesOnly().states(Variants.facing()).register();
        }
        for (String name : SINGLE_STATE) {
            Dejson.block(NS, name).statesOnly().states(Variants.simple()).register();
        }
        Dejson.block(NS, "fire_digamma").statesOnly("hbm:block/fire_digamma_floor")
                .states(Variants.simple()).register();
        Dejson.block(NS, "steel_beam").statesOnly()
                .states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        Dejson.block(NS, "steel_scaffold").statesOnly()
                .states(Variants.product(
                        Variants.uniform("orient", "horizontal_north_south", "horizontal_east_west",
                                "vertical_north_south", "vertical_east_west"),
                        Variants.uniform("variant", "steel", "red", "white", "yellow")))
                .register();
        Dejson.block(NS, "steel_grate").statesOnly().states(Variants.select("height", GRATE_HEIGHTS))
                .register();
    }

    /** Ten walkable heights, each its own hand-built model suffixed with the bare number. */
    private static final String[] GRATE_HEIGHTS = {
            "0", "0", "1", "1", "2", "2", "3", "3", "4", "4",
            "5", "5", "6", "6", "7", "7", "8", "8", "9", "9",
    };

    private static final String[] CUBES_DIRECT = {
            "ancient_scrap", "basalt_brick", "basalt_polished", "basalt_smooth", "basalt_tiles",
            "block_aluminium", "block_asbestos", "block_beryllium", "block_cobalt",
            "block_combine_steel", "block_copper", "block_corium_cobble", "block_desh",
            "block_dineutronium", "block_dura_steel", "block_euphemium", "block_fluorite", "block_foam",
            "block_meteor", "block_meteor_broken", "block_meteor_cobble", "block_meteor_treasure",
            "block_niter", "block_red_copper", "block_saturnite", "block_starmetal", "block_steel",
            "block_sulfur", "block_titanium", "block_tungsten", "brick_asbestos",
            "brick_concrete_broken", "brick_concrete_cracked", "brick_dungeon", "brick_dungeon_circle",
            "brick_dungeon_flat", "brick_dungeon_tile", "brick_fire", "brick_jungle",
            "brick_jungle_cracked", "brick_light", "brick_obsidian", "cmb_brick", "concrete_asbestos",
            "crystal_hardened", "deco_aluminium", "deco_asbestos", "deco_beryllium", "deco_lead",
            "deco_red_copper", "deco_rusty_steel", "deco_steel", "deco_titanium", "deco_tungsten",
            "fatduck", "fusion_component", "fusion_component_blanket", "fusion_component_bscco_welded",
            "fusion_component_motor", "gneiss_brick", "gneiss_chiseled", "gneiss_tile", "meteor_brick",
            "meteor_brick_chiseled", "meteor_brick_cracked", "meteor_brick_mossy", "meteor_polished",
            "muffler", "oil_pipe", "ore_aluminium", "ore_asbestos", "ore_australium",
            "ore_bedrock_block", "ore_bedrock_oil", "ore_beryllium", "ore_cinnabar", "ore_cobalt",
            "ore_coltan", "ore_copper", "ore_fluorite", "ore_gneiss_asbestos", "ore_gneiss_copper",
            "ore_gneiss_gas", "ore_gneiss_gold", "ore_gneiss_iron", "ore_gneiss_lithium",
            "ore_gneiss_rare", "ore_gneiss_schrabidium", "ore_lead", "ore_lignite", "ore_nether_cobalt",
            "ore_nether_fire", "ore_nether_plutonium", "ore_nether_schrabidium", "ore_nether_sulfur",
            "ore_nether_tungsten", "ore_niter", "ore_oil", "ore_oil_empty", "ore_rare",
            "ore_schrabidium", "ore_sulfur", "ore_tektite_osmiridium", "ore_thorium", "ore_titanium",
            "ore_tungsten", "pink_planks", "reinforced_sand", "reinforced_stone", "sandbags",
            "seal_frame", "stone_cracked", "stone_gneiss", "struct_launcher", "struct_scaffold",
            "struct_torus_core", "struct_watz_core", "structure_anchor", "tektite", "tile_lab",
            "tile_lab_broken", "tile_lab_cracked", "waste_planks", "watz_casing", "watz_casing_tooled"
    };

    private static final String[] CUBES_VIA = {
            "ash_digamma", "block_actinium", "block_bakelite", "block_bismuth", "block_boron",
            "block_cadmium", "block_coltan", "block_corium", "block_electrical_scrap", "block_graphite",
            "block_lanthanium", "block_lead", "block_magnetized_tungsten", "block_mox_fuel",
            "block_neptunium", "block_niobium", "block_plutonium", "block_plutonium_fuel",
            "block_polonium", "block_polymer", "block_pu238", "block_pu239", "block_pu240",
            "block_pu_mix", "block_ra226", "block_red_phosphorus", "block_rubber", "block_schrabidate",
            "block_schrabidium", "block_schrabidium_fuel", "block_schraranium", "block_scrap",
            "block_solinium", "block_tantalium", "block_thorium", "block_thorium_fuel",
            "block_trinitite", "block_u233", "block_u235", "block_u238", "block_uranium",
            "block_uranium_fuel", "block_white_phosphorus", "block_zirconium", "brick_compound",
            "brick_concrete", "brick_concrete_mossy", "cluster_aluminium", "cluster_copper",
            "cluster_depth_iron", "cluster_depth_titanium", "cluster_depth_tungsten", "cluster_iron",
            "cluster_titanium", "cmb_brick_reinforced", "deco_rbmk", "deco_rbmk_smooth", "dirt_dead",
            "dirt_oily", "ducrete", "ducrete_smooth", "gravel_diamond", "gravel_obsidian", "moon_turf",
            "mud_block", "ore_alexandrite", "ore_depth_borax", "ore_depth_cinnabar",
            "ore_depth_nether_neodymium", "ore_depth_nether_nitan", "ore_depth_zirconium",
            "ore_gneiss_uranium", "ore_gneiss_uranium_scorched", "ore_nether_coal",
            "ore_nether_smoldering", "ore_nether_uranium", "ore_nether_uranium_scorched",
            "ore_oil_sand", "ore_uranium", "ore_uranium_scorched", "rbmk_steam_inlet",
            "rbmk_steam_outlet", "reinforced_brick", "sand_boron", "sand_dirty", "sand_dirty_red",
            "sand_gold", "sand_lead", "sand_quartz", "stone_depth", "stone_depth_nether",
            "waste_leaves"
    };

    /**
     * Blocks that are one texture on all six faces, with one state and one model, and whose
     * texture is named after the block. The single largest shape in the corpus.
     */
    private static void cubes() {
        for (String name : CUBES_DIRECT) {
            Dejson.block(NS, name)
                    .models(Models.cubeAll("hbm:blocks/" + name))
                    .states(Variants.simple())
                    .blockItemDirect()
                    .register();
        }
        for (String name : CUBES_VIA) {
            Dejson.block(NS, name)
                    .models(Models.cubeAll("hbm:blocks/" + name))
                    .states(Variants.simple())
                    .blockItem()
                    .register();
        }
    }

    private static final String[] SLABS_DIRECT = {
            "brick_asbestos", "brick_compound", "brick_concrete", "brick_concrete_broken",
            "brick_concrete_cracked", "brick_concrete_mossy", "brick_fire", "brick_light",
            "brick_obsidian", "cmb_brick_reinforced", "concrete_asbestos", "concrete_black",
            "concrete_blue", "concrete_brown", "concrete_cyan", "concrete_gray", "concrete_green",
            "concrete_light_blue", "concrete_lime", "concrete_magenta", "concrete_orange",
            "concrete_pink", "concrete_purple", "concrete_red", "concrete_silver", "concrete_white",
            "concrete_yellow", "ducrete", "ducrete_brick", "ducrete_reinforced", "ducrete_smooth",
            "reinforced_brick", "reinforced_sand", "reinforced_stone", "tile_lab", "tile_lab_broken",
            "tile_lab_cracked"
    };

    /** Slabs cut from one texture: {@code <base>_slab} over {@code blocks/<base>}. */
    private static void slabs() {
        for (String base : SLABS_DIRECT) {
            Dejson.block(NS, base + "_slab")
                    .models(Models.slabAll("hbm:blocks/" + base))
                    .states(Variants.slab())
                    .blockItemDirect()
                    .register();
        }
    }

    private static final String[] STAIRS_DIRECT = {
            "brick_asbestos", "brick_compound", "brick_concrete", "brick_concrete_broken",
            "brick_concrete_cracked", "brick_concrete_mossy", "brick_fire", "brick_light",
            "brick_obsidian", "cmb_brick_reinforced", "concrete_asbestos", "ducrete", "ducrete_smooth",
            "reinforced_brick", "reinforced_sand", "reinforced_stone", "tile_lab", "tile_lab_broken",
            "tile_lab_cracked"
    };

    /** Stairs cut from one texture: {@code <base>_stairs} over {@code blocks/<base>}. */
    private static void stairs() {
        for (String base : STAIRS_DIRECT) {
            Dejson.block(NS, base + "_stairs")
                    .models(Models.stairsAll("hbm:blocks/" + base))
                    .states(Variants.stairs())
                    .blockItemDirect()
                    .register();
        }
    }

    private static final String[] RENDERED_0 = {
            "chimney_brick", "chimney_industrial", "furnace_iron", "furnace_steel", "fusion_boiler",
            "fusion_breeder", "fusion_collector", "fusion_coupler", "fusion_klystron",
            "fusion_klystron_creative", "fusion_mhdt", "fusion_plasma_forge", "fusion_torus",
            "heat_boiler", "heater_electric", "heater_firebox", "heater_heatex", "heater_oilburner",
            "heater_oven", "launch_pad", "machine_arc_furnace", "machine_arc_welder", "machine_ashpit",
            "machine_assembly_factory", "machine_assembly_machine", "machine_bat9000",
            "machine_battery_redd", "machine_battery_socket", "machine_bigasstank",
            "machine_blast_furnace", "machine_catalytic_cracker", "machine_catalytic_reformer",
            "machine_chemical_factory", "machine_chemical_plant", "machine_coker",
            "machine_combustion_engine", "machine_compressor", "machine_condenser_powered",
            "machine_crucible", "machine_crystallizer", "machine_cyclotron", "machine_deuterium_tower",
            "machine_electrolyser", "machine_exposure_chamber", "machine_fel", "machine_flare",
            "machine_fluidtank", "machine_fracking_tower", "machine_fraction_tower",
            "machine_furnace_combination", "machine_hydrotreater", "machine_industrial_boiler",
            "machine_liquefactor", "machine_mining_laser", "machine_precass", "machine_puf6_tank",
            "machine_pump_electric", "machine_pump_steam", "machine_pumpjack", "machine_purex",
            "machine_pyrooven", "machine_radgen", "machine_refinery", "machine_sawmill", "machine_silex",
            "machine_solar_boiler", "machine_soldering_station", "machine_solidifier", "machine_stirling",
            "machine_stirling_creative", "machine_stirling_steel", "machine_strand_caster",
            "machine_uf6_tank", "machine_vacuum_distill", "machine_well", "machine_wood_burner",
            "machine_zirnox", "radio_autocal", "watz"
    };

    /**
     * Folded machines: the core and its cells are one block told apart by {@code cell}, and the
     * renderer on the core draws both, so every variant is the same empty placeholder.
     */
    private static final String[] RENDERED_FOLDED = {
            "fraction_spacer", "machine_ammo_press", "machine_centrifuge", "machine_drain",
            "machine_epress", "machine_fensu", "machine_gascent", "machine_mixer", "machine_press",
            "radio_telex", "watz_pump"
    };

    private static final String[] RENDERED_1 = {
            "anvil_arsenic_bronze", "anvil_bismuth_bronze", "anvil_desh", "anvil_dnt",
            "anvil_ferrouranium", "anvil_iron", "anvil_lead", "anvil_murky", "anvil_osmiridium",
            "anvil_saturnite", "anvil_schrabidate", "anvil_steel", "boat", "boxcar", "deco_sat_foeq",
            "deco_sat_laser", "deco_sat_mapper", "deco_sat_radar", "deco_sat_resonator",
            "deco_sat_scanner", "foundry_outlet", "foundry_slagtap", "machine_diesel", "refueler",
            "steel_corner", "steel_roof", "steel_wall", "turret_arty", "turret_chekhov",
            "turret_friendly", "turret_fritz", "turret_himars", "turret_howard",
            "turret_howard_damaged", "turret_jeremy", "turret_maxwell", "turret_richard",
            "turret_tauon"
    };

    private static final String[] RENDERED_2 = {
            "foundry_basin", "gas_explosive", "gas_flammable", "machine_autosaw", "machine_minirtg",
            "machine_rtg_grey", "machine_storage_drum", "rbmk_console", "rbmk_crane_console",
            "rtg_polonium", "slag", "solar_mirror", "turret_emplacement", "turret_pad", "turret_sentry",
            "turret_sentry_damaged"
    };

    private static final String[] RENDERED_3 = {
            "nuke_boy", "nuke_gadget", "nuke_man", "nuke_mike", "nuke_n2", "nuke_tsar"
    };

    private static final String[] RENDERED_4 = {
            "machine_difurnace_ext"
    };

    private static final String[] RENDERED_5 = {
            "rbmk_absorber"
    };

    private static final String[] RENDERED_6 = {
            "rbmk_blank"
    };

    private static final String[] RENDERED_7 = {
            "rbmk_boiler"
    };

    private static final String[] RENDERED_8 = {
            "rbmk_cooler"
    };

    private static final String[] RENDERED_9 = {
            "rbmk_fuel_rod"
    };

    private static final String[] RENDERED_10 = {
            "rbmk_fuel_rod_moderated"
    };

    private static final String[] RENDERED_11 = {
            "rbmk_fuel_rod_reasim"
    };

    private static final String[] RENDERED_12 = {
            "rbmk_fuel_rod_reasim_moderated"
    };

    private static final String[] RENDERED_13 = {
            "rbmk_heater"
    };

    private static final String[] RENDERED_14 = {
            "rbmk_moderator"
    };

    private static final String[] RENDERED_15 = {
            "rbmk_outgasser"
    };

    private static final String[] RENDERED_16 = {
            "rbmk_reflector"
    };

    private static final String[] RENDERED_17 = {
            "rbmk_storage"
    };

    /**
     * Machines drawn by a renderer rather than by a model. Their blockstate exists only because
     * vanilla demands one; every variant of every one of them points at the same empty placeholder.
     */
    private static void renderedMachines() {
        for (String name : RENDERED_0) {
            Dejson.block(NS, name).rendered("hbm:block/machine_dummy_empty").states(Variants.uniformRange("meta", 0, 15)).register();
        }
        for (String name : RENDERED_FOLDED) {
            Dejson.block(NS, name).rendered("hbm:block/machine_dummy_empty")
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).register();
        }
        for (String name : RENDERED_1) {
            Dejson.block(NS, name).rendered("hbm:block/machine_dummy_empty").states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        }
        for (String name : RENDERED_2) {
            Dejson.block(NS, name).rendered("hbm:block/machine_dummy_empty").states(Variants.simple()).register();
        }
        for (String name : RENDERED_3) {
            Dejson.block(NS, name).rendered("hbm:block/machine_dummy_empty").states(Variants.uniformRange("facing", 2, 5)).register();
        }
        for (String name : RENDERED_4) {
            Dejson.block(NS, name).rendered("hbm:block/difurnace_extension").states(Variants.simple()).register();
        }
        for (String name : RENDERED_5) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_absorber_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_6) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_blank_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_7) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_boiler_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_8) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_cooler_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_9) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_element_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_10) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_element_mod_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_11) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_element_reasim_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_12) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_element_reasim_mod_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_13) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_heater_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_14) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_moderator_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_15) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_outgasser_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_16) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_reflector_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
        for (String name : RENDERED_17) {
            Dejson.block(NS, name).rendered("hbm:block/rbmk/rbmk_storage_column_none").states(Variants.product(Variants.uniform("lid", "none", "concrete", "glass"), Variants.uniform("part", "core", "middle", "top"))).register();
        }
    }

    /** Everything whose shape or blockstate is its own; one statement each. */
    private static void everythingElse() {
        others0();
        others1();
        others2();
    }

    private static void others0() {
        Dejson.block(NS, "balefire")
                .models(Models.group()
                        .and("_floor", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/template_fire_floor", Slots.of("fire", "hbm:blocks/balefire", "particle", "hbm:blocks/balefire")))))
                .states(Variants.select("age", "0", "_floor", "1", "_floor", "2", "_floor", "3", "_floor", "4", "_floor", "5", "_floor", "6", "_floor", "7", "_floor", "8", "_floor", "9", "_floor", "10", "_floor", "11", "_floor", "12", "_floor", "13", "_floor", "14", "_floor", "15", "_floor"))
                .blockItem()
                .register();

        Dejson.block(NS, "basalt")
                .models(Models.column("hbm:blocks/basalt_top", "hbm:blocks/basalt"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "block_au198")
                .models(Models.cubeAll("minecraft:block/gold_block"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "block_slag")
                .models(Models.group()
                        .and("", Models.cubeAll("hbm:blocks/block_slag"))
                        .and("_cracked", Models.cubeAll("hbm:blocks/block_slag_broken")))
                .states(Variants.select("cracked", "false", "", "true", "_cracked"))
                .blockItemDirect()
                .register();

        Dejson.block(NS, "block_smore")
                .models(Models.column("hbm:blocks/block_smore_top", "hbm:blocks/block_smore_side"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "c4")
                .models(Models.cubeBottomTop("hbm:blocks/c4_bottom", "hbm:blocks/c4_side", "hbm:blocks/c4_top"))
                .states(Variants.uniform("armed", "false", "true"))
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete")
                .models(Models.cubeAll("hbm:blocks/concrete_tile"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored")
                .models(Models.group()
                        .and("_black", Models.cubeAll("hbm:blocks/concrete_black"))
                        .and("_blue", Models.cubeAll("hbm:blocks/concrete_blue"))
                        .and("_brown", Models.cubeAll("hbm:blocks/concrete_brown"))
                        .and("_cyan", Models.cubeAll("hbm:blocks/concrete_cyan"))
                        .and("_gray", Models.cubeAll("hbm:blocks/concrete_gray"))
                        .and("_green", Models.cubeAll("hbm:blocks/concrete_green"))
                        .and("_light_blue", Models.cubeAll("hbm:blocks/concrete_light_blue"))
                        .and("_lime", Models.cubeAll("hbm:blocks/concrete_lime"))
                        .and("_magenta", Models.cubeAll("hbm:blocks/concrete_magenta"))
                        .and("_orange", Models.cubeAll("hbm:blocks/concrete_orange"))
                        .and("_pink", Models.cubeAll("hbm:blocks/concrete_pink"))
                        .and("_purple", Models.cubeAll("hbm:blocks/concrete_purple"))
                        .and("_red", Models.cubeAll("hbm:blocks/concrete_red"))
                        .and("_silver", Models.cubeAll("hbm:blocks/concrete_silver"))
                        .and("_white", Models.cubeAll("hbm:blocks/concrete_white"))
                        .and("_yellow", Models.cubeAll("hbm:blocks/concrete_yellow")))
                .states(Variants.select("color", "white", "_white", "orange", "_orange", "magenta", "_magenta", "light_blue", "_light_blue", "yellow", "_yellow", "lime", "_lime", "pink", "_pink", "gray", "_gray", "light_gray", "_silver", "cyan", "_cyan", "purple", "_purple", "blue", "_blue", "brown", "_brown", "green", "_green", "red", "_red", "black", "_black"))
                .register();

        Dejson.block(NS, "concrete_colored_ext")
                .models(Models.group()
                        .and("_bronze", Models.cubeAll("hbm:blocks/concrete_colored_ext.bronze"))
                        .and("_hazard", Models.cubeAll("hbm:blocks/concrete_colored_ext.hazard"))
                        .and("_indigo", Models.cubeAll("hbm:blocks/concrete_colored_ext.indigo"))
                        .and("_machine", Models.cubeAll("hbm:blocks/concrete_colored_ext.machine"))
                        .and("_machine_stripe", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/concrete_colored_ext.machine").and("down", "hbm:blocks/concrete_colored_ext.machine").and("north", "hbm:blocks/concrete_colored_ext.machine_stripe").and("south", "hbm:blocks/concrete_colored_ext.machine_stripe").and("west", "hbm:blocks/concrete_colored_ext.machine_stripe").and("east", "hbm:blocks/concrete_colored_ext.machine_stripe").and("particle", "hbm:blocks/concrete_colored_ext.machine_stripe")))
                        .and("_pink", Models.cubeAll("hbm:blocks/concrete_colored_ext.pink"))
                        .and("_purple", Models.cubeAll("hbm:blocks/concrete_colored_ext.purple"))
                        .and("_sand", Models.cubeAll("hbm:blocks/concrete_colored_ext.sand")))
                .states(Variants.select("type", "machine", "_machine", "machine_stripe", "_machine_stripe", "indigo", "_indigo", "purple", "_purple", "pink", "_pink", "hazard", "_hazard", "sand", "_sand", "bronze", "_bronze"))
                .register();

        Dejson.block(NS, "concrete_colored_ext_stairs_bronze")
                .models(Models.stairsAll("hbm:blocks/concrete_colored_ext.bronze"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_ext_stairs_hazard")
                .models(Models.stairsAll("hbm:blocks/concrete_colored_ext.hazard"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_ext_stairs_indigo")
                .models(Models.stairsAll("hbm:blocks/concrete_colored_ext.indigo"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_ext_stairs_machine")
                .models(Models.stairsAll("hbm:blocks/concrete_colored_ext.machine"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_ext_stairs_machine_stripe")
                .models(Models.stairsAll("hbm:blocks/concrete_colored_ext.machine_stripe"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_ext_stairs_pink")
                .models(Models.stairsAll("hbm:blocks/concrete_colored_ext.pink"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_ext_stairs_purple")
                .models(Models.stairsAll("hbm:blocks/concrete_colored_ext.purple"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_ext_stairs_sand")
                .models(Models.stairsAll("hbm:blocks/concrete_colored_ext.sand"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_black")
                .models(Models.stairsAll("hbm:blocks/concrete_black"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_blue")
                .models(Models.stairsAll("hbm:blocks/concrete_blue"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_brown")
                .models(Models.stairsAll("hbm:blocks/concrete_brown"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_cyan")
                .models(Models.stairsAll("hbm:blocks/concrete_cyan"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_gray")
                .models(Models.stairsAll("hbm:blocks/concrete_gray"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_green")
                .models(Models.stairsAll("hbm:blocks/concrete_green"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_light_blue")
                .models(Models.stairsAll("hbm:blocks/concrete_light_blue"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_lime")
                .models(Models.stairsAll("hbm:blocks/concrete_lime"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_magenta")
                .models(Models.stairsAll("hbm:blocks/concrete_magenta"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_orange")
                .models(Models.stairsAll("hbm:blocks/concrete_orange"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_pink")
                .models(Models.stairsAll("hbm:blocks/concrete_pink"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_purple")
                .models(Models.stairsAll("hbm:blocks/concrete_purple"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_red")
                .models(Models.stairsAll("hbm:blocks/concrete_red"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_silver")
                .models(Models.stairsAll("hbm:blocks/concrete_silver"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_white")
                .models(Models.stairsAll("hbm:blocks/concrete_white"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_colored_stairs_yellow")
                .models(Models.stairsAll("hbm:blocks/concrete_yellow"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_slab")
                .models(Models.slabAll("hbm:blocks/concrete_tile"))
                .states(Variants.slab())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_smooth")
                .models(Models.cubeAll("hbm:blocks/concrete"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_smooth_slab")
                .models(Models.slabAll("hbm:blocks/concrete"))
                .states(Variants.slab())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_smooth_stairs")
                .models(Models.stairsAll("hbm:blocks/concrete"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "concrete_stairs")
                .models(Models.stairsAll("hbm:blocks/concrete_tile"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "crate_desh")
                .models(Models.cubeBottomTop("hbm:blocks/crate_desh_top", "hbm:blocks/crate_desh_side", "hbm:blocks/crate_desh_top"))
                .states(Variants.facing())
                .blockItem()
                .register();

        Dejson.block(NS, "crate_iron")
                .models(Models.cubeBottomTop("hbm:blocks/crate_iron_top", "hbm:blocks/crate_iron_side", "hbm:blocks/crate_iron_top"))
                .states(Variants.facing())
                .blockItem()
                .register();

        Dejson.block(NS, "crate_steel")
                .models(Models.cubeBottomTop("hbm:blocks/crate_steel_top", "hbm:blocks/crate_steel_side", "hbm:blocks/crate_steel_top"))
                .states(Variants.facing())
                .blockItem()
                .register();

        Dejson.block(NS, "crate_template")
                .models(Models.cubeBottomTop("hbm:blocks/crate_template", "hbm:blocks/crate_template", "hbm:blocks/crate_template"))
                .states(Variants.facing())
                .blockItem()
                .register();

        Dejson.block(NS, "creative_fluid_source")
                .models(Models.cubeAll("hbm:blocks/foundry_tank_side"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "creative_generator")
                .models(Models.cubeAll("hbm:blocks/machine_generator"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "digamma_matter")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/digamma_matter")))
                .states(Variants.uniformRange("meta", 0, 15))
                .blockItem()
                .register();

        Dejson.block(NS, "dynamite")
                .models(Models.cubeBottomTop("hbm:blocks/dynamite_bottom", "hbm:blocks/dynamite_side", "hbm:blocks/dynamite_top"))
                .states(Variants.uniform("armed", "false", "true"))
                .blockItemDirect()
                .register();

        Dejson.block(NS, "fan")
                .models(Models.parented("minecraft:block/air", Slots.EMPTY))
                .states(Variants.uniform("facing", "down", "up", "north", "south", "west", "east"))
                .register();

        Dejson.block(NS, "fluid_valve")
                .models(Models.group()
                        .and("_off", Models.cubeAll("hbm:blocks/fluid_valve_off"))
                        .and("_on", Models.cubeAll("hbm:blocks/fluid_valve_on")))
                .states(Variants.select("open", "false", "_off", "true", "_on"))
                .register();

        Dejson.block(NS, "fusion_core_block")
                .models(Models.column("hbm:blocks/fusion_core_side_alt", "hbm:blocks/fusion_core_side_alt"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "fusion_hatch")
                .models(Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/fusion_hatch").and("north", "hbm:blocks/fusion_hatch").and("south", "hbm:blocks/fusion_heater_side").and("east", "hbm:blocks/fusion_heater_side").and("west", "hbm:blocks/fusion_heater_side").and("up", "hbm:blocks/block_tungsten").and("down", "hbm:blocks/block_tungsten")))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "fusion_heater")
                .models(Models.column("hbm:blocks/fusion_heater_top", "hbm:blocks/fusion_heater_side"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "glass_ash")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_ash")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "glass_boron")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_boron")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "glass_lead")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_lead")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "glass_polarized")
                .models(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/glass_polarized")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "glass_polonium")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_polonium")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "glass_quartz")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_quartz")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "glass_trinitite")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_trinitite")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "glass_uranium")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/glass_uranium")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "hadron_analysis_glass")
                .models(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/hadron_analysis_glass")))
                .states(Variants.simple())
                .blockItem()
                .register();
    }

    private static void others1() {
        Dejson.block(NS, "hadron_plating_glass")
                .models(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/hadron_plating_glass")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "lightstone")
                .models(Models.group()
                        .and("_bricks", Models.cubeAll("hbm:blocks/lightstone.bricks"))
                        .and("_bricks_chiseled", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/lightstone.bricks_chiseled.top").and("down", "hbm:blocks/lightstone.bricks_chiseled.top").and("north", "hbm:blocks/lightstone.bricks_chiseled").and("south", "hbm:blocks/lightstone.bricks_chiseled").and("west", "hbm:blocks/lightstone.bricks_chiseled").and("east", "hbm:blocks/lightstone.bricks_chiseled").and("particle", "hbm:blocks/lightstone.bricks_chiseled")))
                        .and("_chiseled", Models.parented("minecraft:block/cube", Slots.of("up", "hbm:blocks/lightstone.chiseled.top").and("down", "hbm:blocks/lightstone.chiseled.top").and("north", "hbm:blocks/lightstone.chiseled").and("south", "hbm:blocks/lightstone.chiseled").and("west", "hbm:blocks/lightstone.chiseled").and("east", "hbm:blocks/lightstone.chiseled").and("particle", "hbm:blocks/lightstone.chiseled")))
                        .and("_tile", Models.cubeAll("hbm:blocks/lightstone.tile"))
                        .and("_unrefined", Models.cubeAll("hbm:blocks/lightstone.unrefined")))
                .states(Variants.select("type", "unrefined", "_unrefined", "tile", "_tile", "bricks", "_bricks", "bricks_chiseled", "_bricks_chiseled", "chiseled", "_chiseled"))
                .register();

        Dejson.block(NS, "lightstone_bricks_slab")
                .models(Models.slabAll("hbm:blocks/lightstone.bricks"))
                .states(Variants.slab())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "lightstone_bricks_stairs")
                .models(Models.stairsAll("hbm:blocks/lightstone.bricks"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "lightstone_tile_slab")
                .models(Models.slabAll("hbm:blocks/lightstone.tile"))
                .states(Variants.slab())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "lightstone_tile_stairs")
                .models(Models.stairsAll("hbm:blocks/lightstone.tile"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_armor_table")
                .models(Models.cubeBottomTop("hbm:blocks/armor_table_bottom", "hbm:blocks/armor_table_side", "hbm:blocks/armor_table_top"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_battery")
                .models(Models.cubeAll("hbm:blocks/battery_side_alt"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_condenser")
                .models(Models.cubeAll("hbm:blocks/condenser"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_deuterium_extractor")
                .models(Models.cubeBottomTop("hbm:blocks/machine_deuterium_extractor_bottom", "hbm:blocks/machine_deuterium_extractor_side", "hbm:blocks/machine_deuterium_extractor_top"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_industrial_turbine")
                .models(Models.parented("minecraft:block/air", Slots.EMPTY))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "machine_intake")
                .models(Models.parented("minecraft:block/air", Slots.EMPTY))
                .states(Variants.uniformRange("meta", 0, 15))
                .register();

        Dejson.block(NS, "machine_large_turbine")
                .models(Models.parented("minecraft:block/air", Slots.EMPTY))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "machine_shredder")
                .models(Models.parented("minecraft:block/cube", Slots.of("down", "hbm:blocks/machine_shredder_bottom_alt").and("up", "hbm:blocks/machine_shredder_top_alt").and("north", "hbm:blocks/machine_shredder_front_alt").and("south", "hbm:blocks/machine_shredder_front_alt").and("west", "hbm:blocks/machine_shredder_side_alt").and("east", "hbm:blocks/machine_shredder_side_alt").and("particle", "hbm:blocks/machine_shredder_front_alt")))
                .states(Variants.facing())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_steam_engine")
                .models(Models.parented("minecraft:block/air", Slots.EMPTY))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "machine_transformer")
                .models(Models.cubeBottomTop("hbm:blocks/machine_transformer_bottom_iron", "hbm:blocks/machine_transformer_iron", "hbm:blocks/machine_transformer_top_iron"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_transformer_20")
                .models(Models.cubeBottomTop("hbm:blocks/machine_transformer_top_iron", "hbm:blocks/machine_transformer_iron", "hbm:blocks/machine_transformer_bottom_iron"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_transformer_dnt")
                .models(Models.cubeBottomTop("hbm:blocks/machine_transformer_bottom", "hbm:blocks/machine_transformer", "hbm:blocks/machine_transformer_top"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_transformer_dnt_20")
                .models(Models.cubeBottomTop("hbm:blocks/machine_transformer_top", "hbm:blocks/machine_transformer", "hbm:blocks/machine_transformer_bottom"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_turbine")
                .models(Models.cubeBottomTop("hbm:blocks/machine_turbine_top", "hbm:blocks/machine_turbine_base", "hbm:blocks/machine_turbine_top"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "machine_turbine_gas")
                .models(Models.parented("minecraft:block/air", Slots.EMPTY))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "machine_turbofan")
                .models(Models.parented("minecraft:block/air", Slots.EMPTY))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "machine_waste_drum")
                .models(Models.cubeAll("hbm:blocks/machine_transformer"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "mass_storage")
                .models(Models.cubeBottomTop("hbm:blocks/mass_storage_top", "hbm:blocks/mass_storage_side", "hbm:blocks/mass_storage_top"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "mass_storage_desh")
                .models(Models.cubeBottomTop("hbm:blocks/mass_storage_top_desh", "hbm:blocks/mass_storage_side_desh", "hbm:blocks/mass_storage_top_desh"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "mass_storage_iron")
                .models(Models.cubeBottomTop("hbm:blocks/mass_storage_top_iron", "hbm:blocks/mass_storage_side_iron", "hbm:blocks/mass_storage_top_iron"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "mass_storage_wood")
                .models(Models.cubeBottomTop("hbm:blocks/mass_storage_top_wood", "hbm:blocks/mass_storage_side_wood", "hbm:blocks/mass_storage_top_wood"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "meteor_battery")
                .models(Models.column("hbm:blocks/meteor_power", "hbm:blocks/meteor_spawner_side"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "oil_spill")
                .models(Models.group()
                        .and("_full", Models.cubeAll("hbm:blocks/oil_spill"))
                        .and("_height10", Models.parented("minecraft:block/snow_height10", Slots.of("particle", "hbm:blocks/oil_spill", "texture", "hbm:blocks/oil_spill")))
                        .and("_height12", Models.parented("minecraft:block/snow_height12", Slots.of("particle", "hbm:blocks/oil_spill", "texture", "hbm:blocks/oil_spill")))
                        .and("_height14", Models.parented("minecraft:block/snow_height14", Slots.of("particle", "hbm:blocks/oil_spill", "texture", "hbm:blocks/oil_spill")))
                        .and("_height2", Models.parented("minecraft:block/snow_height2", Slots.of("particle", "hbm:blocks/oil_spill", "texture", "hbm:blocks/oil_spill")))
                        .and("_height4", Models.parented("minecraft:block/snow_height4", Slots.of("particle", "hbm:blocks/oil_spill", "texture", "hbm:blocks/oil_spill")))
                        .and("_height6", Models.parented("minecraft:block/snow_height6", Slots.of("particle", "hbm:blocks/oil_spill", "texture", "hbm:blocks/oil_spill")))
                        .and("_height8", Models.parented("minecraft:block/snow_height8", Slots.of("particle", "hbm:blocks/oil_spill", "texture", "hbm:blocks/oil_spill"))))
                .states(Variants.select("layers", "1", "_height2", "2", "_height4", "3", "_height6", "4", "_height8", "5", "_height10", "6", "_height12", "7", "_height14", "8", "_full"))
                .register();

        Dejson.block(NS, "ore_sellafield_diamond")
                .models(Models.group()
                        .and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_diamond_0")))
                        .and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_diamond_1")))
                        .and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_diamond_2")))
                        .and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_diamond_3"))))
                .states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4)))
                .register();

        Dejson.block(NS, "ore_sellafield_emerald")
                .models(Models.group()
                        .and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_emerald_0")))
                        .and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_emerald_1")))
                        .and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_emerald_2")))
                        .and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_emerald_3"))))
                .states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4)))
                .register();

        Dejson.block(NS, "ore_sellafield_radgem")
                .models(Models.group()
                        .and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_radgem_0")))
                        .and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_radgem_1")))
                        .and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_radgem_2")))
                        .and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_radgem_3"))))
                .states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4)))
                .register();

        Dejson.block(NS, "ore_sellafield_schrabidium")
                .models(Models.group()
                        .and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_schrabidium_0")))
                        .and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_schrabidium_1")))
                        .and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_schrabidium_2")))
                        .and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_schrabidium_3"))))
                .states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4)))
                .register();

        Dejson.block(NS, "ore_sellafield_uranium_scorched")
                .models(Models.group()
                        .and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_uranium_scorched_0")))
                        .and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_uranium_scorched_1")))
                        .and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_uranium_scorched_2")))
                        .and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/ore_sellafield_uranium_scorched_3"))))
                .states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4)))
                .register();

        Dejson.block(NS, "ore_tikite")
                .models(Models.cubeAll("hbm:blocks/ore_tikite_alt"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "pink_slab")
                .models(Models.slabAll("hbm:blocks/pink_planks"))
                .states(Variants.slab())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "pink_stairs")
                .models(Models.stairsAll("hbm:blocks/pink_planks"))
                .states(Variants.stairs())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "radio_torch_controller")
                .models(Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_controller")))
                .states(Variants.product(Variants.facingUp(), Variants.uniform("lit", "false", "true")))
                .register();

        Dejson.block(NS, "radio_torch_counter")
                .models(Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_counter")))
                .states(Variants.product(Variants.facingUp(), Variants.uniform("lit", "false", "true")))
                .register();

        Dejson.block(NS, "radio_torch_logic")
                .models(Models.group()
                        .and("_off", Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_logic_off")))
                        .and("_on", Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_logic_on"))))
                .states(Variants.product(Variants.facingUp(), Variants.select("lit", "false", "_off", "true", "_on")))
                .register();

        Dejson.block(NS, "radio_torch_reader")
                .models(Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_reader")))
                .states(Variants.product(Variants.facingUp(), Variants.uniform("lit", "false", "true")))
                .register();

        Dejson.block(NS, "radio_torch_receiver")
                .models(Models.group()
                        .and("_off", Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_rec_off")))
                        .and("_on", Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_rec_on"))))
                .states(Variants.product(Variants.facingUp(), Variants.select("lit", "false", "_off", "true", "_on")))
                .register();

        Dejson.block(NS, "radio_torch_sender")
                .models(Models.group()
                        .and("_off", Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_sender_off")))
                        .and("_on", Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/rtty_sender_on"))))
                .states(Variants.product(Variants.facingUp(), Variants.select("lit", "false", "_off", "true", "_on")))
                .register();

        Dejson.block(NS, "radsensor")
                .models(Models.cubeBottomTop("hbm:blocks/radsensor", "hbm:blocks/radsensor_side", "hbm:blocks/radsensor"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "rbmk_debris")
                .models(Models.cubeAll("hbm:blocks/rbmk/rbmk_debris"))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "rbmk_debris_burning")
                .models(Models.cubeAll("hbm:blocks/rbmk/rbmk_debris_burning"))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "rbmk_debris_digamma")
                .models(Models.cubeAll("hbm:blocks/rbmk/rbmk_debris_digamma"))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "rbmk_debris_radiating")
                .models(Models.cubeAll("hbm:blocks/rbmk/rbmk_debris_radiating"))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "reinforced_glass")
                .models(Models.renderType("minecraft:cutout", Models.cubeAll("hbm:blocks/reinforced_glass")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "reinforced_laminate")
                .models(Models.renderType("minecraft:translucent", Models.cubeAll("hbm:blocks/reinforced_laminate")))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "sellafield_bedrock")
                .models(Models.group()
                        .and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked")))
                        .and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_1")))
                        .and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_2")))
                        .and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_3"))))
                .states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4)))
                .register();

        Dejson.block(NS, "sellafield_slaked")
                .models(Models.group()
                        .and("_0", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked")))
                        .and("_1", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_1")))
                        .and("_2", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_2")))
                        .and("_3", Models.parented("hbm:block/tinted_cube_all", Slots.of("all", "hbm:blocks/sellafield_slaked_3"))))
                .states(Variants.product(Variants.uniformRange("shade", 0, 9), Variants.pool(4)))
                .register();

        Dejson.block(NS, "semtex")
                .models(Models.cubeBottomTop("hbm:blocks/semtex_bottom", "hbm:blocks/semtex_side", "hbm:blocks/semtex_top"))
                .states(Variants.uniform("armed", "false", "true"))
                .blockItemDirect()
                .register();

        Dejson.block(NS, "stalactite_asbestos")
                .models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalactite.asbestos")))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "stalactite_sulfur")
                .models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalactite.sulfur")))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "stalagmite_asbestos")
                .models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalagmite.asbestos")))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "stalagmite_sulfur")
                .models(Models.renderType("minecraft:cutout", Models.cross("hbm:blocks/stalagmite.sulfur")))
                .states(Variants.simple())
                .register();

        Dejson.block(NS, "stone_resource_asbestos")
                .models(Models.cubeAll("hbm:blocks/stone_resource.asbestos"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "stone_resource_bauxite")
                .models(Models.cubeAll("hbm:blocks/stone_resource.bauxite"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "stone_resource_hematite")
                .models(Models.cubeAll("hbm:blocks/stone_resource.hematite"))
                .states(Variants.simple())
                .blockItem()
                .register();
    }

    private static void others2() {
        Dejson.block(NS, "stone_resource_limestone")
                .models(Models.cubeAll("hbm:blocks/stone_resource.limestone"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "stone_resource_malachite")
                .models(Models.cubeAll("hbm:blocks/stone_resource.malachite"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "stone_resource_sulfur")
                .models(Models.cubeAll("hbm:blocks/stone_resource.sulfur"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "taint")
                .models(Models.cubeAll("hbm:blocks/taint"))
                .states(Variants.uniformRange("taintage", 0, 15))
                .blockItem()
                .register();

        Dejson.block(NS, "test_fan_blowable")
                .models(Models.cubeAll("minecraft:block/stone"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "tnt_ntm")
                .models(Models.cubeBottomTop("hbm:blocks/tnt_bottom", "hbm:blocks/tnt_side", "hbm:blocks/tnt_top"))
                .states(Variants.uniform("armed", "false", "true"))
                .blockItemDirect()
                .register();

        Dejson.block(NS, "waste_earth")
                .models(Models.cubeBottomTop("minecraft:block/dirt", "hbm:blocks/contamination/waste_grass_side_0", "hbm:blocks/contamination/waste_grass_top_0"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "waste_log")
                .models(Models.column("hbm:blocks/waste_log_top", "hbm:blocks/waste_log_side"))
                .states(Variants.axis())
                .blockItem()
                .register();

        Dejson.block(NS, "waste_mycelium")
                .models(Models.cubeBottomTop("minecraft:block/dirt", "hbm:blocks/contamination/waste_mycelium_side_0", "hbm:blocks/contamination/waste_mycelium_top"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "waste_trinitite")
                .models(Models.cubeAll("hbm:blocks/contamination/waste_trinitite_0"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "waste_trinitite_red")
                .models(Models.cubeAll("hbm:blocks/contamination/waste_trinitite_red_0"))
                .states(Variants.simple())
                .blockItem()
                .register();

        Dejson.block(NS, "watz_cooler")
                .models(Models.column("hbm:blocks/watz_cooler_top", "hbm:blocks/watz_cooler_side"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        Dejson.block(NS, "watz_element")
                .models(Models.column("hbm:blocks/watz_element_top", "hbm:blocks/watz_element_side"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();
    }
}
