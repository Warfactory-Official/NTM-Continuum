package com.hbm.world.gen.component;

/**
 * The 1.7 {@code ModBlocks} and {@code Blocks} fields the hardcoded structure builders reference, as {@link LegacyBlock}s
 * named by their 1.7 registry names (the field name where it differs is noted), transcribed from 1.7
 * {@code ModBlocks.java}'s {@code setBlockName} calls; field names are kept so the ported builders read like 1.7's.
 */
public final class LegacyBlocks {

    private LegacyBlocks() {}

    /** 1.7 {@code com.hbm.blocks.ModBlocks}. */
    public static final class Mod {
        private Mod() {}

        public static final LegacyBlock anvil_iron = LegacyBlock.hbm("anvil_iron");
        public static final LegacyBlock asphalt = LegacyBlock.hbm("asphalt");
        public static final LegacyBlock barbed_wire = LegacyBlock.hbm("barbed_wire");
        public static final LegacyBlock barrel_corroded = LegacyBlock.hbm("barrel_corroded");
        public static final LegacyBlock barrel_plastic = LegacyBlock.hbm("barrel_plastic");
        public static final LegacyBlock block_copper = LegacyBlock.hbm("block_copper");
        public static final LegacyBlock block_lead = LegacyBlock.hbm("block_lead");
        public static final LegacyBlock bobblehead = LegacyBlock.hbm("bobblehead");
        public static final LegacyBlock brick_concrete = LegacyBlock.hbm("brick_concrete");
        public static final LegacyBlock brick_concrete_broken = LegacyBlock.hbm("brick_concrete_broken");
        public static final LegacyBlock brick_concrete_broken_stairs = LegacyBlock.hbm("brick_concrete_broken_stairs");
        public static final LegacyBlock brick_concrete_cracked = LegacyBlock.hbm("brick_concrete_cracked");
        public static final LegacyBlock brick_concrete_cracked_stairs = LegacyBlock.hbm("brick_concrete_cracked_stairs");
        public static final LegacyBlock brick_concrete_mossy = LegacyBlock.hbm("brick_concrete_mossy");
        public static final LegacyBlock brick_concrete_mossy_stairs = LegacyBlock.hbm("brick_concrete_mossy_stairs");
        public static final LegacyBlock brick_concrete_stairs = LegacyBlock.hbm("brick_concrete_stairs");
        public static final LegacyBlock brick_light = LegacyBlock.hbm("brick_light");
        public static final LegacyBlock brick_slab = LegacyBlock.hbm("brick_slab");
        public static final LegacyBlock cable_detector = LegacyBlock.hbm("cable_detector");
        public static final LegacyBlock cable_diode = LegacyBlock.hbm("cable_diode");
        public static final LegacyBlock cable_switch = LegacyBlock.hbm("cable_switch");
        public static final LegacyBlock capacitor_copper = LegacyBlock.hbm("capacitor_copper");
        public static final LegacyBlock concrete = LegacyBlock.hbm("concrete");
        public static final LegacyBlock concrete_brick_slab = LegacyBlock.hbm("concrete_brick_slab");
        public static final LegacyBlock concrete_colored = LegacyBlock.hbm("concrete_colored");
        public static final LegacyBlock concrete_colored_ext = LegacyBlock.hbm("concrete_colored_ext");
        public static final LegacyBlock concrete_pillar = LegacyBlock.hbm("concrete_pillar");
        public static final LegacyBlock concrete_slab = LegacyBlock.hbm("concrete_slab");
        public static final LegacyBlock concrete_smooth = LegacyBlock.hbm("concrete_smooth");
        public static final LegacyBlock concrete_smooth_stairs = LegacyBlock.hbm("concrete_smooth_stairs");
        public static final LegacyBlock concrete_stairs = LegacyBlock.hbm("concrete_stairs");
        public static final LegacyBlock concrete_super = LegacyBlock.hbm("concrete_super");
        public static final LegacyBlock concrete_super_broken = LegacyBlock.hbm("concrete_super_broken");
        public static final LegacyBlock crashed_balefire = LegacyBlock.hbm("crashed_bomb");  // registered as tile.crashed_bomb
        public static final LegacyBlock crate = LegacyBlock.hbm("crate");
        public static final LegacyBlock crate_ammo = LegacyBlock.hbm("crate_ammo");
        public static final LegacyBlock crate_can = LegacyBlock.hbm("crate_can");
        public static final LegacyBlock crate_iron = LegacyBlock.hbm("crate_iron");
        public static final LegacyBlock crate_lead = LegacyBlock.hbm("crate_lead");
        public static final LegacyBlock crate_metal = LegacyBlock.hbm("crate_metal");
        public static final LegacyBlock crate_steel = LegacyBlock.hbm("crate_steel");
        public static final LegacyBlock crate_weapon = LegacyBlock.hbm("crate_weapon");
        public static final LegacyBlock deco_beryllium = LegacyBlock.hbm("deco_beryllium");
        public static final LegacyBlock deco_computer = LegacyBlock.hbm("deco_computer");
        public static final LegacyBlock deco_crt = LegacyBlock.hbm("deco_crt");
        public static final LegacyBlock deco_lead = LegacyBlock.hbm("deco_lead");
        public static final LegacyBlock deco_loot = LegacyBlock.hbm("deco_loot");
        public static final LegacyBlock deco_pipe_framed_green_rusted = LegacyBlock.hbm("deco_pipe_framed_green_rusted");
        public static final LegacyBlock deco_pipe_framed_rusted = LegacyBlock.hbm("deco_pipe_framed_rusted");
        public static final LegacyBlock deco_pipe_quad_red = LegacyBlock.hbm("deco_pipe_quad_red");
        public static final LegacyBlock deco_pipe_quad_rusted = LegacyBlock.hbm("deco_pipe_quad_rusted");
        public static final LegacyBlock deco_pipe_rim = LegacyBlock.hbm("deco_pipe_rim");
        public static final LegacyBlock deco_pipe_rim_green_rusted = LegacyBlock.hbm("deco_pipe_rim_green_rusted");
        public static final LegacyBlock deco_pipe_rim_rusted = LegacyBlock.hbm("deco_pipe_rim_rusted");
        public static final LegacyBlock deco_red_copper = LegacyBlock.hbm("deco_red_copper");
        public static final LegacyBlock deco_steel = LegacyBlock.hbm("deco_steel");
        public static final LegacyBlock deco_titanium = LegacyBlock.hbm("deco_titanium");
        public static final LegacyBlock deco_toaster = LegacyBlock.hbm("deco_toaster");
        public static final LegacyBlock deco_tungsten = LegacyBlock.hbm("deco_tungsten");
        public static final LegacyBlock door_bunker = LegacyBlock.hbm("door_bunker");
        public static final LegacyBlock door_metal = LegacyBlock.hbm("door_metal");
        public static final LegacyBlock door_office = LegacyBlock.hbm("door_office");
        public static final LegacyBlock fan = LegacyBlock.hbm("fan");
        public static final LegacyBlock fence_metal = LegacyBlock.hbm("fence_metal");
        public static final LegacyBlock filing_cabinet = LegacyBlock.hbm("filing_cabinet");
        public static final LegacyBlock fluid_duct_gauge = LegacyBlock.hbm("fluid_duct_gauge");
        public static final LegacyBlock hadron_coil_alloy = LegacyBlock.hbm("hadron_coil_alloy");
        public static final LegacyBlock hev_battery = LegacyBlock.hbm("hev_battery");
        public static final LegacyBlock ladder_steel = LegacyBlock.hbm("ladder_steel");
        public static final LegacyBlock ladder_sturdy = LegacyBlock.hbm("ladder_sturdy");
        public static final LegacyBlock launch_pad_rusted = LegacyBlock.hbm("launch_pad_rusted");
        public static final LegacyBlock lox_barrel = LegacyBlock.hbm("lox_barrel");
        public static final LegacyBlock machine_boiler_off = LegacyBlock.hbm("machine_boiler_off");
        public static final LegacyBlock machine_diesel = LegacyBlock.hbm("machine_diesel");
        public static final LegacyBlock machine_electric_furnace_off = LegacyBlock.hbm("machine_electric_furnace_off");
        public static final LegacyBlock machine_microwave = LegacyBlock.hbm("machine_microwave");
        public static final LegacyBlock machine_shredder = LegacyBlock.hbm("machine_shredder");
        public static final LegacyBlock machine_transformer = LegacyBlock.hbm("machine_transformer");
        public static final LegacyBlock machine_turbine = LegacyBlock.hbm("machine_turbine");
        public static final LegacyBlock meteor_brick = LegacyBlock.hbm("meteor_brick");
        public static final LegacyBlock meteor_brick_cracked = LegacyBlock.hbm("meteor_brick_cracked");
        public static final LegacyBlock meteor_brick_mossy = LegacyBlock.hbm("meteor_brick_mossy");
        public static final LegacyBlock meteor_polished = LegacyBlock.hbm("meteor_polished");
        public static final LegacyBlock meteor_spawner = LegacyBlock.hbm("meteor_spawner");
        public static final LegacyBlock mine_ap = LegacyBlock.hbm("mine_ap");
        public static final LegacyBlock pink_barrel = LegacyBlock.hbm("pink_barrel");
        public static final LegacyBlock plant_flower = LegacyBlock.hbm("plant_flower");
        public static final LegacyBlock pwr_channel = LegacyBlock.hbm("pwr_channel");
        public static final LegacyBlock pwr_control = LegacyBlock.hbm("pwr_control");
        public static final LegacyBlock pwr_fuel = LegacyBlock.hbm("pwr_fuel");
        public static final LegacyBlock radio_telex = LegacyBlock.hbm("radio_telex");
        public static final LegacyBlock radio_torch_receiver = LegacyBlock.hbm("radio_torch_receiver");
        public static final LegacyBlock radio_torch_sender = LegacyBlock.hbm("radio_torch_sender");
        public static final LegacyBlock radiorec = LegacyBlock.hbm("radiorec");
        public static final LegacyBlock red_barrel = LegacyBlock.hbm("red_barrel");
        public static final LegacyBlock red_cable_gauge = LegacyBlock.hbm("red_cable_gauge");
        public static final LegacyBlock reeds = LegacyBlock.hbm("plant_reeds");  // registered as tile.plant_reeds
        public static final LegacyBlock reinforced_glass = LegacyBlock.hbm("reinforced_glass");
        public static final LegacyBlock reinforced_lamp_off = LegacyBlock.hbm("reinforced_lamp_off");
        public static final LegacyBlock reinforced_sand = LegacyBlock.hbm("reinforced_sand");
        public static final LegacyBlock reinforced_stone = LegacyBlock.hbm("reinforced_stone");
        public static final LegacyBlock reinforced_stone_stairs = LegacyBlock.hbm("reinforced_stone_stairs");
        public static final LegacyBlock safe = LegacyBlock.hbm("safe");
        public static final LegacyBlock silo_hatch_large = LegacyBlock.hbm("silo_hatch_large");
        public static final LegacyBlock steel_beam = LegacyBlock.hbm("steel_beam");
        public static final LegacyBlock steel_corner = LegacyBlock.hbm("steel_corner");
        public static final LegacyBlock steel_grate = LegacyBlock.hbm("steel_grate");
        public static final LegacyBlock steel_poles = LegacyBlock.hbm("steel_poles");
        public static final LegacyBlock steel_roof = LegacyBlock.hbm("steel_roof");
        public static final LegacyBlock steel_scaffold = LegacyBlock.hbm("steel_scaffold");
        public static final LegacyBlock steel_wall = LegacyBlock.hbm("steel_wall");
        public static final LegacyBlock tape_recorder = LegacyBlock.hbm("tape_recorder");
        public static final LegacyBlock tile_lab = LegacyBlock.hbm("tile_lab");
        public static final LegacyBlock tile_lab_broken = LegacyBlock.hbm("tile_lab_broken");
        public static final LegacyBlock tile_lab_cracked = LegacyBlock.hbm("tile_lab_cracked");
        public static final LegacyBlock toxic_block = LegacyBlock.hbm("toxic_block");
        public static final LegacyBlock trapdoor_steel = LegacyBlock.hbm("trapdoor_steel");
        public static final LegacyBlock turret_howard_damaged = LegacyBlock.hbm("turret_howard_damaged");
        public static final LegacyBlock turret_sentry_damaged = LegacyBlock.hbm("turret_sentry_damaged");
        public static final LegacyBlock vinyl_tile = LegacyBlock.hbm("vinyl_tile");
        public static final LegacyBlock vitrified_barrel = LegacyBlock.hbm("vitrified_barrel");
        public static final LegacyBlock waste_planks = LegacyBlock.hbm("waste_planks");
    }

    /** 1.7.10 {@code net.minecraft.init.Blocks}. */
    public static final class Vanilla {
        private Vanilla() {}

        public static final LegacyBlock air = LegacyBlock.vanilla("air");
        public static final LegacyBlock bed = LegacyBlock.vanilla("bed");
        public static final LegacyBlock bookshelf = LegacyBlock.vanilla("bookshelf");
        public static final LegacyBlock brick_block = LegacyBlock.vanilla("brick_block");
        public static final LegacyBlock carpet = LegacyBlock.vanilla("carpet");
        public static final LegacyBlock cauldron = LegacyBlock.vanilla("cauldron");
        public static final LegacyBlock chest = LegacyBlock.vanilla("chest");
        public static final LegacyBlock cocoa = LegacyBlock.vanilla("cocoa");
        public static final LegacyBlock crafting_table = LegacyBlock.vanilla("crafting_table");
        public static final LegacyBlock dark_oak_stairs = LegacyBlock.vanilla("dark_oak_stairs");
        public static final LegacyBlock dirt = LegacyBlock.vanilla("dirt");
        public static final LegacyBlock dispenser = LegacyBlock.vanilla("dispenser");
        public static final LegacyBlock double_stone_slab = LegacyBlock.vanilla("double_stone_slab");
        public static final LegacyBlock end_portal_frame = LegacyBlock.vanilla("end_portal_frame");
        public static final LegacyBlock fence = LegacyBlock.vanilla("fence");
        public static final LegacyBlock fence_gate = LegacyBlock.vanilla("fence_gate");
        public static final LegacyBlock flower_pot = LegacyBlock.vanilla("flower_pot");
        public static final LegacyBlock furnace = LegacyBlock.vanilla("furnace");
        public static final LegacyBlock glass_pane = LegacyBlock.vanilla("glass_pane");
        public static final LegacyBlock heavy_weighted_pressure_plate = LegacyBlock.vanilla("heavy_weighted_pressure_plate");
        public static final LegacyBlock hopper = LegacyBlock.vanilla("hopper");
        public static final LegacyBlock iron_bars = LegacyBlock.vanilla("iron_bars");
        public static final LegacyBlock iron_door = LegacyBlock.vanilla("iron_door");
        public static final LegacyBlock ladder = LegacyBlock.vanilla("ladder");
        public static final LegacyBlock lever = LegacyBlock.vanilla("lever");
        public static final LegacyBlock lit_pumpkin = LegacyBlock.vanilla("lit_pumpkin");
        public static final LegacyBlock log = LegacyBlock.vanilla("log");
        public static final LegacyBlock nether_brick_stairs = LegacyBlock.vanilla("nether_brick_stairs");
        public static final LegacyBlock noteblock = LegacyBlock.vanilla("noteblock");
        public static final LegacyBlock oak_stairs = LegacyBlock.vanilla("oak_stairs");
        public static final LegacyBlock piston = LegacyBlock.vanilla("piston");
        public static final LegacyBlock planks = LegacyBlock.vanilla("planks");
        public static final LegacyBlock powered_comparator = LegacyBlock.vanilla("powered_comparator");
        public static final LegacyBlock powered_repeater = LegacyBlock.vanilla("powered_repeater");
        public static final LegacyBlock pumpkin = LegacyBlock.vanilla("pumpkin");
        public static final LegacyBlock rail = LegacyBlock.vanilla("rail");
        public static final LegacyBlock redstone_lamp = LegacyBlock.vanilla("redstone_lamp");
        public static final LegacyBlock sand = LegacyBlock.vanilla("sand");
        public static final LegacyBlock sandstone = LegacyBlock.vanilla("sandstone");
        public static final LegacyBlock sandstone_stairs = LegacyBlock.vanilla("sandstone_stairs");
        public static final LegacyBlock spruce_stairs = LegacyBlock.vanilla("spruce_stairs");
        public static final LegacyBlock sticky_piston = LegacyBlock.vanilla("sticky_piston");
        public static final LegacyBlock stone_brick_stairs = LegacyBlock.vanilla("stone_brick_stairs");
        public static final LegacyBlock stone_button = LegacyBlock.vanilla("stone_button");
        public static final LegacyBlock stone_slab = LegacyBlock.vanilla("stone_slab");
        public static final LegacyBlock stone_stairs = LegacyBlock.vanilla("stone_stairs");
        public static final LegacyBlock stonebrick = LegacyBlock.vanilla("stonebrick");
        public static final LegacyBlock trapdoor = LegacyBlock.vanilla("trapdoor");
        public static final LegacyBlock tripwire_hook = LegacyBlock.vanilla("tripwire_hook");
        public static final LegacyBlock unpowered_comparator = LegacyBlock.vanilla("unpowered_comparator");
        public static final LegacyBlock unpowered_repeater = LegacyBlock.vanilla("unpowered_repeater");
        public static final LegacyBlock water = LegacyBlock.vanilla("water");
        public static final LegacyBlock web = LegacyBlock.vanilla("web");
        public static final LegacyBlock wooden_door = LegacyBlock.vanilla("wooden_door");
        public static final LegacyBlock wooden_pressure_plate = LegacyBlock.vanilla("wooden_pressure_plate");
        public static final LegacyBlock wooden_slab = LegacyBlock.vanilla("wooden_slab");
        public static final LegacyBlock wool = LegacyBlock.vanilla("wool");
    }
}
