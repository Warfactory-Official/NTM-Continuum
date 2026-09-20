package com.hbm.blocks;

import dev.golem.core.dejson.Drops;

/**
 * Every block loot table hbm can state in code, declared through Golem Core's {@code dejson}
 * vocabulary instead of written as JSON.
 *
 * <p>The {@value #FILES_REPLACED} loot tables these declarations stand in for have been deleted.
 * They said six things between them: drops itself, drops itself even when blown up, drops nothing,
 * drops double as a double slab, keeps its inventory, drops its mineral unless silk-touched. The
 * commonest of those, "drops itself", had drifted into six different spellings on disk, because
 * there was nowhere to say it once. The two tables that genuinely are programs,
 * {@code waste_leaves} and {@code zirnox_destroyed}, are still files, which is the right place for
 * them.
 *
 * <p>The spellings that differed only in {@code bonus_rolls} and {@code random_sequence} were
 * normalised onto the shape vanilla's own generator emits before the files were deleted; neither
 * key changes what a player gets. {@code survives_explosion} was not normalised — whether a blown-up
 * block leaves its item behind is a decision about the block, so {@link #ALWAYS} is its own list.
 *
 * <p>A file still beats a declaration: {@code dejsonGenerate} skips any path that exists under
 * {@code src/main/resources}, so writing one back is how a block gets an exception.
 */
public final class ModBlockDrops {

    private static final String NS = "hbm";

    /** How many loot tables the declarations below stand in for; quoted in this class's own javadoc. */
    static final int FILES_REPLACED = 541;

    private static boolean declared;

    private ModBlockDrops() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        for (String block : SELF) {
            Drops.self(NS, block);
        }
        for (String block : ALWAYS) {
            Drops.selfAlways(NS, block);
        }
        for (String block : NOTHING) {
            Drops.nothing(NS, block);
        }
        for (String block : SLABS) {
            Drops.slab(NS, block);
        }
        for (String block : CRATES) {
            Drops.container(NS, block, "minecraft:custom_name", "minecraft:container");
        }
        for (String block : STORAGE) {
            Drops.container(NS, block, "minecraft:container", "minecraft:custom_data", "minecraft:custom_name");
        }
        ores();
        Drops.only(NS, "fallout", "hbm:falloutitem");
        Drops.only(NS, "ore_oil", "hbm:oil_tar_crude");
    }

    private static final String[] SELF = {
            "anvil_arsenic_bronze", "anvil_bismuth_bronze", "anvil_desh", "anvil_dnt", "anvil_ferrouranium",
            "anvil_iron", "anvil_lead", "anvil_murky", "anvil_osmiridium", "anvil_saturnite", "anvil_schrabidate",
            "anvil_steel", "ash_digamma", "basalt", "basalt_brick", "basalt_polished", "basalt_smooth",
            "basalt_tiles", "block_actinium", "block_aluminium", "block_au198", "block_bakelite", "block_beryllium",
            "block_bismuth", "block_boron", "block_cadmium", "block_cobalt", "block_coltan", "block_combine_steel",
            "block_copper", "block_corium", "block_desh", "block_dineutronium", "block_dura_steel",
            "block_electrical_scrap", "block_euphemium", "block_fluorite", "block_foam", "block_lanthanium",
            "block_lead", "block_magnetized_tungsten", "block_meteor", "block_meteor_treasure", "block_mox_fuel",
            "block_neptunium", "block_niobium", "block_niter", "block_plutonium", "block_plutonium_fuel",
            "block_polonium", "block_polymer", "block_pu238", "block_pu239", "block_pu240", "block_pu_mix",
            "block_ra226", "block_red_copper", "block_red_phosphorus", "block_rubber", "block_saturnite",
            "block_schrabidate", "block_schrabidium", "block_schrabidium_fuel", "block_schraranium", "block_scrap",
            "block_slag", "block_smore", "block_solinium", "block_starmetal", "block_steel", "block_sulfur",
            "block_tantalium", "block_thorium", "block_thorium_fuel", "block_titanium", "block_trinitite",
            "block_tungsten", "block_u233", "block_u235", "block_u238", "block_uranium", "block_uranium_fuel",
            "block_white_phosphorus", "block_zirconium", "boat", "boxcar", "brick_asbestos_stairs",
            "brick_compound", "brick_compound_stairs", "brick_concrete", "brick_concrete_broken",
            "brick_concrete_broken_stairs", "brick_concrete_cracked", "brick_concrete_cracked_stairs",
            "brick_concrete_mossy", "brick_concrete_mossy_stairs", "brick_concrete_stairs", "brick_dungeon",
            "brick_dungeon_circle", "brick_dungeon_flat", "brick_dungeon_tile", "brick_fire", "brick_fire_stairs",
            "brick_jungle", "brick_jungle_cracked", "brick_light", "brick_light_stairs", "brick_obsidian",
            "brick_obsidian_stairs", "chimney_brick", "chimney_industrial", "cmb_brick", "cmb_brick_reinforced",
            "cmb_brick_reinforced_stairs", "concrete", "concrete_asbestos", "concrete_asbestos_stairs",
            "concrete_colored_ext_stairs_bronze", "concrete_colored_ext_stairs_hazard",
            "concrete_colored_ext_stairs_indigo", "concrete_colored_ext_stairs_machine",
            "concrete_colored_ext_stairs_machine_stripe", "concrete_colored_ext_stairs_pink",
            "concrete_colored_ext_stairs_purple", "concrete_colored_ext_stairs_sand",
            "concrete_colored_stairs_black", "concrete_colored_stairs_blue", "concrete_colored_stairs_brown",
            "concrete_colored_stairs_cyan", "concrete_colored_stairs_gray", "concrete_colored_stairs_green",
            "concrete_colored_stairs_light_blue", "concrete_colored_stairs_lime", "concrete_colored_stairs_magenta",
            "concrete_colored_stairs_orange", "concrete_colored_stairs_pink", "concrete_colored_stairs_purple",
            "concrete_colored_stairs_red", "concrete_colored_stairs_silver", "concrete_colored_stairs_white",
            "concrete_colored_stairs_yellow", "concrete_smooth", "concrete_smooth_stairs", "concrete_stairs",
            "creative_fluid_source", "creative_generator", "crystal_hardened", "deco_aluminium", "deco_beryllium",
            "deco_lead", "deco_red_copper", "deco_rusty_steel", "deco_sat_foeq", "deco_sat_laser",
            "deco_sat_mapper", "deco_sat_radar", "deco_sat_resonator", "deco_sat_scanner", "deco_steel",
            "deco_titanium", "deco_tungsten", "dirt_dead", "dirt_oily", "ducrete", "ducrete_smooth",
            "ducrete_smooth_stairs", "ducrete_stairs", "fan", "fatduck", "fluid_duct_mk2", "fluid_valve",
            "foundry_basin", "foundry_channel", "foundry_mold", "foundry_outlet", "foundry_slagtap",
            "fraction_spacer", "fusion_boiler", "fusion_collector", "fusion_component", "fusion_component_blanket",
            "fusion_component_bscco_welded", "fusion_component_motor", "fusion_core_block", "fusion_coupler",
            "fusion_hatch", "fusion_heater", "fusion_klystron", "fusion_klystron_creative", "fusion_mhdt",
            "gneiss_brick", "gneiss_chiseled", "gneiss_tile", "gravel_diamond", "gravel_obsidian", "heat_boiler",
            "heater_electric", "heater_firebox", "heater_heatex", "heater_oilburner", "heater_oven",
            "ladder_aluminium", "ladder_cobalt", "ladder_copper", "ladder_gold", "ladder_iron", "ladder_lead",
            "ladder_red", "ladder_red_top", "ladder_steel", "ladder_sturdy", "ladder_titanium", "ladder_tungsten",
            "launch_pad", "lightstone_bricks_stairs", "lightstone_tile_stairs", "machine_ammo_press",
            "machine_arc_furnace", "machine_arc_welder", "machine_ashpit", "machine_assembly_factory",
            "machine_assembly_machine", "machine_autosaw", "machine_battery", "machine_battery_redd",
            "machine_battery_socket", "machine_blast_furnace", "machine_catalytic_cracker",
            "machine_catalytic_reformer", "machine_centrifuge", "machine_chemical_factory",
            "machine_chemical_plant", "machine_coker", "machine_combustion_engine", "machine_compressor",
            "machine_condenser", "machine_condenser_powered", "machine_crucible", "machine_crystallizer",
            "machine_cyclotron", "machine_deuterium_extractor", "machine_deuterium_tower", "machine_diesel",
            "machine_difurnace_ext", "machine_difurnace_off", "machine_drain", "machine_electric_furnace_off",
            "machine_epress", "machine_exposure_chamber", "machine_fel", "machine_flare", "machine_fluidtank",
            "machine_fracking_tower", "machine_fraction_tower", "machine_furnace_combination",
            "machine_hydrotreater", "machine_industrial_turbine", "machine_intake", "machine_large_turbine",
            "machine_liquefactor", "machine_mining_laser", "machine_minirtg", "machine_mixer", "machine_precass",
            "machine_press", "machine_puf6_tank", "machine_pump_electric", "machine_pump_steam", "machine_pumpjack",
            "machine_purex", "machine_pyrooven", "machine_radgen", "machine_refinery", "machine_rtg_grey",
            "machine_shredder", "machine_silex", "machine_solar_boiler", "machine_soldering_station",
            "machine_solidifier", "machine_storage_drum", "machine_strand_caster", "machine_weapon_table", "machine_transformer",
            "machine_transformer_20", "machine_transformer_dnt", "machine_transformer_dnt_20", "machine_turbine",
            "machine_uf6_tank", "machine_vacuum_distill", "machine_waste_drum", "machine_well", "machine_zirnox",
            "meteor_battery", "meteor_brick", "meteor_brick_chiseled", "meteor_brick_cracked", "meteor_brick_mossy",
            "meteor_polished", "moon_turf", "muffler", "nuke_boy", "nuke_gadget", "nuke_man", "nuke_mike",
            "nuke_n2", "nuke_tsar", "ore_aluminium", "ore_australium", "ore_beryllium", "ore_copper",
            "ore_gneiss_copper", "ore_gneiss_gas", "ore_gneiss_gold", "ore_gneiss_iron", "ore_gneiss_lithium",
            "ore_gneiss_rare", "ore_gneiss_schrabidium", "ore_gneiss_uranium", "ore_gneiss_uranium_scorched",
            "ore_lead", "ore_lignite", "ore_nether_cobalt", "ore_nether_fire", "ore_nether_plutonium",
            "ore_nether_schrabidium", "ore_nether_tungsten", "ore_nether_uranium", "ore_nether_uranium_scorched",
            "ore_oil_empty", "ore_oil_sand", "ore_rare", "ore_schrabidium", "ore_sellafield_schrabidium",
            "ore_sellafield_uranium_scorched", "ore_tektite_osmiridium", "ore_thorium", "ore_tikite",
            "ore_titanium", "ore_tungsten", "ore_uranium", "ore_uranium_scorched", "pink_planks", "pink_stairs",
            "radio_autocal", "radio_telex", "radio_torch_controller", "radio_torch_counter", "radio_torch_logic",
            "radio_torch_reader", "radio_torch_receiver", "radio_torch_sender", "radsensor", "red_cable",
            "refueler", "reinforced_brick", "reinforced_brick_stairs", "reinforced_laminate", "reinforced_sand",
            "reinforced_sand_stairs", "reinforced_stone", "reinforced_stone_stairs", "rtg_polonium", "sand_boron",
            "sand_dirty", "sand_dirty_red", "sand_gold", "sand_lead", "sand_quartz", "sandbags", "seal_frame",
            "sellafield_slaked", "solar_mirror", "steel_beam", "steel_corner", "steel_grate", "steel_roof",
            "steel_scaffold", "steel_wall", "stone_cracked", "stone_gneiss", "struct_launcher", "struct_scaffold",
            "struct_torus_core", "struct_watz_core", "structure_anchor", "tektite", "tile_lab",
            "tile_lab_broken_stairs", "tile_lab_cracked", "tile_lab_cracked_stairs", "tile_lab_stairs",
            "turret_arty", "turret_chekhov", "turret_friendly", "turret_fritz", "turret_himars", "turret_howard",
            "turret_howard_damaged", "turret_jeremy", "turret_maxwell", "turret_richard", "turret_sentry",
            "turret_tauon", "waste_earth", "waste_log", "waste_mycelium", "waste_planks", "waste_trinitite",
            "waste_trinitite_red", "watz_casing", "watz_casing_tooled", "watz_cooler", "watz_element", "watz_pump"
    };

    /** Blocks that drop themselves even out of an explosion, where everything above does not. */
    private static final String[] ALWAYS = {
            "ancient_scrap", "block_asbestos", "block_corium_cobble", "block_graphite", "brick_asbestos", "c4",
            "deco_asbestos", "deco_rbmk", "deco_rbmk_smooth", "dynamite", "foam_layer", "rbmk_absorber",
            "rbmk_blank", "rbmk_boiler", "rbmk_console", "rbmk_control", "rbmk_control_auto", "rbmk_control_mod",
            "rbmk_control_reasim", "rbmk_control_reasim_auto", "rbmk_cooler", "rbmk_crane_console", "rbmk_debris",
            "rbmk_debris_burning", "rbmk_debris_digamma", "rbmk_debris_radiating", "rbmk_fuel_rod",
            "rbmk_fuel_rod_moderated", "rbmk_fuel_rod_reasim", "rbmk_fuel_rod_reasim_moderated", "rbmk_gauge",
            "rbmk_heater", "rbmk_moderator", "rbmk_outgasser", "rbmk_reflector", "rbmk_steam_inlet",
            "rbmk_steam_outlet", "rbmk_storage", "semtex", "tile_lab_broken", "tnt_ntm", "fireworks"
    };

    private static final String[] NOTHING = {
            "fusion_torus", "gas_radon", "gas_radon_dense", "glass_ash", "glass_polarized", "glass_polonium",
            "glass_trinitite", "glass_uranium", "hadron_analysis_glass", "hadron_plating_glass", "mud_block",
            "oil_pipe", "oil_spill", "ore_bedrock_oil", "reinforced_glass", "turret_emplacement", "turret_pad",
            "turret_sentry_damaged", "watz"
    };

    private static final String[] SLABS = {
            "brick_asbestos_slab", "brick_compound_slab", "brick_concrete_broken_slab",
            "brick_concrete_cracked_slab", "brick_concrete_mossy_slab", "brick_concrete_slab", "brick_fire_slab",
            "brick_light_slab", "brick_obsidian_slab", "cmb_brick_reinforced_slab", "concrete_asbestos_slab",
            "concrete_black_slab", "concrete_blue_slab", "concrete_brown_slab", "concrete_cyan_slab",
            "concrete_gray_slab", "concrete_green_slab", "concrete_light_blue_slab", "concrete_lime_slab",
            "concrete_magenta_slab", "concrete_orange_slab", "concrete_pink_slab", "concrete_purple_slab",
            "concrete_red_slab", "concrete_silver_slab", "concrete_slab", "concrete_smooth_slab",
            "concrete_white_slab", "concrete_yellow_slab", "ducrete_brick_slab", "ducrete_reinforced_slab",
            "ducrete_slab", "ducrete_smooth_slab", "lightstone_bricks_slab", "lightstone_tile_slab", "pink_slab",
            "reinforced_brick_slab", "reinforced_sand_slab", "reinforced_stone_slab", "tile_lab_broken_slab",
            "tile_lab_cracked_slab", "tile_lab_slab"
    };

    private static final String[] CRATES = {
            "crate_desh", "crate_iron", "crate_steel", "crate_template"
    };

    private static final String[] STORAGE = {
            "mass_storage", "mass_storage_desh", "mass_storage_iron", "mass_storage_wood"
    };

    private static void ores() {
        Drops.ore(NS, "ore_asbestos", "hbm:ingot_asbestos");
        Drops.ore(NS, "ore_cinnabar", "hbm:cinnabar", 1, 2);
        Drops.ore(NS, "ore_coltan", "hbm:fragment_coltan");
        Drops.ore(NS, "ore_fluorite", "hbm:fluorite", 2, 4);
        Drops.ore(NS, "ore_gneiss_asbestos", "hbm:ingot_asbestos");
        Drops.ore(NS, "ore_nether_sulfur", "hbm:sulfur", 2, 4);
        Drops.ore(NS, "ore_niter", "hbm:niter", 1, 2);
        Drops.ore(NS, "ore_sulfur", "hbm:sulfur", 2, 4);
        Drops.oreNoFortune(NS, "block_meteor_broken", "hbm:fragment_meteorite", 1, 3);
        Drops.oreNoFortune(NS, "block_meteor_cobble", "hbm:fragment_meteorite", 1, 3);
        Drops.oreNoFortune(NS, "ore_cobalt", "hbm:fragment_cobalt", 4, 9);
        Drops.oreDrops(NS, "ore_sellafield_diamond", "minecraft:diamond");
        Drops.oreDrops(NS, "ore_sellafield_emerald", "minecraft:emerald");
        Drops.oreDrops(NS, "ore_sellafield_radgem", "hbm:gem_rad");
        Drops.silkTouchOnly(NS, "glass_boron");
        Drops.silkTouchOnly(NS, "glass_lead");
    }
}
