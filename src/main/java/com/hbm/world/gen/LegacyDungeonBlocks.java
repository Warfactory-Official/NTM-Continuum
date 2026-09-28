package com.hbm.world.gen;

import com.hbm.world.gen.component.LegacyBlock;

/**
 * The 1.7 {@code ModBlocks} and {@code Blocks} fields the legacy hardcoded dungeons (1.7 {@code com.hbm.world.dungeon},
 * {@code world.feature.Dud}, {@code world.generator}) reference, as {@link LegacyBlock}s named by their 1.7 registry
 * names (only {@code crashed_balefire} differs from its field: 1.7 registered it as {@code tile.crashed_bomb}). The
 * nested classes carry the 1.7 names so the ported builders read exactly like 1.7's; the pair (block, metadata) they
 * write becomes a flattened state in {@link LegacyDungeonWorld#resolve}.
 *
 * <p>Each constant is one shared instance, so the builders' identity comparisons ({@code world.getBlock(..) ==
 * Blocks.chest}, the jungle rooms' {@code bl == ModBlocks.brick_jungle}) work on the virtual world as they did on 1.7's.
 */
public final class LegacyDungeonBlocks {

    private LegacyDungeonBlocks() {}

    /** 1.7 {@code com.hbm.blocks.ModBlocks}. */
    public static final class ModBlocks {
        private ModBlocks() {}

        public static final LegacyBlock ancient_scrap = LegacyBlock.hbm("ancient_scrap");
        public static final LegacyBlock barbed_wire = LegacyBlock.hbm("barbed_wire");
        public static final LegacyBlock block_lead = LegacyBlock.hbm("block_lead");
        public static final LegacyBlock block_meteor = LegacyBlock.hbm("block_meteor");
        public static final LegacyBlock block_uranium = LegacyBlock.hbm("block_uranium");
        public static final LegacyBlock bobblehead = LegacyBlock.hbm("bobblehead");
        public static final LegacyBlock brick_concrete = LegacyBlock.hbm("brick_concrete");
        public static final LegacyBlock brick_concrete_broken = LegacyBlock.hbm("brick_concrete_broken");
        public static final LegacyBlock brick_concrete_cracked = LegacyBlock.hbm("brick_concrete_cracked");
        public static final LegacyBlock brick_concrete_marked = LegacyBlock.hbm("brick_concrete_marked");
        public static final LegacyBlock brick_concrete_mossy = LegacyBlock.hbm("brick_concrete_mossy");
        public static final LegacyBlock brick_jungle = LegacyBlock.hbm("brick_jungle");
        public static final LegacyBlock brick_jungle_circle = LegacyBlock.hbm("brick_jungle_circle");
        public static final LegacyBlock brick_jungle_cracked = LegacyBlock.hbm("brick_jungle_cracked");
        public static final LegacyBlock brick_jungle_fragile = LegacyBlock.hbm("brick_jungle_fragile");
        public static final LegacyBlock brick_jungle_glyph = LegacyBlock.hbm("brick_jungle_glyph");
        public static final LegacyBlock brick_jungle_lava = LegacyBlock.hbm("brick_jungle_lava");
        public static final LegacyBlock brick_jungle_trap = LegacyBlock.hbm("brick_jungle_trap");
        public static final LegacyBlock brick_light = LegacyBlock.hbm("brick_light");
        public static final LegacyBlock brick_obsidian = LegacyBlock.hbm("brick_obsidian");
        public static final LegacyBlock cable_switch = LegacyBlock.hbm("cable_switch");
        public static final LegacyBlock capacitor_copper = LegacyBlock.hbm("capacitor_copper");
        public static final LegacyBlock concrete = LegacyBlock.hbm("concrete");
        public static final LegacyBlock concrete_pillar = LegacyBlock.hbm("concrete_pillar");
        public static final LegacyBlock concrete_smooth = LegacyBlock.hbm("concrete_smooth");
        public static final LegacyBlock crashed_balefire = LegacyBlock.hbm("crashed_bomb");  // registered as tile.crashed_bomb
        public static final LegacyBlock crate = LegacyBlock.hbm("crate");
        public static final LegacyBlock crate_ammo = LegacyBlock.hbm("crate_ammo");
        public static final LegacyBlock crate_can = LegacyBlock.hbm("crate_can");
        public static final LegacyBlock crate_jungle = LegacyBlock.hbm("crate_jungle");
        public static final LegacyBlock crate_metal = LegacyBlock.hbm("crate_metal");
        public static final LegacyBlock crate_steel = LegacyBlock.hbm("crate_steel");
        public static final LegacyBlock deco_lead = LegacyBlock.hbm("deco_lead");
        public static final LegacyBlock deco_red_copper = LegacyBlock.hbm("deco_red_copper");
        public static final LegacyBlock deco_steel = LegacyBlock.hbm("deco_steel");
        public static final LegacyBlock deco_titanium = LegacyBlock.hbm("deco_titanium");
        public static final LegacyBlock deco_tungsten = LegacyBlock.hbm("deco_tungsten");
        public static final LegacyBlock fence_metal = LegacyBlock.hbm("fence_metal");
        public static final LegacyBlock fusion_hatch = LegacyBlock.hbm("fusion_hatch");
        public static final LegacyBlock fusion_heater = LegacyBlock.hbm("fusion_heater");
        public static final LegacyBlock gas_radon_tomb = LegacyBlock.hbm("gas_radon_tomb");
        public static final LegacyBlock geiger = LegacyBlock.hbm("geiger");
        public static final LegacyBlock hadron_coil_alloy = LegacyBlock.hbm("hadron_coil_alloy");
        public static final LegacyBlock machine_centrifuge = LegacyBlock.hbm("machine_centrifuge");
        public static final LegacyBlock machine_electric_furnace_off = LegacyBlock.hbm("machine_electric_furnace_off");
        public static final LegacyBlock machine_puf6_tank = LegacyBlock.hbm("machine_puf6_tank");
        public static final LegacyBlock machine_uf6_tank = LegacyBlock.hbm("machine_uf6_tank");
        public static final LegacyBlock nuke_man = LegacyBlock.hbm("nuke_man");
        public static final LegacyBlock ore_nether_plutonium = LegacyBlock.hbm("ore_nether_plutonium");
        public static final LegacyBlock ore_schrabidium = LegacyBlock.hbm("ore_schrabidium");
        public static final LegacyBlock pole_satellite_receiver = LegacyBlock.hbm("pole_satellite_receiver");
        public static final LegacyBlock pole_top = LegacyBlock.hbm("pole_top");
        public static final LegacyBlock pwr_control = LegacyBlock.hbm("pwr_control");
        public static final LegacyBlock pwr_fuel = LegacyBlock.hbm("pwr_fuel");
        public static final LegacyBlock red_cable = LegacyBlock.hbm("red_cable");
        public static final LegacyBlock red_wire_coated = LegacyBlock.hbm("red_wire_coated");
        public static final LegacyBlock reinforced_brick = LegacyBlock.hbm("reinforced_brick");
        public static final LegacyBlock reinforced_glass = LegacyBlock.hbm("reinforced_glass");
        public static final LegacyBlock reinforced_lamp_off = LegacyBlock.hbm("reinforced_lamp_off");
        public static final LegacyBlock reinforced_light = LegacyBlock.hbm("reinforced_light");
        public static final LegacyBlock reinforced_sand = LegacyBlock.hbm("reinforced_sand");
        public static final LegacyBlock reinforced_stone = LegacyBlock.hbm("reinforced_stone");
        public static final LegacyBlock sellafield = LegacyBlock.hbm("sellafield");
        public static final LegacyBlock sellafield_slaked = LegacyBlock.hbm("sellafield_slaked");
        public static final LegacyBlock steel_poles = LegacyBlock.hbm("steel_poles");
        public static final LegacyBlock tape_recorder = LegacyBlock.hbm("tape_recorder");
        public static final LegacyBlock toxic_block = LegacyBlock.hbm("toxic_block");
        public static final LegacyBlock waste_earth = LegacyBlock.hbm("waste_earth");
        public static final LegacyBlock waste_planks = LegacyBlock.hbm("waste_planks");
        public static final LegacyBlock yellow_barrel = LegacyBlock.hbm("yellow_barrel");
    }

    /** 1.7.10 {@code net.minecraft.init.Blocks} (field name = registry name in 1.7.10). */
    public static final class Blocks {
        private Blocks() {}

        public static final LegacyBlock air = LegacyBlock.vanilla("air");
        public static final LegacyBlock bedrock = LegacyBlock.vanilla("bedrock");
        public static final LegacyBlock bookshelf = LegacyBlock.vanilla("bookshelf");
        public static final LegacyBlock chest = LegacyBlock.vanilla("chest");
        public static final LegacyBlock dirt = LegacyBlock.vanilla("dirt");
        public static final LegacyBlock double_wooden_slab = LegacyBlock.vanilla("double_wooden_slab");
        public static final LegacyBlock fence = LegacyBlock.vanilla("fence");
        public static final LegacyBlock flower_pot = LegacyBlock.vanilla("flower_pot");
        public static final LegacyBlock grass = LegacyBlock.vanilla("grass");
        public static final LegacyBlock gravel = LegacyBlock.vanilla("gravel");
        public static final LegacyBlock hardened_clay = LegacyBlock.vanilla("hardened_clay");
        public static final LegacyBlock iron_door = LegacyBlock.vanilla("iron_door");
        public static final LegacyBlock ladder = LegacyBlock.vanilla("ladder");
        public static final LegacyBlock lava = LegacyBlock.vanilla("lava");
        public static final LegacyBlock lever = LegacyBlock.vanilla("lever");
        public static final LegacyBlock mob_spawner = LegacyBlock.vanilla("mob_spawner");
        public static final LegacyBlock oak_stairs = LegacyBlock.vanilla("oak_stairs");
        public static final LegacyBlock obsidian = LegacyBlock.vanilla("obsidian");
        public static final LegacyBlock planks = LegacyBlock.vanilla("planks");
        public static final LegacyBlock redstone_block = LegacyBlock.vanilla("redstone_block");
        public static final LegacyBlock redstone_wire = LegacyBlock.vanilla("redstone_wire");
        public static final LegacyBlock sand = LegacyBlock.vanilla("sand");
        public static final LegacyBlock sandstone = LegacyBlock.vanilla("sandstone");
        public static final LegacyBlock snow_layer = LegacyBlock.vanilla("snow_layer");
        public static final LegacyBlock stained_hardened_clay = LegacyBlock.vanilla("stained_hardened_clay");
        public static final LegacyBlock stone = LegacyBlock.vanilla("stone");
        public static final LegacyBlock stone_brick_stairs = LegacyBlock.vanilla("stone_brick_stairs");
        public static final LegacyBlock stone_stairs = LegacyBlock.vanilla("stone_stairs");
        public static final LegacyBlock stonebrick = LegacyBlock.vanilla("stonebrick");
        public static final LegacyBlock torch = LegacyBlock.vanilla("torch");
        public static final LegacyBlock unpowered_repeater = LegacyBlock.vanilla("unpowered_repeater");
        public static final LegacyBlock vine = LegacyBlock.vanilla("vine");
        public static final LegacyBlock wall_sign = LegacyBlock.vanilla("wall_sign");
        public static final LegacyBlock water = LegacyBlock.vanilla("water");
        public static final LegacyBlock web = LegacyBlock.vanilla("web");
        public static final LegacyBlock wooden_door = LegacyBlock.vanilla("wooden_door");
        public static final LegacyBlock wooden_slab = LegacyBlock.vanilla("wooden_slab");
    }
}
