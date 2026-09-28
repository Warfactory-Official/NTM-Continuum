package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Tags;
import dev.golem.core.dejson.Variants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;

/**
 * Dejson declarations for the dungeon family ({@link ModDungeonBlocks}). Touches no Minecraft class: it runs in a
 * plain JVM from {@code dejson.gradle.kts}, so the trap and glyph ids are spelled out here.
 *
 * <p>1.7 icons: the forgotten bricks put their {@code _top} icon on both ends; the glyph bricks their glyph on the
 * four sides over plain enargite; the Strange Stone its keyhole on the sides over vanilla stone; the Strange Red
 * Room Brick its keyhole on the face it looks along (the Red Room brick's own rule); the pedestal the three boxes of
 * 1.7's {@code RenderPedestal}, {@code pedestal_top} on the ends and {@code pedestal_side} around; rebar the twelve
 * bars of {@code BlockRebar}'s full render; the rails the vanilla rail shapes, the high-speed and booster rails
 * wearing their straight texture on curves (1.7 registered no {@code _turned} icon for them). The skeleton holder,
 * loot pile, old lantern and invisible action block are drawn by renderers (or not at all); their items are 1.7's
 * flat icons of their block textures, as {@code RenderItem} drew a render-type -1 block.
 */
public final class DungeonDeclarations {

    private static final String NS = "hbm";
    private static final String DUMMY = "hbm:block/machine_dummy_empty";
    private static boolean declared;

    static final String[] TRAPS = {"falling_rocks", "fire", "arrow", "spikes", "mine", "web", "flaming_arrow", "pillar",
            "rad_conversion", "magic_converstion", "slowness", "weakness", "poison_dart", "zombie", "spiders"};

    private DungeonDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        blocks();
        rails();
        tags();
        recipes();
    }

    private static void cube(String id, String texture) {
        Dejson.block(NS, id).models(Models.cubeAll("hbm:blocks/" + texture)).states(Variants.simple()).blockItemDirect().register();
    }

    private static void column(String id, String end, String side) {
        Dejson.block(NS, id).models(Models.column(end, side)).states(Variants.simple()).blockItemDirect().register();
    }

    private static void blocks() {
        // forgotten dungeon: unbreakable, drops nothing
        column("brick_forgotten", "hbm:blocks/brick_forgotten_top", "hbm:blocks/brick_forgotten");
        column("brick_forgotten_bw", "hbm:blocks/brick_forgotten_bw_top", "hbm:blocks/brick_forgotten_bw");
        cube("brick_forgotten_nullstone", "playground/nullstone_demo_1_wip");
        column("brick_forgotten_hole", "hbm:blocks/brick_forgotten_top", "hbm:blocks/brick_forgotten_hole");
        column("brick_forgotten_hole_empty", "hbm:blocks/brick_forgotten_top", "hbm:blocks/brick_forgotten_hole_empty");
        cube("brick_forgotten_nullroom_wood", "nr_planks");
        cube("brick_forgotten_nullroom_stone", "nr_stone");
        column("brick_forgotten_lock", "hbm:blocks/brick_forgotten_top", "hbm:blocks/brick_forgotten_lock");
        column("brick_forgotten_lock_bw", "hbm:blocks/brick_forgotten_bw_top", "hbm:blocks/brick_forgotten_bw_lock");
        column("brick_forgotten_lock_nullstone", "hbm:blocks/playground/nullstone_demo_1_wip",
                "hbm:blocks/playground/nullstone_demo_2_wip");
        for (String id : new String[]{"brick_forgotten", "brick_forgotten_bw", "brick_forgotten_nullstone",
                "brick_forgotten_hole", "brick_forgotten_hole_empty", "brick_forgotten_nullroom_wood",
                "brick_forgotten_nullroom_stone", "brick_forgotten_lock", "brick_forgotten_lock_bw",
                "brick_forgotten_lock_nullstone"}) {
            Drops.nothing(NS, id);
        }

        // jungle temple
        cube("brick_jungle_fragile", "brick_jungle_fragile");
        Drops.nothing(NS, "brick_jungle_fragile");
        for (String id : new String[]{"brick_jungle_lava", "brick_jungle_ooze", "brick_jungle_mystic", "brick_jungle_circle"}) {
            cube(id, id);
            Drops.self(NS, id);
        }
        for (String trap : TRAPS) {
            String id = "brick_jungle_trap_" + trap;
            cube(id, "brick_jungle_trap");
            // 1.7 TrappedBrick had no damageDropped: every trap drops metadata 0, the falling-rocks trap
            Drops.only(NS, id, "hbm:brick_jungle_trap_falling_rocks");
        }
        for (int i = 0; i < 16; i++) {
            String id = "brick_jungle_glyph_" + i;
            column(id, "hbm:blocks/brick_jungle", "hbm:blocks/brick_jungle_glyph_" + i);
            Drops.self(NS, id);
        }

        // the keyholes
        column("stone_keyhole", "minecraft:block/stone", "hbm:blocks/stone_keyhole");
        // 1.7 BlockKeyhole extends BlockStone, which drops cobblestone
        Drops.only(NS, "stone_keyhole", "minecraft:cobblestone");

        Map<String, List<Placement>> keyhole = new LinkedHashMap<>();
        keyhole.put("facing=up", List.of(Placement.model("_top")));
        keyhole.put("facing=down", List.of(Placement.model("_top").plus(Placement.rotated(180, 0, false))));
        keyhole.put("facing=north", List.of(Placement.NONE));
        keyhole.put("facing=east", List.of(Placement.rotated(0, 90, false)));
        keyhole.put("facing=south", List.of(Placement.rotated(0, 180, false)));
        keyhole.put("facing=west", List.of(Placement.rotated(0, 270, false)));
        Dejson.block(NS, "stone_keyhole_meta")
                .models(Models.group()
                        .and("", Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/brick_base")
                                .and("down", "hbm:blocks/brick_base").and("up", "hbm:blocks/brick_base")
                                .and("north", "hbm:blocks/stone_keyhole_meta").and("south", "hbm:blocks/brick_base")
                                .and("west", "hbm:blocks/brick_base").and("east", "hbm:blocks/brick_base")))
                        .and("_top", Models.cubeBottomTop("hbm:blocks/brick_base", "hbm:blocks/brick_base", "hbm:blocks/brick_red_top")))
                .states(Variants.of(Set.of("facing"), keyhole))
                .blockItemDirect()
                .register();
        Drops.nothing(NS, "stone_keyhole_meta");

        // 1.7 RenderPedestal: foot 0..0.25, head 0.75..1, column 0.125..0.875 between
        Dejson.block(NS, "pedestal")
                .models(elements("hbm:blocks/pedestal_top", "hbm:blocks/pedestal_top", "hbm:blocks/pedestal_side", false,
                        box(0, 0, 0, 16, 4, 16), box(0, 12, 0, 16, 16, 16), box(2, 4, 2, 14, 12, 14)))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "pedestal");

        Dejson.block(NS, "skeleton_holder").rendered(DUMMY)
                .states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        Dejson.item(NS, "skeleton_holder").models(Models.generated("minecraft:block/soul_sand")).register();
        Drops.self(NS, "skeleton_holder");

        Dejson.block(NS, "deco_loot").rendered(DUMMY).states(Variants.simple()).register();
        Dejson.item(NS, "deco_loot").models(Models.generated("hbm:blocks/block_steel")).register();
        Drops.nothing(NS, "deco_loot");

        Dejson.block(NS, "lantern_behemoth").rendered(DUMMY)
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("cell", "false", "true"))).register();
        Dejson.item(NS, "lantern_behemoth").models(Models.generated("hbm:blocks/block_rust")).register();
        Drops.nothing(NS, "lantern_behemoth");

        cube("dungeon_spawner", "dungeon_spawner");
        Drops.nothing(NS, "dungeon_spawner");

        // both drawn by RenderLogicBlock (the invisible one by nothing); the model is their particle and item
        Dejson.block(NS, "logic_block").rendered(DUMMY).states(Variants.simple()).register();
        Dejson.item(NS, "logic_block").models(Models.cubeAll("hbm:blocks/logic_block")).register();
        Drops.self(NS, "logic_block");
        Dejson.block(NS, "logic_block_invis").rendered(DUMMY).states(Variants.simple()).register();
        Dejson.item(NS, "logic_block_invis").models(Models.generated("hbm:blocks/logic_block")).register();
        Drops.self(NS, "logic_block_invis");

        // 1.7 BlockRebar full render: four bars along each axis, a quarter block off centre, 2 px thick
        List<float[]> bars = new ArrayList<>();
        float lo = 7 - 4, hi = 9 - 4, lo2 = 7 + 4, hi2 = 9 + 4;
        float[][] offsets = {{lo, hi}, {lo2, hi2}};
        for (float[] a : offsets) for (float[] b : offsets) {
            bars.add(box(a[0], 0, b[0], a[1], 16, b[1]));
            bars.add(box(0, a[0], b[0], 16, a[1], b[1]));
            bars.add(box(a[0], b[0], 0, a[1], b[1], 16));
        }
        Dejson.block(NS, "rebar")
                .models(Models.renderType("minecraft:cutout", elements("hbm:blocks/rebar", "hbm:blocks/rebar",
                        "hbm:blocks/rebar", true, bars.toArray(new float[0][]))))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "rebar");

        Dejson.item(NS, "rebar_placer").models(Models.generated("hbm:items/rebar_placer")).register();
    }

    private static void rails() {
        rail("rail_wood", "rail_wood", "rail_wood_turned");
        rail("rail_narrow", "rail_narrow", "rail_narrow_turned");
        rail("rail_highspeed", "rail_highspeed", "rail_highspeed");
        rail("rail_booster", "rail_booster", "rail_booster");
    }

    /** The vanilla {@code rail} blockstate and models over this rail's textures, on the cutout layer. */
    private static void rail(String id, String straight, String turned) {
        Map<String, List<Placement>> shapes = new LinkedHashMap<>();
        shapes.put("shape=north_south", List.of(Placement.NONE));
        shapes.put("shape=east_west", List.of(Placement.rotated(0, 90, false)));
        shapes.put("shape=ascending_east", List.of(Placement.model("_raised_ne").plus(Placement.rotated(0, 90, false))));
        shapes.put("shape=ascending_west", List.of(Placement.model("_raised_sw").plus(Placement.rotated(0, 90, false))));
        shapes.put("shape=ascending_north", List.of(Placement.model("_raised_ne")));
        shapes.put("shape=ascending_south", List.of(Placement.model("_raised_sw")));
        shapes.put("shape=south_east", List.of(Placement.model("_corner")));
        shapes.put("shape=south_west", List.of(Placement.model("_corner").plus(Placement.rotated(0, 90, false))));
        shapes.put("shape=north_west", List.of(Placement.model("_corner").plus(Placement.rotated(0, 180, false))));
        shapes.put("shape=north_east", List.of(Placement.model("_corner").plus(Placement.rotated(0, 270, false))));
        String s = "hbm:blocks/" + straight;
        Dejson.block(NS, id)
                .models(Models.group()
                        .and("", Models.renderType("minecraft:cutout", Models.parented("minecraft:block/rail_flat", Slots.of("rail", s))))
                        .and("_corner", Models.renderType("minecraft:cutout",
                                Models.parented("minecraft:block/rail_curved", Slots.of("rail", "hbm:blocks/" + turned))))
                        .and("_raised_ne", Models.renderType("minecraft:cutout",
                                Models.parented("minecraft:block/template_rail_raised_ne", Slots.of("rail", s))))
                        .and("_raised_sw", Models.renderType("minecraft:cutout",
                                Models.parented("minecraft:block/template_rail_raised_sw", Slots.of("rail", s)))))
                .states(Variants.of(Set.of("shape", "waterlogged"), waterlogged(shapes)))
                .item(Models.generated(s))
                .register();
        Drops.self(NS, id);
    }

    /** Every rail state exists waterlogged too; the model does not care. */
    private static Map<String, List<Placement>> waterlogged(Map<String, List<Placement>> shapes) {
        Map<String, List<Placement>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<Placement>> e : shapes.entrySet()) {
            out.put(e.getKey() + ",waterlogged=false", e.getValue());
            out.put(e.getKey() + ",waterlogged=true", e.getValue());
        }
        return out;
    }

    private static void tags() {
        Tags.block("minecraft", "rails", "hbm:rail_wood", "hbm:rail_narrow", "hbm:rail_highspeed", "hbm:rail_booster");
        Tags.item("minecraft", "rails", "hbm:rail_wood", "hbm:rail_narrow", "hbm:rail_highspeed", "hbm:rail_booster");
    }

    private static void recipes() {
        // 1.7 CraftingManager
        shaped(NS, "rebar", "hbm:rebar", 8, rows("BB", "BB"), "B", "#c:bolts/steel");
        shaped(NS, "rail_wood", "hbm:rail_wood", 16, rows("S S", "SRS", "S S"),
                "S", "minecraft:stick", "R", "hbm:plant_item_rope");
        shaped(NS, "rail_narrow", "hbm:rail_narrow", 64, rows("S S", "S S", "S S"), "S", "hbm:steel_beam");
        shaped(NS, "rail_highspeed", "hbm:rail_highspeed", 16, rows("S S", "SIS", "S S"),
                "S", "#c:ingots/steel", "I", "#c:plates/iron");
        shaped(NS, "rail_booster", "hbm:rail_booster", 6, rows("S S", "CIC", "SRS"),
                "S", "#c:ingots/steel", "I", "#c:plates/iron", "R", "#c:ingots/red_copper", "C", "hbm:coil_copper");
        // 1.7 ToolRecipes
        shaped(NS, "rebar_placer", "hbm:rebar_placer", rows("RDR", "DWD", "RDR"),
                "R", "hbm:rebar", "D", "hbm:ducttape", "W", "hbm:wrench");
    }

    // ---- element models ----

    private static float[] box(float x0, float y0, float z0, float x1, float y1, float z1) {
        return new float[]{x0, y0, z0, x1, y1, z1};
    }

    /**
     * A model of plain boxes, each face mapping the part of the texture its bounds cover (1.7 RenderBlocks' UVs).
     * {@code noCull}: 1.7's {@code shouldSideBeRendered} answered true, so no face is culled against neighbours.
     */
    private static Shape elements(String bottom, String top, String side, boolean noCull, float[]... boxes) {
        return base -> {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:block/block");
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", side);
            textures.addProperty("bottom", bottom);
            textures.addProperty("top", top);
            textures.addProperty("side", side);
            model.add("textures", textures);
            JsonArray elements = new JsonArray();
            for (float[] b : boxes) {
                JsonObject el = new JsonObject();
                el.add("from", vec(b[0], b[1], b[2]));
                el.add("to", vec(b[3], b[4], b[5]));
                JsonObject faces = new JsonObject();
                faces.add("down", face("#bottom", b[0], 16 - b[5], b[3], 16 - b[2], noCull, b[1] == 0 ? "down" : null));
                faces.add("up", face("#top", b[0], b[2], b[3], b[5], noCull, b[4] == 16 ? "up" : null));
                faces.add("north", face("#side", 16 - b[3], 16 - b[4], 16 - b[0], 16 - b[1], noCull, b[2] == 0 ? "north" : null));
                faces.add("south", face("#side", b[0], 16 - b[4], b[3], 16 - b[1], noCull, b[5] == 16 ? "south" : null));
                faces.add("west", face("#side", b[2], 16 - b[4], b[5], 16 - b[1], noCull, b[0] == 0 ? "west" : null));
                faces.add("east", face("#side", 16 - b[5], 16 - b[4], 16 - b[2], 16 - b[1], noCull, b[3] == 16 ? "east" : null));
                el.add("faces", faces);
                elements.add(el);
            }
            model.add("elements", elements);
            return Map.of(base, model);
        };
    }

    private static JsonArray vec(float x, float y, float z) {
        JsonArray a = new JsonArray();
        a.add(x);
        a.add(y);
        a.add(z);
        return a;
    }

    private static JsonObject face(String texture, float u0, float v0, float u1, float v1, boolean noCull, String cull) {
        JsonObject f = new JsonObject();
        JsonArray uv = new JsonArray();
        uv.add(u0);
        uv.add(v0);
        uv.add(u1);
        uv.add(v1);
        f.add("uv", uv);
        f.addProperty("texture", texture);
        if (!noCull && cull != null) f.addProperty("cullface", cull);
        return f;
    }
}
