package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Shape;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Dejson declarations for the structure wand blocks ({@code ModStructureWandBlocks}). Touches no Minecraft class.
 *
 * <p>1.7 looks: {@code wand_air} is {@code RenderBlockWand}'s 1..15 px glass box drawn from outside and from inside
 * (the inner faces are zero-thickness elements facing in), culled against other wand blocks, and its item the flat
 * block icon; {@code wand_loot}/{@code wand_logic} carry their {@code _top} icon on both ends and turn it with the
 * 1.7 metadata ({@code IBlockSideRotation}); {@code wand_jigsaw}/{@code wand_tandem} show their front on the facing
 * side, the back opposite, {@code _top} on the ends and {@code _side} around (for an up/down facing the
 * {@code _top} icon moves to west/east, as 1.7's {@code getIcon} did); {@code wand_structure} is its save or load
 * icon on every face.
 */
public final class StructureWandDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private StructureWandDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        Dejson.block(NS, "wand_air")
                .models(Models.renderType("minecraft:cutout", wandAirModel()))
                .states(Variants.simple()).register();
        Dejson.item(NS, "wand_air").models(Models.generated("hbm:blocks/wand_air")).register();

        topRotated("wand_loot");
        topRotated("wand_logic");
        oriented("wand_jigsaw");
        oriented("wand_tandem");

        Dejson.block(NS, "wand_structure")
                .models(Models.group()
                        .and("", Models.cubeAll("hbm:blocks/wand_structure_save"))
                        .and("_load", Models.cubeAll("hbm:blocks/wand_structure_load")))
                .states(Variants.select("mode", "save", "", "load", "_load"))
                .register();
        Dejson.item(NS, "wand_structure").models(Models.group()
                        .and("", Models.blockItem("hbm:block/wand_structure"))
                        .and("_load", Models.blockItem("hbm:block/wand_structure_load")))
                .selects(NS + ":item_variant").register();

        for (String id : new String[]{"wand_air", "wand_loot", "wand_jigsaw", "wand_logic", "wand_tandem", "wand_structure"}) {
            Drops.self(NS, id);
        }
    }

    private static void topRotated(String id) {
        Map<String, List<Placement>> rotations = new LinkedHashMap<>();
        for (int r = 0; r < 4; r++) rotations.put("rotation=" + r, List.of(Placement.rotated(0, r * 90, false)));
        Dejson.block(NS, id)
                .models(Models.cubeBottomTop("hbm:blocks/" + id + "_top", "hbm:blocks/" + id, "hbm:blocks/" + id + "_top"))
                .states(Variants.of(Set.of("rotation"), rotations))
                .blockItemDirect()
                .register();
    }

    private static void oriented(String id) {
        String front = "hbm:blocks/" + id, back = front + "_back", top = front + "_top", side = front + "_side";
        Map<String, List<Placement>> facings = new LinkedHashMap<>();
        facings.put("facing=north", List.of(Placement.NONE));
        facings.put("facing=east", List.of(Placement.rotated(0, 90, false)));
        facings.put("facing=south", List.of(Placement.rotated(0, 180, false)));
        facings.put("facing=west", List.of(Placement.rotated(0, 270, false)));
        facings.put("facing=up", List.of(Placement.model("_vertical")));
        facings.put("facing=down", List.of(Placement.model("_vertical").plus(Placement.rotated(180, 0, false))));
        Dejson.block(NS, id)
                .models(Models.group()
                        .and("", Models.parented("minecraft:block/cube", Slots.of("particle", front)
                                .and("north", front).and("south", back).and("up", top).and("down", top)
                                .and("east", side).and("west", side)))
                        .and("_vertical", Models.parented("minecraft:block/cube", Slots.of("particle", front)
                                .and("up", front).and("down", back).and("north", side).and("south", side)
                                .and("east", top).and("west", top))))
                .states(Variants.of(Set.of("facing"), facings))
                .blockItemDirect()
                .register();
    }

    /** The 1..15 px box, outside faces (culled against other wand blocks) plus six inward faces. */
    private static Shape wandAirModel() {
        return base -> {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:block/block");
            JsonObject textures = new JsonObject();
            textures.addProperty("particle", "hbm:blocks/wand_air");
            textures.addProperty("all", "hbm:blocks/wand_air");
            model.add("textures", textures);
            JsonArray elements = new JsonArray();

            JsonObject outer = new JsonObject();
            outer.add("from", vec(1, 1, 1));
            outer.add("to", vec(15, 15, 15));
            JsonObject faces = new JsonObject();
            for (String dir : new String[]{"down", "up", "north", "south", "west", "east"}) faces.add(dir, face(dir));
            outer.add("faces", faces);
            elements.add(outer);

            // renderFromInside: each wall again, facing into the box
            elements.add(plane(vec(1, 1, 1), vec(15, 1, 15), "up"));
            elements.add(plane(vec(1, 15, 1), vec(15, 15, 15), "down"));
            elements.add(plane(vec(1, 1, 1), vec(15, 15, 1), "south"));
            elements.add(plane(vec(1, 1, 15), vec(15, 15, 15), "north"));
            elements.add(plane(vec(1, 1, 1), vec(1, 15, 15), "east"));
            elements.add(plane(vec(15, 1, 1), vec(15, 15, 15), "west"));

            model.add("elements", elements);
            return Map.of(base, model);
        };
    }

    private static JsonObject plane(JsonArray from, JsonArray to, String dir) {
        JsonObject el = new JsonObject();
        el.add("from", from);
        el.add("to", to);
        JsonObject faces = new JsonObject();
        JsonObject f = new JsonObject();
        f.add("uv", uv());
        f.addProperty("texture", "#all");
        faces.add(dir, f);
        el.add("faces", faces);
        return el;
    }

    private static JsonObject face(String cullface) {
        JsonObject f = new JsonObject();
        f.add("uv", uv());
        f.addProperty("texture", "#all");
        f.addProperty("cullface", cullface);
        return f;
    }

    private static JsonArray uv() {
        JsonArray uv = new JsonArray();
        uv.add(1);
        uv.add(1);
        uv.add(15);
        uv.add(15);
        return uv;
    }

    private static JsonArray vec(float x, float y, float z) {
        JsonArray v = new JsonArray();
        v.add(x);
        v.add(y);
        v.add(z);
        return v;
    }
}
