package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.hbm.blocks.generic.DecoPipe;
import com.hbm.blocks.machine.Floodlight;

/**
 * Dejson declarations (models, blockstates, item definitions, loot) for the decorative block family.
 * Touches no Minecraft class: it runs in a plain JVM from {@code dejson.gradle.kts}.
 *
 * <p>Authored (hand-written) files this class leans on: the four {@code template_pipe*} element models,
 * the two {@code template_chain*} models, the glass-pane multipart blockstates and models, and the
 * {@code hbm:machine_obj} item definitions of the OBJ-drawn blocks under {@code assets/hbm/items/}.
 */
public final class DecoDeclarations {

    private static final String NS = "hbm";
    private static final String DUMMY = "hbm:block/machine_dummy_empty";
    /** The 1.7 {@code PlatemetalType} enum, lowercased, in ordinal order (a plain constant: this class must not load a Minecraft class). */
    public static final String[] PLATEMETAL_COLORS = {"base", "black", "white", "red", "green", "light_gray", "blue", "purple",
            "cyan", "pink", "lime", "yellow", "light_blue", "magenta", "orange"};
    private static boolean declared;

    private DecoDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // ---- pipes: an authored octagonal template per rType, textured per colour ----
        String[][] pipeColours = {
                {"", "pipe_top", "pipe_side"},
                {"_rusted", "pipe_top_rusty", "pipe_side_rusty"},
                {"_green", "pipe_top_green", "pipe_side_green"},
                {"_green_rusted", "pipe_top_green_rusty", "pipe_side_green_rusty"},
                {"_red", "pipe_top_red", "pipe_side_red"},
                {"_marked", "pipe_top_marked", "pipe_side_marked"},
        };
        for (DecoPipe.Style style : DecoPipe.Style.values()) {
            String infix = switch (style) {
                case PLAIN -> "";
                case RIM -> "_rim";
                case QUAD -> "_quad";
                case FRAMED -> "_framed";
            };
            for (String[] colour : pipeColours) {
                String id = "deco_pipe" + infix + colour[0];
                Dejson.block(NS, id)
                        .models(Models.parented("hbm:block/" + style.template,
                                Slots.of("top", "hbm:blocks/" + colour[1], "side", "hbm:blocks/" + colour[2], "particle", "hbm:blocks/" + colour[1])))
                        .states(Variants.axis())
                        .blockItemDirect()
                        .register();
                Drops.self(NS, id);
            }
        }

        // ---- rotatable pillars ----
        pillar("block_schrabidium_cluster", "block_schrabidium_cluster_top", "block_schrabidium_cluster_side");
        pillar("block_euphemium_cluster", "block_euphemium_cluster_top", "block_euphemium_cluster_side");
        pillar("block_insulator", "block_insulator_top", "block_insulator_side");
        pillar("block_fiberglass", "block_fiberglass_top", "block_fiberglass_side");
        pillar("block_tritium", "block_tritium_top", "block_tritium_side");
        pillar("concrete_pillar", "concrete_pillar_top", "concrete_pillar_side");
        pillar("meteor_pillar", "meteor_pillar_top", "meteor_pillar");

        // ---- lamps ----
        cube("reinforced_light");
        cube("reinforced_lamp_off");
        cubeDropping("reinforced_lamp_on", "reinforced_lamp_off");
        cube("lamp_tritium_green_off");
        cubeDropping("lamp_tritium_green_on", "lamp_tritium_green_off");
        cube("lamp_tritium_blue_off");
        cubeDropping("lamp_tritium_blue_on", "lamp_tritium_blue_off");

        // ---- spotlights: renderer-drawn; the off block drops the on block (1.7 getItemDropped → getOn) ----
        Dispatch fixture = Variants.product(
                Variants.uniform("facing", "down", "up", "north", "south", "west", "east"),
                Variants.uniform("broken", "false", "true"));
        for (String light : new String[]{"incandescent", "fluoro", "halogen"}) {
            Dejson.block(NS, "spotlight_" + light).rendered(DUMMY).states(fixture).register();
            Drops.self(NS, "spotlight_" + light);
            Dejson.block(NS, "spotlight_" + light + "_off").rendered(DUMMY).states(fixture).register();
            Drops.only(NS, "spotlight_" + light + "_off", "hbm:spotlight_" + light);
        }
        Dejson.block(NS, "spotlight_beam").rendered(DUMMY)
                .states(Variants.product(Variants.uniform("color", "0", "1", "2"),
                        Variants.uniform("down", "false", "true"), Variants.uniform("up", "false", "true"),
                        Variants.uniform("north", "false", "true"), Variants.uniform("south", "false", "true"),
                        Variants.uniform("west", "false", "true"), Variants.uniform("east", "false", "true")))
                .register();
        Drops.nothing(NS, "spotlight_beam");
        // Lantern: a folded five-tall post drawn entirely by LanternRenderer, so the blockstate exists
        // only because vanilla demands one. FACING x CELL is BlockMultiblockMachine's state space.
        Dejson.block(NS, "lantern").rendered(DUMMY)
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("cell", "false", "true"))).register();
        Drops.self(NS, "lantern");
        Dejson.item(NS, "lantern").models(Models.generated("hbm:blocks/block_steel")).register();
        Dejson.block(NS, "lamp_demon").rendered(DUMMY)
                .states(Variants.uniform("facing", "down", "up", "north", "south", "west", "east")).register();
        Drops.self(NS, "lamp_demon");
        // Floodlight: renderer-drawn (facing x flip), item icon authored; its beam is a bare light block.
        Dejson.block(NS, "floodlight").rendered(DUMMY)
                .states(Variants.product(Variants.uniform("facing", "down", "up", "north", "south", "west", "east"),
                        Variants.uniform("flip", "false", "true"))).register();
        Drops.self(NS, "floodlight");
        Dejson.block(NS, "floodlight_beam").rendered(DUMMY).states(Variants.simple()).register();
        Drops.nothing(NS, "floodlight_beam");

        // ---- bricks and concrete ----
        // Red Room brick: the marked face follows the six-way facing; up/down use the "_top" icon set.
        // plain=true is 1.7 meta 6 (no marked face, brick_base all round), set only by the keyhole rooms.
        Map<String, List<Placement>> redBrick = new LinkedHashMap<>();
        redBrick.put("facing=up,plain=false", List.of(Placement.model("_top")));
        redBrick.put("facing=down,plain=false", List.of(Placement.model("_top").plus(Placement.rotated(180, 0, false))));
        redBrick.put("facing=north,plain=false", List.of(Placement.NONE));
        redBrick.put("facing=east,plain=false", List.of(Placement.rotated(0, 90, false)));
        redBrick.put("facing=south,plain=false", List.of(Placement.rotated(0, 180, false)));
        redBrick.put("facing=west,plain=false", List.of(Placement.rotated(0, 270, false)));
        for (String facing : new String[]{"up", "down", "north", "east", "south", "west"}) {
            redBrick.put("facing=" + facing + ",plain=true", List.of(Placement.model("_plain")));
        }
        Dejson.block(NS, "brick_red")
                .models(Models.group()
                        .and("", Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/brick_base")
                                .and("down", "hbm:blocks/brick_base").and("up", "hbm:blocks/brick_base")
                                .and("north", "hbm:blocks/brick_red").and("south", "hbm:blocks/brick_base")
                                .and("west", "hbm:blocks/brick_base").and("east", "hbm:blocks/brick_base")))
                        .and("_top", Models.cubeBottomTop("hbm:blocks/brick_base", "hbm:blocks/brick_base", "hbm:blocks/brick_red_top"))
                        .and("_plain", Models.cubeAll("hbm:blocks/brick_base")))
                .states(Variants.of(Set.of("facing", "plain"), redBrick))
                .blockItemDirect()
                .register();
        Drops.nothing(NS, "brick_red");

        Dejson.block(NS, "brick_concrete_marked")
                .models(Models.cubeBottomTop("hbm:blocks/brick_concrete", "hbm:blocks/brick_concrete_marked", "hbm:blocks/brick_concrete"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "brick_concrete_marked");
        cube("brick_ducrete");
        cube("reinforced_ducrete");
        cube("concrete_rebar");
        stairs("brick_ducrete_stairs", "brick_ducrete");

        // Über concrete: damage 0-9 pristine, 10-11 m0, 12-13 m1, 14 m2, 15 m3 (1.7 getIcon).
        Map<String, List<Placement>> uber = new LinkedHashMap<>();
        for (int damage = 0; damage <= 15; damage++) {
            String suffix = damage == 15 ? "_m3" : damage == 14 ? "_m2" : damage > 11 ? "_m1" : damage > 9 ? "_m0" : "";
            uber.put("damage=" + damage, List.of(Placement.model(suffix)));
        }
        Dejson.block(NS, "concrete_super")
                .models(Models.group()
                        .and("", Models.cubeAll("hbm:blocks/concrete_super"))
                        .and("_m0", Models.cubeAll("hbm:blocks/concrete_super_m0"))
                        .and("_m1", Models.cubeAll("hbm:blocks/concrete_super_m1"))
                        .and("_m2", Models.cubeAll("hbm:blocks/concrete_super_m2"))
                        .and("_m3", Models.cubeAll("hbm:blocks/concrete_super_m3")))
                .states(Variants.of(Set.of("damage"), uber))
                .blockItemDirect()
                .register();
        Drops.self(NS, "concrete_super");
        cube("concrete_super_broken");

        // ---- asphalt ----
        cube("asphalt");
        cube("asphalt_light");
        stairs("asphalt_stairs", "asphalt");
        Dejson.block(NS, "asphalt_slab").models(Models.slabAll("hbm:blocks/asphalt")).states(Variants.slab())
                .blockItemDirect().register();
        Drops.slab(NS, "asphalt_slab");

        // ---- vinyl / platemetal (the dotted 1.7 texture names are kept) ----
        cube("vinyl_tile_large", "vinyl_tile.large");
        cube("vinyl_tile_small", "vinyl_tile.small");
        for (String colour : PLATEMETAL_COLORS) {
            cube("platemetal_" + colour, "platemetal." + colour);
        }

        // ---- depth bricks: unbreakable, so no loot (like stone_depth) ----
        for (String depth : new String[]{"depth_brick", "depth_tiles", "depth_nether_brick", "depth_nether_tiles", "depth_dnt"}) {
            Dejson.block(NS, depth).models(Models.cubeAll("hbm:blocks/" + depth)).states(Variants.simple())
                    .blockItemDirect().register();
            Drops.nothing(NS, depth);
        }

        // ---- item grate: the ten authored steel_grate heights, re-skinned with the wide top ----
        Models.Group grate = Models.group();
        for (int height = 0; height <= 9; height++) {
            grate = grate.and(String.valueOf(height), Models.parented("hbm:block/steel_grate" + height,
                    Slots.of("end", "hbm:blocks/grate_wide_top", "particle", "hbm:blocks/grate_wide_top")));
        }
        Dejson.block(NS, "steel_grate_wide").models(grate)
                .states(Variants.select("height", "0", "0", "1", "1", "2", "2", "3", "3", "4", "4",
                        "5", "5", "6", "6", "7", "7", "8", "8", "9", "9"))
                .item(Models.blockItem("hbm:block/steel_grate_wide0"))
                .register();
        Drops.self(NS, "steel_grate_wide");

        // ---- panes: multipart blockstates and models are authored; only the flat item and the loot are declared ----
        Dejson.item(NS, "reinforced_glass_pane").models(Models.generated("hbm:blocks/reinforced_glass_pane")).register();
        Dejson.item(NS, "reinforced_laminate_pane").models(Models.generated("hbm:blocks/reinforced_laminate_pane")).register();
        Drops.nothing(NS, "reinforced_glass_pane"); // 1.7 doesDrop = false
        Drops.self(NS, "reinforced_laminate_pane");

        // ---- chain: hanging cross or flat wall plane, "end" icon when nothing holds it from below ----
        Map<String, List<Placement>> chain = new LinkedHashMap<>();
        String[] facings = {"north", "east", "south", "west"};
        for (int quarter = 0; quarter < facings.length; quarter++) {
            for (String end : new String[]{"false", "true"}) {
                String endSuffix = "true".equals(end) ? "_end" : "";
                chain.put("facing=" + facings[quarter] + ",wall=false,end=" + end, List.of(Placement.model(endSuffix)));
                chain.put("facing=" + facings[quarter] + ",wall=true,end=" + end,
                        List.of(Placement.model("_wall" + endSuffix).plus(Placement.rotated(0, quarter * 90, false))));
            }
        }
        Dejson.block(NS, "dungeon_chain")
                .models(Models.group()
                        .and("", Models.parented("hbm:block/template_chain", Slots.of("chain", "hbm:blocks/chain")))
                        .and("_end", Models.parented("hbm:block/template_chain", Slots.of("chain", "hbm:blocks/chain_end")))
                        .and("_wall", Models.parented("hbm:block/template_chain_wall", Slots.of("chain", "hbm:blocks/chain")))
                        .and("_wall_end", Models.parented("hbm:block/template_chain_wall", Slots.of("chain", "hbm:blocks/chain_end"))))
                .states(Variants.of(Set.of("facing", "wall", "end"), chain))
                .item(Models.generated("hbm:blocks/chain"))
                .register();
        Drops.self(NS, "dungeon_chain");

        // ---- terrain ----
        Dejson.block(NS, "ntm_dirt").models(Models.cubeAll("minecraft:block/dirt")).states(Variants.simple())
                .blockItemDirect().register();
        Drops.only(NS, "ntm_dirt", "minecraft:dirt");
        Dejson.block(NS, "impact_dirt").models(Models.cubeAll("hbm:blocks/waste_earth_bottom")).states(Variants.simple())
                .blockItemDirect().register();
        Drops.only(NS, "impact_dirt", "minecraft:dirt");
        cube("sand_uranium");
        cube("sand_polonium");

        // ---- loot crates ----
        cube("crate_weapon");
        cube("crate_lead");
        cube("crate_metal");
        cube("crate_red");
        cube("crate_jungle");
        Dejson.block(NS, "crate_ammo")
                .models(Models.cubeBottomTop("hbm:blocks/crate_ammo_bottom", "hbm:blocks/crate_ammo_side", "hbm:blocks/crate_ammo_top"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "crate_ammo");

        // ---- OBJ-drawn steel furniture: renderer-drawn, authored machine_obj item icons ----
        Dispatch horizontal = Variants.uniform("facing", "north", "south", "west", "east");
        for (String pole : new String[]{"steel_poles", "pole_top", "pole_satellite_receiver", "tape_recorder",
                "deco_toaster_iron", "deco_toaster_steel", "deco_toaster_wood",
                "deco_crt_clean", "deco_crt_broken", "deco_crt_blinking", "deco_crt_bsod", "deco_computer_ibm_300pl"}) {
            Dejson.block(NS, pole).rendered(DUMMY).states(horizontal).register();
            Drops.self(NS, pole);
        }
    }

    private static void pillar(String id, String top, String side) {
        Dejson.block(NS, id).models(Models.column("hbm:blocks/" + top, "hbm:blocks/" + side)).states(Variants.axis())
                .blockItemDirect().register();
        Drops.self(NS, id);
    }

    private static void cube(String id) {
        cube(id, id);
    }

    private static void cube(String id, String texture) {
        Dejson.block(NS, id).models(Models.cubeAll("hbm:blocks/" + texture)).states(Variants.simple())
                .blockItemDirect().register();
        Drops.self(NS, id);
    }

    /** A lit block that drops (and is picked as) its unlit twin. */
    private static void cubeDropping(String id, String drop) {
        Dejson.block(NS, id).models(Models.cubeAll("hbm:blocks/" + id)).states(Variants.simple())
                .blockItemDirect().register();
        Drops.only(NS, id, "hbm:" + drop);
    }

    private static void stairs(String id, String texture) {
        Dejson.block(NS, id).models(Models.stairsAll("hbm:blocks/" + texture)).states(Variants.stairs())
                .blockItemDirect().register();
        Drops.self(NS, id);
    }
}
