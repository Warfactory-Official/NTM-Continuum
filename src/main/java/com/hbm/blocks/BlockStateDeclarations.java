package com.hbm.blocks;

import dev.golem.core.dejson.States;
import dev.golem.core.dejson.Variant;
import dev.golem.core.dejson.When;

/**
 * Blockstates that are not one block model with suffixes and quarter turns: the multipart pipes,
 * cables, ducts, fences and foundry pieces that assemble themselves per connected side, and the
 * variant maps whose states point at unrelated models.
 *
 * <p>Touches no Minecraft class: Dejson runs {@link #declare()} in a plain JVM at build time.
 */
public final class BlockStateDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private BlockStateDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        part0();
        part1();
    }

    private static void part0() {
        States.multipart(NS, "conveyor_chute")
                .part(When.of("feeds", "false", "facing", "north"), v("hbm:block/conveyor_chute_belt"))
                .part(When.of("feeds", "false", "facing", "east"), v("hbm:block/conveyor_chute_belt", 0, 90))
                .part(When.of("feeds", "false", "facing", "south"), v("hbm:block/conveyor_chute_belt", 0, 180))
                .part(When.of("feeds", "false", "facing", "west"), v("hbm:block/conveyor_chute_belt", 0, 270))
                .part(When.of("feeds", "true", "north", "true"), v("hbm:block/conveyor_chute_stub_north"))
                .part(When.of("feeds", "true", "south", "true"), v("hbm:block/conveyor_chute_stub_south"))
                .part(When.of("feeds", "true", "west", "true"), v("hbm:block/conveyor_chute_stub_west"))
                .part(When.of("feeds", "true", "east", "true"), v("hbm:block/conveyor_chute_stub_east"))
                .part(v("hbm:block/conveyor_chute_frame"))
                .part(When.of("north", "false", "feeds", "true"), v("hbm:block/conveyor_chute_grate"))
                .part(When.of("north", "false", "feeds", "false", "facing", "east|south|west"), v("hbm:block/conveyor_chute_grate_short"))
                .part(When.of("east", "false", "feeds", "true"), v("hbm:block/conveyor_chute_grate", 0, 90))
                .part(When.of("east", "false", "feeds", "false", "facing", "north|south|west"), v("hbm:block/conveyor_chute_grate_short", 0, 90))
                .part(When.of("south", "false", "feeds", "true"), v("hbm:block/conveyor_chute_grate", 0, 180))
                .part(When.of("south", "false", "feeds", "false", "facing", "north|east|west"), v("hbm:block/conveyor_chute_grate_short", 0, 180))
                .part(When.of("west", "false", "feeds", "true"), v("hbm:block/conveyor_chute_grate", 0, 270))
                .part(When.of("west", "false", "feeds", "false", "facing", "north|east|south"), v("hbm:block/conveyor_chute_grate_short", 0, 270))
                .register();
        States.multipart(NS, "fence_metal")
                .part(When.or(When.of("north", "true", "south", "false"), When.of("north", "false", "south", "true"), When.of("west", "true", "east", "false"), When.of("west", "false", "east", "true"), When.of("north", "false", "south", "false", "west", "false", "east", "false"), When.of("north", "true", "south", "true", "west", "true", "east", "true")), v("hbm:block/fence_metal_post"))
                .part(When.of("north", "true"), v("hbm:block/fence_metal_side", 0, 0, true))
                .part(When.of("east", "true"), v("hbm:block/fence_metal_side", 0, 90, true))
                .part(When.of("south", "true"), v("hbm:block/fence_metal_side", 0, 180, true))
                .part(When.of("west", "true"), v("hbm:block/fence_metal_side", 0, 270, true))
                .register();
        States.multipart(NS, "fence_metal_post")
                .part(v("hbm:block/fence_metal_post"))
                .part(When.of("north", "true"), v("hbm:block/fence_metal_side", 0, 0, true))
                .part(When.of("east", "true"), v("hbm:block/fence_metal_side", 0, 90, true))
                .part(When.of("south", "true"), v("hbm:block/fence_metal_side", 0, 180, true))
                .part(When.of("west", "true"), v("hbm:block/fence_metal_side", 0, 270, true))
                .register();
        States.multipart(NS, "fluid_duct_box_copper_0")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_copper_1")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_copper_2")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_copper_3")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_copper_4")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_silver_0")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_silver_1")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_silver_2")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_silver_3")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_silver_4")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_white_0")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_white_1")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_white_2")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_white_3")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_box_white_4")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_exhaust_0")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_exhaust_1")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_exhaust_2")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_exhaust_3")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_exhaust_4")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_gauge")
                .part(v("hbm:block/fluid_duct_gauge"))
                .register();
        States.multipart(NS, "fluid_duct_mk2")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_neo")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_neo_colored")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_neo_silver")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "fluid_duct_paintable")
                .part(v("hbm:block/fluid_duct_paintable"))
                .register();
        States.multipart(NS, "fluid_duct_paintable_block_exhaust")
                .part(v("hbm:block/fluid_duct_paintable_block_exhaust"))
                .register();
    }

    private static void part1() {
        States.variants(NS, "foam_layer")
                .variant("layers=1", v("hbm:block/foam_layer_height2"))
                .variant("layers=2", v("hbm:block/foam_layer_height4"))
                .variant("layers=3", v("hbm:block/foam_layer_height6"))
                .variant("layers=4", v("hbm:block/foam_layer_height8"))
                .variant("layers=5", v("hbm:block/foam_layer_height10"))
                .variant("layers=6", v("hbm:block/foam_layer_height12"))
                .variant("layers=7", v("hbm:block/foam_layer_height14"))
                .variant("layers=8", v("hbm:block/block_foam"))
                .register();
        States.variants(NS, "foundry_channel")
                .variant("meta=0", v("hbm:block/foundry_channel_center"))
                .variant("meta=1", v("hbm:block/foundry_channel_side", 0, 90))
                .variant("meta=2", v("hbm:block/foundry_channel_side", 0, 270))
                .variant("meta=3", v("hbm:block/foundry_channel_straight"))
                .variant("meta=4", v("hbm:block/foundry_channel_side", 0, 180))
                .variant("meta=5", v("hbm:block/foundry_channel_corner", 0, 90))
                .variant("meta=6", v("hbm:block/foundry_channel_corner", 0, 180))
                .variant("meta=7", v("hbm:block/foundry_channel_t"))
                .variant("meta=8", v("hbm:block/foundry_channel_side"))
                .variant("meta=9", v("hbm:block/foundry_channel_corner"))
                .variant("meta=10", v("hbm:block/foundry_channel_corner", 0, 270))
                .variant("meta=11", v("hbm:block/foundry_channel_t", 0, 180))
                .variant("meta=12", v("hbm:block/foundry_channel_straight", 0, 90))
                .variant("meta=13", v("hbm:block/foundry_channel_t", 0, 270))
                .variant("meta=14", v("hbm:block/foundry_channel_t", 0, 90))
                .variant("meta=15", v("hbm:block/foundry_channel_cross"))
                .register();
        States.multipart(NS, "foundry_tank")
                .part(When.of("north", "false", "down", "false", "out_north", "false", "up", "false"), v("hbm:block/foundry_tank_wall_side_inner"))
                .part(When.of("north", "false", "down", "false", "out_north", "false", "up", "true"), v("hbm:block/foundry_tank_wall_side_bottom"))
                .part(When.of("north", "false", "down", "false", "out_north", "true", "up", "false"), v("hbm:block/foundry_tank_wall_side_outlet_inner"))
                .part(When.of("north", "false", "down", "false", "out_north", "true", "up", "true"), v("hbm:block/foundry_tank_wall_side_outlet_bottom"))
                .part(When.of("north", "false", "down", "true", "out_north", "false", "up", "false"), v("hbm:block/foundry_tank_wall_upper_inner"))
                .part(When.of("north", "false", "down", "true", "out_north", "false", "up", "true"), v("hbm:block/foundry_tank_wall_upper_bottom"))
                .part(When.of("north", "false", "down", "true", "out_north", "true", "up", "false"), v("hbm:block/foundry_tank_wall_upper_outlet_inner"))
                .part(When.of("north", "false", "down", "true", "out_north", "true", "up", "true"), v("hbm:block/foundry_tank_wall_upper_outlet_bottom"))
                .part(When.of("east", "false", "down", "false", "out_east", "false", "up", "false"), v("hbm:block/foundry_tank_wall_side_inner", 0, 90))
                .part(When.of("east", "false", "down", "false", "out_east", "false", "up", "true"), v("hbm:block/foundry_tank_wall_side_bottom", 0, 90))
                .part(When.of("east", "false", "down", "false", "out_east", "true", "up", "false"), v("hbm:block/foundry_tank_wall_side_outlet_inner", 0, 90))
                .part(When.of("east", "false", "down", "false", "out_east", "true", "up", "true"), v("hbm:block/foundry_tank_wall_side_outlet_bottom", 0, 90))
                .part(When.of("east", "false", "down", "true", "out_east", "false", "up", "false"), v("hbm:block/foundry_tank_wall_upper_inner", 0, 90))
                .part(When.of("east", "false", "down", "true", "out_east", "false", "up", "true"), v("hbm:block/foundry_tank_wall_upper_bottom", 0, 90))
                .part(When.of("east", "false", "down", "true", "out_east", "true", "up", "false"), v("hbm:block/foundry_tank_wall_upper_outlet_inner", 0, 90))
                .part(When.of("east", "false", "down", "true", "out_east", "true", "up", "true"), v("hbm:block/foundry_tank_wall_upper_outlet_bottom", 0, 90))
                .part(When.of("south", "false", "down", "false", "out_south", "false", "up", "false"), v("hbm:block/foundry_tank_wall_side_inner", 0, 180))
                .part(When.of("south", "false", "down", "false", "out_south", "false", "up", "true"), v("hbm:block/foundry_tank_wall_side_bottom", 0, 180))
                .part(When.of("south", "false", "down", "false", "out_south", "true", "up", "false"), v("hbm:block/foundry_tank_wall_side_outlet_inner", 0, 180))
                .part(When.of("south", "false", "down", "false", "out_south", "true", "up", "true"), v("hbm:block/foundry_tank_wall_side_outlet_bottom", 0, 180))
                .part(When.of("south", "false", "down", "true", "out_south", "false", "up", "false"), v("hbm:block/foundry_tank_wall_upper_inner", 0, 180))
                .part(When.of("south", "false", "down", "true", "out_south", "false", "up", "true"), v("hbm:block/foundry_tank_wall_upper_bottom", 0, 180))
                .part(When.of("south", "false", "down", "true", "out_south", "true", "up", "false"), v("hbm:block/foundry_tank_wall_upper_outlet_inner", 0, 180))
                .part(When.of("south", "false", "down", "true", "out_south", "true", "up", "true"), v("hbm:block/foundry_tank_wall_upper_outlet_bottom", 0, 180))
                .part(When.of("west", "false", "down", "false", "out_west", "false", "up", "false"), v("hbm:block/foundry_tank_wall_side_inner", 0, 270))
                .part(When.of("west", "false", "down", "false", "out_west", "false", "up", "true"), v("hbm:block/foundry_tank_wall_side_bottom", 0, 270))
                .part(When.of("west", "false", "down", "false", "out_west", "true", "up", "false"), v("hbm:block/foundry_tank_wall_side_outlet_inner", 0, 270))
                .part(When.of("west", "false", "down", "false", "out_west", "true", "up", "true"), v("hbm:block/foundry_tank_wall_side_outlet_bottom", 0, 270))
                .part(When.of("west", "false", "down", "true", "out_west", "false", "up", "false"), v("hbm:block/foundry_tank_wall_upper_inner", 0, 270))
                .part(When.of("west", "false", "down", "true", "out_west", "false", "up", "true"), v("hbm:block/foundry_tank_wall_upper_bottom", 0, 270))
                .part(When.of("west", "false", "down", "true", "out_west", "true", "up", "false"), v("hbm:block/foundry_tank_wall_upper_outlet_inner", 0, 270))
                .part(When.of("west", "false", "down", "true", "out_west", "true", "up", "true"), v("hbm:block/foundry_tank_wall_upper_outlet_bottom", 0, 270))
                .part(When.of("down", "false"), v("hbm:block/foundry_tank_floor"))
                .register();
        States.variants(NS, "machine_difurnace_off")
                .variant("facing=north,lit=false,ext=false", v("hbm:block/machine_difurnace_off"))
                .variant("facing=east,lit=false,ext=false", v("hbm:block/machine_difurnace_off", 0, 90))
                .variant("facing=south,lit=false,ext=false", v("hbm:block/machine_difurnace_off", 0, 180))
                .variant("facing=west,lit=false,ext=false", v("hbm:block/machine_difurnace_off", 0, 270))
                .variant("facing=north,lit=true,ext=false", v("hbm:block/machine_difurnace_on"))
                .variant("facing=east,lit=true,ext=false", v("hbm:block/machine_difurnace_on", 0, 90))
                .variant("facing=south,lit=true,ext=false", v("hbm:block/machine_difurnace_on", 0, 180))
                .variant("facing=west,lit=true,ext=false", v("hbm:block/machine_difurnace_on", 0, 270))
                .variant("facing=north,lit=false,ext=true", v("hbm:block/machine_difurnace_off_tall"))
                .variant("facing=east,lit=false,ext=true", v("hbm:block/machine_difurnace_off_tall", 0, 90))
                .variant("facing=south,lit=false,ext=true", v("hbm:block/machine_difurnace_off_tall", 0, 180))
                .variant("facing=west,lit=false,ext=true", v("hbm:block/machine_difurnace_off_tall", 0, 270))
                .variant("facing=north,lit=true,ext=true", v("hbm:block/machine_difurnace_on_tall"))
                .variant("facing=east,lit=true,ext=true", v("hbm:block/machine_difurnace_on_tall", 0, 90))
                .variant("facing=south,lit=true,ext=true", v("hbm:block/machine_difurnace_on_tall", 0, 180))
                .variant("facing=west,lit=true,ext=true", v("hbm:block/machine_difurnace_on_tall", 0, 270))
                .register();
        States.variants(NS, "machine_electric_furnace_off")
                .variant("facing=north,lit=false", v("hbm:block/machine_electric_furnace_off"))
                .variant("facing=east,lit=false", v("hbm:block/machine_electric_furnace_off", 0, 90))
                .variant("facing=south,lit=false", v("hbm:block/machine_electric_furnace_off", 0, 180))
                .variant("facing=west,lit=false", v("hbm:block/machine_electric_furnace_off", 0, 270))
                .variant("facing=north,lit=true", v("hbm:block/machine_electric_furnace_on"))
                .variant("facing=east,lit=true", v("hbm:block/machine_electric_furnace_on", 0, 90))
                .variant("facing=south,lit=true", v("hbm:block/machine_electric_furnace_on", 0, 180))
                .variant("facing=west,lit=true", v("hbm:block/machine_electric_furnace_on", 0, 270))
                .register();
        States.variants(NS, "machine_furnace_brick_off")
                .variant("facing=north,lit=false", v("hbm:block/machine_furnace_brick_off"))
                .variant("facing=east,lit=false", v("hbm:block/machine_furnace_brick_off", 0, 90))
                .variant("facing=south,lit=false", v("hbm:block/machine_furnace_brick_off", 0, 180))
                .variant("facing=west,lit=false", v("hbm:block/machine_furnace_brick_off", 0, 270))
                .variant("facing=north,lit=true", v("hbm:block/machine_furnace_brick_on"))
                .variant("facing=east,lit=true", v("hbm:block/machine_furnace_brick_on", 0, 90))
                .variant("facing=south,lit=true", v("hbm:block/machine_furnace_brick_on", 0, 180))
                .variant("facing=west,lit=true", v("hbm:block/machine_furnace_brick_on", 0, 270))
                .register();
        States.variants(NS, "machine_weapon_table")
                .variant("", v("hbm:block/machine_weapon_table"))
                .register();
        States.multipart(NS, "pipe_anchor")
                .part(When.of("facing", "down"), v("hbm:block/pipe_anchor_down"))
                .part(When.of("facing", "up"), v("hbm:block/pipe_anchor_up"))
                .part(When.of("facing", "north"), v("hbm:block/pipe_anchor_north"))
                .part(When.of("facing", "south"), v("hbm:block/pipe_anchor_south"))
                .part(When.of("facing", "west"), v("hbm:block/pipe_anchor_west"))
                .part(When.of("facing", "east"), v("hbm:block/pipe_anchor_east"))
                .register();
        States.multipart(NS, "pneumatic_tube")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "pneumatic_tube_paintable")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.variants(NS, "rbmk_control")
                .variant("lid=none,part=core", v("hbm:block/rbmk/rbmk_control_column_none"))
                .variant("lid=concrete,part=core", v("hbm:block/rbmk/rbmk_control_column_none"))
                .variant("lid=glass,part=core", v("hbm:block/rbmk/rbmk_control_column_none"))
                .variant("lid=none,part=middle", v("hbm:block/rbmk/rbmk_control_column_none"))
                .variant("lid=concrete,part=middle", v("hbm:block/rbmk/rbmk_control_column_none"))
                .variant("lid=glass,part=middle", v("hbm:block/rbmk/rbmk_control_column_none"))
                .variant("lid=none,part=top", v("hbm:block/rbmk/rbmk_control_column_top"))
                .variant("lid=concrete,part=top", v("hbm:block/rbmk/rbmk_control_column_top"))
                .variant("lid=glass,part=top", v("hbm:block/rbmk/rbmk_control_column_top"))
                .register();
        States.variants(NS, "rbmk_control_auto")
                .variant("lid=none,part=core", v("hbm:block/rbmk/rbmk_control_auto_column_none"))
                .variant("lid=concrete,part=core", v("hbm:block/rbmk/rbmk_control_auto_column_none"))
                .variant("lid=glass,part=core", v("hbm:block/rbmk/rbmk_control_auto_column_none"))
                .variant("lid=none,part=middle", v("hbm:block/rbmk/rbmk_control_auto_column_none"))
                .variant("lid=concrete,part=middle", v("hbm:block/rbmk/rbmk_control_auto_column_none"))
                .variant("lid=glass,part=middle", v("hbm:block/rbmk/rbmk_control_auto_column_none"))
                .variant("lid=none,part=top", v("hbm:block/rbmk/rbmk_control_auto_column_top"))
                .variant("lid=concrete,part=top", v("hbm:block/rbmk/rbmk_control_auto_column_top"))
                .variant("lid=glass,part=top", v("hbm:block/rbmk/rbmk_control_auto_column_top"))
                .register();
        States.variants(NS, "rbmk_control_mod")
                .variant("lid=none,part=core", v("hbm:block/rbmk/rbmk_control_mod_column_none"))
                .variant("lid=concrete,part=core", v("hbm:block/rbmk/rbmk_control_mod_column_none"))
                .variant("lid=glass,part=core", v("hbm:block/rbmk/rbmk_control_mod_column_none"))
                .variant("lid=none,part=middle", v("hbm:block/rbmk/rbmk_control_mod_column_none"))
                .variant("lid=concrete,part=middle", v("hbm:block/rbmk/rbmk_control_mod_column_none"))
                .variant("lid=glass,part=middle", v("hbm:block/rbmk/rbmk_control_mod_column_none"))
                .variant("lid=none,part=top", v("hbm:block/rbmk/rbmk_control_mod_column_top"))
                .variant("lid=concrete,part=top", v("hbm:block/rbmk/rbmk_control_mod_column_top"))
                .variant("lid=glass,part=top", v("hbm:block/rbmk/rbmk_control_mod_column_top"))
                .register();
        States.variants(NS, "rbmk_control_reasim")
                .variant("lid=none,part=core", v("hbm:block/rbmk/rbmk_control_reasim_column_none"))
                .variant("lid=concrete,part=core", v("hbm:block/rbmk/rbmk_control_reasim_column_none"))
                .variant("lid=glass,part=core", v("hbm:block/rbmk/rbmk_control_reasim_column_none"))
                .variant("lid=none,part=middle", v("hbm:block/rbmk/rbmk_control_reasim_column_none"))
                .variant("lid=concrete,part=middle", v("hbm:block/rbmk/rbmk_control_reasim_column_none"))
                .variant("lid=glass,part=middle", v("hbm:block/rbmk/rbmk_control_reasim_column_none"))
                .variant("lid=none,part=top", v("hbm:block/rbmk/rbmk_control_reasim_column_top"))
                .variant("lid=concrete,part=top", v("hbm:block/rbmk/rbmk_control_reasim_column_top"))
                .variant("lid=glass,part=top", v("hbm:block/rbmk/rbmk_control_reasim_column_top"))
                .register();
        States.variants(NS, "rbmk_control_reasim_auto")
                .variant("lid=none,part=core", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_none"))
                .variant("lid=concrete,part=core", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_none"))
                .variant("lid=glass,part=core", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_none"))
                .variant("lid=none,part=middle", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_none"))
                .variant("lid=concrete,part=middle", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_none"))
                .variant("lid=glass,part=middle", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_none"))
                .variant("lid=none,part=top", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_top"))
                .variant("lid=concrete,part=top", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_top"))
                .variant("lid=glass,part=top", v("hbm:block/rbmk/rbmk_control_reasim_auto_column_top"))
                .register();
        States.variants(NS, "rbmk_gauge")
                .variant("facing=north", v("hbm:block/rbmk_gauge", 0, 270))
                .variant("facing=south", v("hbm:block/rbmk_gauge", 0, 90))
                .variant("facing=west", v("hbm:block/rbmk_gauge", 0, 180))
                .variant("facing=east", v("hbm:block/rbmk_gauge"))
                .register();
        States.multipart(NS, "red_cable")
                .part(v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "red_cable_classic")
                .part(v("hbm:block/red_cable_classic_core"))
                .part(When.of("north", "true"), v("hbm:block/red_cable_classic_arm"))
                .part(When.of("east", "true"), v("hbm:block/red_cable_classic_arm", 0, 90))
                .part(When.of("south", "true"), v("hbm:block/red_cable_classic_arm", 0, 180))
                .part(When.of("west", "true"), v("hbm:block/red_cable_classic_arm", 0, 270))
                .part(When.of("up", "true"), v("hbm:block/red_cable_classic_arm", 270, 0))
                .part(When.of("down", "true"), v("hbm:block/red_cable_classic_arm", 90, 0))
                .register();
        States.variants(NS, "red_cable_paintable")
                .variant("overlay=true,painted=false", v("hbm:block/red_cable_paintable"))
                .variant("overlay=false,painted=false", v("hbm:block/red_cable_paintable_plain"))
                .variant("overlay=true,painted=true", v("hbm:block/machine_dummy_empty"))
                .variant("overlay=false,painted=true", v("hbm:block/machine_dummy_empty"))
                .register();
        States.multipart(NS, "reinforced_glass_pane")
                .part(v("hbm:block/reinforced_glass_pane_post"))
                .part(When.of("north", "true"), v("hbm:block/reinforced_glass_pane_side"))
                .part(When.of("east", "true"), v("hbm:block/reinforced_glass_pane_side", 0, 90))
                .part(When.of("south", "true"), v("hbm:block/reinforced_glass_pane_side_alt"))
                .part(When.of("west", "true"), v("hbm:block/reinforced_glass_pane_side_alt", 0, 90))
                .part(When.of("north", "false"), v("hbm:block/reinforced_glass_pane_noside"))
                .part(When.of("east", "false"), v("hbm:block/reinforced_glass_pane_noside_alt"))
                .part(When.of("south", "false"), v("hbm:block/reinforced_glass_pane_noside_alt", 0, 90))
                .part(When.of("west", "false"), v("hbm:block/reinforced_glass_pane_noside", 0, 270))
                .register();
        States.multipart(NS, "reinforced_laminate_pane")
                .part(v("hbm:block/reinforced_laminate_pane_post"))
                .part(When.of("north", "true"), v("hbm:block/reinforced_laminate_pane_side"))
                .part(When.of("east", "true"), v("hbm:block/reinforced_laminate_pane_side", 0, 90))
                .part(When.of("south", "true"), v("hbm:block/reinforced_laminate_pane_side_alt"))
                .part(When.of("west", "true"), v("hbm:block/reinforced_laminate_pane_side_alt", 0, 90))
                .part(When.of("north", "false"), v("hbm:block/reinforced_laminate_pane_noside"))
                .part(When.of("east", "false"), v("hbm:block/reinforced_laminate_pane_noside_alt"))
                .part(When.of("south", "false"), v("hbm:block/reinforced_laminate_pane_noside_alt", 0, 90))
                .part(When.of("west", "false"), v("hbm:block/reinforced_laminate_pane_noside", 0, 270))
                .register();
        States.variants(NS, "sand_boron_layer")
                .variant("layers=1", v("hbm:block/sand_boron_layer_height2"))
                .variant("layers=2", v("hbm:block/sand_boron_layer_height4"))
                .variant("layers=3", v("hbm:block/sand_boron_layer_height6"))
                .variant("layers=4", v("hbm:block/sand_boron_layer_height8"))
                .variant("layers=5", v("hbm:block/sand_boron_layer_height10"))
                .variant("layers=6", v("hbm:block/sand_boron_layer_height12"))
                .variant("layers=7", v("hbm:block/sand_boron_layer_height14"))
                .variant("layers=8", v("hbm:block/sand_boron"))
                .register();
        States.multipart(NS, "wood_barrier")
                .part(When.of("west", "true"), v("hbm:block/wood_barrier_west_post"))
                .part(When.of("west", "true"), v("hbm:block/wood_barrier_west_rail"))
                .part(When.of("west", "true", "north", "false"), v("hbm:block/wood_barrier_west_cap_north"))
                .part(When.of("west", "true", "south", "false"), v("hbm:block/wood_barrier_west_cap_south"))
                .part(When.of("east", "true"), v("hbm:block/wood_barrier_east_post"))
                .part(When.of("east", "true"), v("hbm:block/wood_barrier_east_rail"))
                .part(When.of("east", "true", "north", "false"), v("hbm:block/wood_barrier_east_cap_north"))
                .part(When.of("east", "true", "south", "false"), v("hbm:block/wood_barrier_east_cap_south"))
                .part(When.of("north", "true"), v("hbm:block/wood_barrier_north_post"))
                .part(When.of("north", "true"), v("hbm:block/wood_barrier_north_rail"))
                .part(When.of("north", "true", "west", "false"), v("hbm:block/wood_barrier_north_cap_west"))
                .part(When.of("north", "true", "east", "false"), v("hbm:block/wood_barrier_north_cap_east"))
                .part(When.of("south", "true"), v("hbm:block/wood_barrier_south_post"))
                .part(When.of("south", "true"), v("hbm:block/wood_barrier_south_rail"))
                .part(When.of("south", "true", "west", "false"), v("hbm:block/wood_barrier_south_cap_west"))
                .part(When.of("south", "true", "east", "false"), v("hbm:block/wood_barrier_south_cap_east"))
                .part(When.of("up", "true"), v("hbm:block/wood_barrier_top"))
                .register();
        States.multipart(NS, "wood_structure_ceiling")
                .part(v("hbm:block/wood_structure_ceiling"))
                .register();
        States.multipart(NS, "wood_structure_roof")
                .part(v("hbm:block/wood_structure_roof_base"))
                .part(When.of("west", "true"), v("hbm:block/wood_structure_roof_west"))
                .part(When.of("east", "true"), v("hbm:block/wood_structure_roof_east"))
                .register();
        States.multipart(NS, "wood_structure_scaffold")
                .part(v("hbm:block/wood_structure_scaffold_posts"))
                .part(When.of("up", "true"), v("hbm:block/wood_structure_scaffold_post_tops"))
                .part(When.of("up", "false"), v("hbm:block/wood_structure_scaffold_top"))
                .part(When.of("west", "false"), v("hbm:block/wood_structure_scaffold_west"))
                .part(When.of("east", "false"), v("hbm:block/wood_structure_scaffold_east"))
                .part(When.of("north", "false"), v("hbm:block/wood_structure_scaffold_north"))
                .part(When.of("south", "false"), v("hbm:block/wood_structure_scaffold_south"))
                .register();
        States.variants(NS, "zirnox_destroyed")
                .variant("facing=north,cell=false", v("hbm:block/zirnox_destroyed"))
                .variant("facing=north,cell=true", v("hbm:block/zirnox_destroyed"))
                .variant("facing=east,cell=false", v("hbm:block/zirnox_destroyed"))
                .variant("facing=east,cell=true", v("hbm:block/zirnox_destroyed"))
                .variant("facing=south,cell=false", v("hbm:block/zirnox_destroyed"))
                .variant("facing=south,cell=true", v("hbm:block/zirnox_destroyed"))
                .variant("facing=west,cell=false", v("hbm:block/zirnox_destroyed"))
                .variant("facing=west,cell=true", v("hbm:block/zirnox_destroyed"))
                .register();
    }

    private static Variant v(String model) {
        return new Variant(model);
    }

    private static Variant v(String model, int x, int y) {
        return new Variant(model, x, y, false);
    }

    private static Variant v(String model, int x, int y, boolean uvlock) {
        return new Variant(model, x, y, uvlock);
    }
}
