package com.hbm.blocks;

import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.material.MapColor;

/**
 * Each HBM block's map colour: its 1.7 {@code Material}'s, or the 1.7 block's own {@code getMapColor}. A block not
 * listed was {@code Material.iron}. 1.7's air-coloured materials (glass, circuits, gas) stay {@link MapColor#NONE},
 * so maps draw what lies below them, as in 1.7.
 */
public final class MapColors {

    private static final Map<String, MapColor> COLORS = new HashMap<>();

    static {
        put(MapColor.STONE, """
                asphalt asphalt_light asphalt_slab asphalt_stairs basalt basalt_brick basalt_polished basalt_smooth basalt_tiles
                block_bakelite block_euphemium_cluster block_graphite_detector block_graphite_drilled block_graphite_fuel
                block_graphite_lithium block_graphite_plutonium block_graphite_rod block_graphite_source block_graphite_tritium
                block_meteor block_meteor_broken block_meteor_cobble block_meteor_molten block_meteor_treasure block_polymer
                block_rubber block_schrabidium_cluster block_slag block_smore block_white_phosphorus brick_asbestos
                brick_asbestos_slab brick_asbestos_stairs brick_compound brick_compound_slab brick_compound_stairs
                brick_concrete brick_concrete_broken brick_concrete_broken_slab brick_concrete_broken_stairs
                brick_concrete_cracked brick_concrete_cracked_slab brick_concrete_cracked_stairs brick_concrete_marked
                brick_concrete_mossy brick_concrete_mossy_slab brick_concrete_mossy_stairs brick_concrete_slab
                brick_concrete_stairs brick_ducrete brick_ducrete_slab brick_ducrete_stairs brick_dungeon brick_dungeon_circle
                brick_dungeon_flat brick_dungeon_tile brick_fire brick_fire_slab brick_fire_stairs brick_forgotten
                brick_forgotten_bw brick_forgotten_hole brick_forgotten_hole_empty brick_forgotten_lock brick_forgotten_lock_bw
                brick_forgotten_lock_nullstone brick_forgotten_nullroom_stone brick_forgotten_nullroom_wood
                brick_forgotten_nullstone brick_jungle brick_jungle_circle brick_jungle_cracked brick_jungle_fragile
                brick_jungle_glyph_0 brick_jungle_glyph_1 brick_jungle_glyph_10 brick_jungle_glyph_11 brick_jungle_glyph_12
                brick_jungle_glyph_13 brick_jungle_glyph_14 brick_jungle_glyph_15 brick_jungle_glyph_2 brick_jungle_glyph_3
                brick_jungle_glyph_4 brick_jungle_glyph_5 brick_jungle_glyph_6 brick_jungle_glyph_7 brick_jungle_glyph_8
                brick_jungle_glyph_9 brick_jungle_lava brick_jungle_mystic brick_jungle_ooze brick_jungle_trap_arrow
                brick_jungle_trap_falling_rocks brick_jungle_trap_fire brick_jungle_trap_flaming_arrow
                brick_jungle_trap_magic_converstion brick_jungle_trap_mine brick_jungle_trap_pillar
                brick_jungle_trap_poison_dart brick_jungle_trap_rad_conversion brick_jungle_trap_slowness
                brick_jungle_trap_spiders brick_jungle_trap_spikes brick_jungle_trap_weakness brick_jungle_trap_web
                brick_jungle_trap_zombie brick_light brick_light_slab brick_light_stairs brick_obsidian brick_obsidian_slab
                brick_obsidian_stairs brick_red cluster_aluminium cluster_copper cluster_depth_iron cluster_depth_titanium
                cluster_depth_tungsten cluster_iron cluster_titanium cmb_brick cmb_brick_reinforced cmb_brick_reinforced_slab
                cmb_brick_reinforced_stairs concrete concrete_asbestos concrete_asbestos_slab concrete_asbestos_stairs
                concrete_black_slab concrete_blue_slab concrete_brown_slab concrete_colored concrete_colored_ext
                concrete_colored_ext_stairs_bronze concrete_colored_ext_stairs_hazard concrete_colored_ext_stairs_indigo
                concrete_colored_ext_stairs_machine concrete_colored_ext_stairs_machine_stripe concrete_colored_ext_stairs_pink
                concrete_colored_ext_stairs_purple concrete_colored_ext_stairs_sand concrete_colored_stairs_black
                concrete_colored_stairs_blue concrete_colored_stairs_brown concrete_colored_stairs_cyan
                concrete_colored_stairs_gray concrete_colored_stairs_green concrete_colored_stairs_light_blue
                concrete_colored_stairs_lime concrete_colored_stairs_magenta concrete_colored_stairs_orange
                concrete_colored_stairs_pink concrete_colored_stairs_purple concrete_colored_stairs_red
                concrete_colored_stairs_silver concrete_colored_stairs_white concrete_colored_stairs_yellow concrete_cyan_slab
                concrete_gray_slab concrete_green_slab concrete_light_blue_slab concrete_lime_slab concrete_magenta_slab
                concrete_orange_slab concrete_pillar concrete_pink_slab concrete_purple_slab concrete_rebar concrete_red_slab
                concrete_silver_slab concrete_slab concrete_smooth concrete_smooth_slab concrete_smooth_stairs concrete_stairs
                concrete_super concrete_super_broken concrete_white_slab concrete_yellow_slab crate_jungle depth_brick depth_dnt
                depth_nether_brick depth_nether_tiles depth_tiles ducrete ducrete_brick_slab ducrete_reinforced_slab
                ducrete_slab ducrete_smooth ducrete_smooth_slab ducrete_smooth_stairs ducrete_stairs dungeon_spawner
                foundry_basin foundry_channel foundry_mold foundry_outlet foundry_slagtap foundry_tank geysir_chlorine
                geysir_nether gneiss_brick gneiss_chiseled gneiss_tile heater_oven lightstone lightstone_bricks_slab
                lightstone_bricks_stairs lightstone_tile_slab lightstone_tile_stairs logic_block logic_block_invis
                machine_blast_furnace machine_crucible machine_difurnace_off meteor_battery meteor_brick meteor_brick_chiseled
                meteor_brick_cracked meteor_brick_mossy meteor_pillar meteor_polished meteor_spawner ore_alexandrite
                ore_aluminium ore_aluminium_deepslate ore_asbestos ore_asbestos_deepslate ore_australium ore_basalt_asbestos
                ore_basalt_fluorite ore_basalt_gem ore_basalt_molysite ore_basalt_sulfur ore_bedrock_block ore_bedrock_oil
                ore_beryllium ore_beryllium_deepslate ore_cinnabar ore_cinnabar_deepslate ore_cobalt ore_cobalt_deepslate
                ore_coltan ore_coltan_deepslate ore_depth_borax ore_depth_cinnabar ore_depth_nether_neodymium
                ore_depth_nether_nitan ore_depth_zirconium ore_fluorite ore_fluorite_deepslate ore_gneiss_asbestos
                ore_gneiss_copper ore_gneiss_gas ore_gneiss_gold ore_gneiss_iron ore_gneiss_lithium ore_gneiss_rare
                ore_gneiss_schrabidium ore_gneiss_uranium ore_gneiss_uranium_scorched ore_lead ore_lead_deepslate ore_lignite
                ore_lignite_deepslate ore_meteor_aluminium ore_meteor_cobalt ore_meteor_copper ore_meteor_iron
                ore_meteor_rareearth ore_nether_coal ore_nether_cobalt ore_nether_fire ore_nether_plutonium
                ore_nether_schrabidium ore_nether_smoldering ore_nether_sulfur ore_nether_tungsten ore_nether_uranium
                ore_nether_uranium_scorched ore_niter ore_niter_deepslate ore_oil ore_oil_empty ore_rare ore_rare_deepslate
                ore_schrabidium ore_sellafield_diamond ore_sellafield_emerald ore_sellafield_radgem ore_sellafield_schrabidium
                ore_sellafield_uranium_scorched ore_sulfur ore_sulfur_deepslate ore_thorium ore_thorium_deepslate ore_tikite
                ore_titanium ore_titanium_deepslate ore_tungsten ore_tungsten_deepslate ore_uranium ore_uranium_deepslate
                ore_uranium_scorched ore_volcano pedestal pile_brick reinforced_brick reinforced_brick_slab
                reinforced_brick_stairs reinforced_ducrete reinforced_glass reinforced_glass_pane reinforced_laminate
                reinforced_laminate_pane reinforced_lamp_off reinforced_lamp_on reinforced_light reinforced_sand
                reinforced_sand_slab reinforced_sand_stairs reinforced_stone reinforced_stone_slab reinforced_stone_stairs
                sellafield_0 sellafield_1 sellafield_2 sellafield_3 sellafield_4 sellafield_5 sellafield_bedrock
                sellafield_slaked skeleton_holder stalactite_asbestos stalactite_glyphid1 stalactite_glyphid2
                stalactite_glyphid3 stalactite_ice stalactite_snow stalactite_sulfur stalagmite_asbestos stalagmite_glyphid1
                stalagmite_glyphid2 stalagmite_glyphid3 stalagmite_ice stalagmite_snow stalagmite_sulfur stone_biome_desert
                stone_biome_woodland stone_cracked stone_depth stone_depth_nether stone_gneiss stone_keyhole stone_keyhole_meta
                stone_porous stone_resource_asbestos stone_resource_bauxite stone_resource_hematite stone_resource_limestone
                stone_resource_malachite stone_resource_sulfur tile_lab tile_lab_broken tile_lab_broken_slab
                tile_lab_broken_stairs tile_lab_cracked tile_lab_cracked_slab tile_lab_cracked_stairs tile_lab_slab
                tile_lab_stairs vinyl_tile_large vinyl_tile_small
                """);
        put(MapColor.NONE, """
                balefire block_tritium chlorine_gas digamma_matter drone_waypoint drone_waypoint_request fire_digamma
                floodlight_beam gas_asbestos gas_coal gas_explosive gas_flammable gas_meltdown gas_monoxide gas_radon
                gas_radon_dense gas_radon_tomb glass_ash glass_boron glass_lead glass_polarized glass_polonium glass_trinitite
                glass_uranium hadron_analysis_glass hadron_plating_glass ladder_aluminium ladder_cobalt ladder_copper
                ladder_gold ladder_iron ladder_lead ladder_red ladder_red_top ladder_steel ladder_sturdy ladder_titanium
                ladder_tungsten lamp_tritium_blue_off lamp_tritium_blue_on lamp_tritium_green_off lamp_tritium_green_on
                radio_torch_controller radio_torch_counter radio_torch_logic radio_torch_reader radio_torch_receiver
                radio_torch_sender rail_booster rail_highspeed rail_narrow rail_wood snowglobe spotlight_beam spotlight_fluoro
                spotlight_fluoro_off spotlight_halogen spotlight_halogen_off spotlight_incandescent spotlight_incandescent_off
                wand_air
                """);
        put(MapColor.PLANT, """
                glyphid_base glyphid_spawner leaves_layer mush mush_block mush_block_stem plant_dead_bigflower plant_dead_fern
                plant_dead_flower plant_dead_generic plant_dead_grass plant_flower_cd0 plant_flower_cd1 plant_flower_foxglove
                plant_flower_nightshade plant_flower_tobacco plant_flower_weed plant_reeds plant_tall_cd2 plant_tall_cd3
                plant_tall_cd4 plant_tall_weed vine_phosphor waste_leaves
                """);
        put(MapColor.SAND, """
                ash_digamma barricade block_fallout block_red_phosphorus block_scrap block_yellowcake fallout gravel_diamond
                moon_turf ore_oil_sand ore_tektite_osmiridium sand_boron sand_boron_layer sand_dirty sand_dirty_red sand_gold
                sand_lead sand_polonium sand_quartz sand_uranium tektite waste_trinitite waste_trinitite_red
                """);
        put(MapColor.WOOD, """
                crate crate_can crate_supply crate_template crate_weapon frozen_log frozen_planks pink_log pink_planks pink_slab
                pink_stairs radio_telex red_pylon_medium_wood red_pylon_medium_wood_transformer waste_log waste_planks
                wood_barrier wood_structure_ceiling wood_structure_roof wood_structure_scaffold
                """);
        put(MapColor.FIRE, """
                block_c4 block_semtex c4 charge_c4 charge_dynamite charge_miner charge_semtex dynamite fissure_bomb
                rad_lava_block semtex tnt_ntm volcanic_lava_block
                """);
        put(MapColor.DIRT, """
                burning_earth dirt_dead dirt_oily frozen_dirt frozen_grass impact_dirt ntm_dirt oil_spill sandbags waste_earth
                waste_mycelium
                """);
        put(MapColor.WOOL, """
                block_asbestos block_fiberglass block_insulator deco_asbestos plushie
                """);
        put(MapColor.SNOW, """
                block_foam foam_layer
                """);
        put(MapColor.COLOR_PURPLE, """
                acid_block
                """);
        put(MapColor.COLOR_BROWN, """
                corium_block
                """);
        put(MapColor.ICE, """
                glass_quartz
                """);
        put(MapColor.COLOR_ORANGE, """
                mud_block
                """);
        put(MapColor.COLOR_CYAN, """
                schrabidic_block
                """);
        put(MapColor.WATER, """
                sulfuric_acid_block
                """);
        put(MapColor.COLOR_GRAY, """
                taint
                """);
        put(MapColor.COLOR_GREEN, """
                toxic_block
                """);
    }

    private MapColors() {}

    public static void register() {
        DeferredRegister.Blocks.defaultMapColors(Constants.MOD_ID, MapColors::of);
    }

    /** The colour a block of this name starts with, before its own properties. */
    public static MapColor of(String name) {
        return COLORS.getOrDefault(name, MapColor.METAL);
    }

    private static void put(MapColor color, String names) {
        for (String name : names.trim().split("\\s+")) COLORS.put(name, color);
    }
}
