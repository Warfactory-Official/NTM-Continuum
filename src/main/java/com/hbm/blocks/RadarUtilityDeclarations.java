package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * Dejson declarations for the radar and utility machines. Touches no Minecraft class: it runs in a plain JVM
 * from {@code dejson.gradle.kts}. The family's own item ids are spelled out, since {@code ItemIds} is generated
 * from the registry after this wave.
 *
 * <p>The renderer-drawn blocks carry the empty placeholder model. Their items are 1.7's {@code ItemRenderBase}
 * entries, each with its {@code renderInventory} translate/scale as the GUI frame and its {@code renderCommon}
 * as the model transform. The large radar's inventory dish turned with the wall clock; the item model has no
 * Y-axis part spin, so the dish stands still there.
 */
public final class RadarUtilityDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private RadarUtilityDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        blocks();
        items();
        drops();
        recipes();
    }

    private static void blocks() {
        String placeholder = "hbm:block/machine_dummy_empty";
        for (String single : new String[]{"machine_radar", "machine_forcefield"}) {
            Dejson.block(NS, single).rendered(placeholder).states(Variants.simple()).register();
        }
        Dejson.block(NS, "soyuz_capsule").rendered(placeholder)
                .states(Variants.uniform("rusted", "false", "true")).register();
        for (String folded : new String[]{"machine_radar_large", "radar_screen", "machine_orbus"}) {
            Dejson.block(NS, folded).rendered(placeholder)
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).register();
        }

        // 1.7 icons: teleporter_top/_bottom/_side; the anchor's bottom is its side
        Dejson.block(NS, "machine_teleporter")
                .models(Models.cubeBottomTop("hbm:blocks/teleporter_bottom", "hbm:blocks/teleporter_side",
                        "hbm:blocks/teleporter_top"))
                .states(Variants.simple()).blockItemDirect().register();
        Dejson.block(NS, "teleanchor")
                .models(Models.cubeBottomTop("hbm:blocks/tele_anchor_side", "hbm:blocks/tele_anchor_side",
                        "hbm:blocks/tele_anchor_top"))
                .states(Variants.simple()).blockItemDirect().register();

        // 1.7 BlockSeal.getIcon: seal_frame everywhere but the one face that looks at the placer
        Dejson.block(NS, "seal_controller")
                .models(Models.orientable("hbm:blocks/seal_controller", "hbm:blocks/seal_frame", "hbm:blocks/seal_frame"))
                .states(Variants.product(Variants.facing(), Variants.uniform("powered", "false", "true")))
                .blockItemDirect().register();
        Dejson.block(NS, "seal_hatch").models(Models.cubeAll("hbm:blocks/seal_hatch_3"))
                .states(Variants.simple()).blockItemDirect().register();
    }

    private static void items() {
        for (String sprite : new String[]{"radar_linker", "linker", "anchor_remote", "upgrade_radius", "upgrade_health"}) {
            Dejson.item(NS, sprite).models(Models.generated("hbm:items/" + sprite)).register();
        }

        // ItemRenderLibrary machine_radar: inventory T(0,-4,0) S(5); Base, then T(-0.125,0,0) the dish in its own skin
        {
            JsonObject model = machine("hbm:models/machines/radar.obj", "hbm:textures/models/machines/radar_base.png");
            common(model, 1.0, 0.0, -4, 5);
            model.addProperty("noCull", true);
            model.add("parts", strings("Base"));
            model.add("translate", numbers(0, 0, 0));
            model.add("passes", array(pass("hbm:textures/models/machines/radar_dish.png", null, numbers(-0.125, 0, 0), "Dish")));
            item("machine_radar", model);
        }
        // RenderRadarLarge item: inventory T(0,-5,0) S(3); Ry(180) S(0.5), Radar then Dish
        {
            JsonObject model = machine("hbm:models/machines/radar_large.obj", "hbm:textures/models/machines/radar_large.png");
            common(model, 0.5, 180.0, -5, 3);
            model.addProperty("animation", "radar_large"); // G-C: the dish turns, 1.7 currentTimeMillis % 3600 * 0.1
            item("machine_radar_large", model);
        }
        // RenderRadarScreen item: inventory T(0,-3,0) S(5.5); T(0,0,-0.5) renderAll. The item frame centres the
        // panel on its own bounds, which absorbs the half-block shift, so it is not restated.
        {
            JsonObject model = machine("hbm:models/machines/radar_screen.obj", "hbm:textures/models/machines/radar_screen.png");
            common(model, 1.0, 0.0, -3, 5.5);
            item("radar_screen", model);
        }
        // ItemRenderLibrary machine_forcefield: inventory T(0,-4,0) S(6); the radar body in the forcefield skin,
        // then T(0,1,0) the torus
        {
            JsonObject model = machine("hbm:models/radar_base.obj", "hbm:textures/models/forcefield_base.png");
            common(model, 1.0, 0.0, -4, 6);
            model.add("parts", strings("Cube_Cube.001"));
            model.add("translate", numbers(0, 0, 0));
            model.add("passes", array(pass("hbm:textures/models/forcefield_top.png", "hbm:models/forcefield_top.obj",
                    numbers(0, 1, 0), "Torus.001")));
            item("machine_forcefield", model);
        }
        // ItemRenderLibrary machine_orbus: inventory T(0,-3,0) S(2); renderAll
        {
            JsonObject model = machine("hbm:models/machines/orbus.obj", "hbm:textures/models/machines/orbus.png");
            common(model, 1.0, 0.0, -3, 2);
            item("machine_orbus", model);
        }
        // ItemRenderLibrary soyuz_capsule: inventory T(0,-4,0) S(5); the lander's Capsule part
        {
            JsonObject model = machine("hbm:models/soyuz_lander.obj", "hbm:textures/models/soyuz_capsule/soyuz_lander.png");
            common(model, 1.0, 0.0, -4, 5);
            model.add("exclude", strings("Chute"));
            item("soyuz_capsule", model);
        }
    }

    private static void drops() {
        for (String block : new String[]{"machine_radar", "machine_radar_large", "radar_screen", "machine_forcefield",
                "machine_teleporter", "teleanchor", "seal_controller", "soyuz_capsule"}) {
            Drops.self(NS, block);
        }
        // 1.7 IPersistentNBT: a filled orbus keeps its tank in the item, as the barrels do
        JsonObject orbus = Drops.container(NS, "machine_orbus", "minecraft:custom_data", "minecraft:custom_name");
        // only the core rolls this table; a broken cell drops the core's (MachineOrbus.onPlayerWillDestroy), 1.7's
        // IPersistentNBT.getDrops through CompatExternal.getCoreFromPos from any part of the tank
        JsonObject cellProperty = new JsonObject();
        cellProperty.addProperty("cell", "false");
        JsonObject coreOnly = new JsonObject();
        coreOnly.addProperty("condition", "minecraft:block_state_property");
        coreOnly.addProperty("block", "hbm:machine_orbus");
        coreOnly.add("properties", cellProperty);
        JsonArray orbusConditions = new JsonArray();
        orbusConditions.add(coreOnly);
        orbus.getAsJsonArray("pools").get(0).getAsJsonObject().add("conditions", orbusConditions);
        Drops.nothing(NS, "seal_hatch");
    }

    private static void recipes() {
        // 1.7 CraftingManager
        shaped(NS, "teleanchor", "hbm:teleanchor", rows("ODO", "EAE", "ODO"),
                "O", VanillaIds.obsidian, "D", TagIds.gems_diamond, "E", ItemIds.powder_magic, "A", ItemIds.gem_alexandrite);
        // ItemBattery.getEmptyBattery: the crafted recall device is flat
        shapeless(NS, "anchor_remote", stack("hbm:anchor_remote").with("hbm:battery_charge", 0),
                TagIds.gems_diamond, ItemIds.ducttape, ItemIds.circuit_basic);
        shaped(NS, "radar_screen", "hbm:radar_screen", rows("PCP", "SRS", "PCP"),
                "P", TagIds.ingots_any_plastic, "C", ItemIds.circuit_basic, "S", TagIds.plates_steel, "R", ItemIds.crt_display);
        shaped(NS, "radar_linker", "hbm:radar_linker", rows("S", "C", "P"),
                "S", ItemIds.crt_display, "C", ItemIds.circuit_basic, "P", TagIds.plates_steel);
        // 1.7 ToolRecipes
        shaped(NS, "linker", "hbm:linker", rows("I I", "ICI", "GGG"),
                "I", TagIds.plates_iron, "G", TagIds.plates_gold, "C", ItemIds.circuit_advanced);
    }

    /** {@code renderCommon} scale and yaw, and {@code renderInventory}'s {@code T(0, ty, 0) S(s)}. */
    private static void common(JsonObject model, double scale, double yaw, double guiY, double guiScale) {
        model.addProperty("scale", scale);
        model.addProperty("yaw", yaw);
        model.addProperty("rawLegacyTransform", true);
        model.add("guiTranslate", numbers(0, guiY, 0));
        model.addProperty("guiScale", guiScale);
    }

    private static JsonObject pass(String texture, String obj, JsonArray translate, String... parts) {
        JsonObject pass = new JsonObject();
        if (texture != null) pass.addProperty("texture", texture);
        if (obj != null) pass.addProperty("obj", obj);
        pass.add("parts", strings(parts));
        pass.add("translate", translate);
        return pass;
    }

    private static JsonObject machine(String obj, String texture) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", obj);
        model.addProperty("texture", texture);
        return model;
    }

    private static void item(String name, JsonObject model) {
        Dejson.item(NS, name).shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/" + name).add("model", model);
        Dejson.itemDefinition("hbm:item/" + name).addProperty("oversized_in_gui", true);
    }

    private static JsonArray array(JsonObject... values) {
        JsonArray array = new JsonArray();
        for (JsonObject value : values) array.add(value);
        return array;
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }

    private static JsonArray strings(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(value);
        return array;
    }
}
