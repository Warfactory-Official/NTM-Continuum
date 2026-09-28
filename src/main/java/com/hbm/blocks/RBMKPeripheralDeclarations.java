package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Placement;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;

/**
 * Dejson declarations for the RBMK console peripherals: the six Redstone-over-Radio panels and the blank
 * panel, the autoloader and the steam connector, plus the 1.7 crafting recipes of the whole panel family
 * (CraftingManager 770-782, the gauge and terminal included). The panel slab borrows the gauge's
 * east-facing three-quarter element model with the display texture; item icons are {@code hbm:machine_obj}
 * item definitions declared here (the panels framed like blocks through {@code minecraft:special}, the
 * autoloader drawn raw as RenderRBMKAutoloader's item pass). Touches no Minecraft class.
 */
public final class RBMKPeripheralDeclarations {

    private static final String NS = "hbm";
    private static final String[] PANELS = {
            "rbmk_display_blank", "rbmk_display", "rbmk_key_pad", "rbmk_lever", "rbmk_numitron", "rbmk_graph", "rbmk_indicator"};
    private static boolean declared;

    private RBMKPeripheralDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // rbmk_gauge's element sits at x 0..12, i.e. the model faces east; the other three facings turn it.
        var eastModel = Variants.of(Set.of("facing"), Map.of(
                "facing=east", List.of(Placement.NONE),
                "facing=south", List.of(Placement.rotated(0, 90, false)),
                "facing=west", List.of(Placement.rotated(0, 180, false)),
                "facing=north", List.of(Placement.rotated(0, 270, false))));
        for (String panel : PANELS) {
            Dejson.block(NS, panel)
                    .models(Models.parented("hbm:block/rbmk_gauge",
                            Slots.of("all", "hbm:blocks/rbmk/rbmk_display", "particle", "hbm:blocks/rbmk/rbmk_display")))
                    .states(eastModel).register();
            Drops.self(NS, panel);
        }
        Dejson.block(NS, "rbmk_autoloader").rendered("hbm:block/machine_dummy_empty")
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("cell", "false", "true"))).register();
        Drops.self(NS, "rbmk_autoloader");
        items();
        Dejson.block(NS, "rbmk_loader").models(Models.cubeAll("hbm:blocks/rbmk_loader")).states(Variants.simple()).blockItem().register();
        Drops.self(NS, "rbmk_loader");

        // --- 1.7 CraftingManager 770, 774-782 ---
        shaped(NS, "rbmk_loader", "hbm:rbmk_loader", rows("SCS", "CBC", "SCS"),
                "S", TagIds.plates_steel, "C", TagIds.ingots_copper, "B", ItemIds.tank_steel);
        shaped(NS, "rbmk_display_blank", "hbm:rbmk_display_blank", 8, rows("B", "D"),
                "B", TagIds.ingots_boron, "D", ItemIds.concrete_asbestos);
        shaped(NS, "rbmk_display", "hbm:rbmk_display", rows("C", "B"),
                "C", ItemIds.crt_display, "B", "hbm:rbmk_display_blank");
        shaped(NS, "rbmk_key_pad", "hbm:rbmk_key_pad", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_sender, "B", "hbm:rbmk_display_blank", "C", ItemIds.circuit_vacuum_tube);
        shaped(NS, "rbmk_gauge", "hbm:rbmk_gauge", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_receiver, "B", "hbm:rbmk_display_blank", "C", ItemIds.circuit_vacuum_tube);
        shaped(NS, "rbmk_numitron", "hbm:rbmk_numitron", rows(" R ", "CCC", " B "),
                "R", ItemIds.radio_torch_receiver, "B", "hbm:rbmk_display_blank", "C", ItemIds.circuit_numitron);
        shaped(NS, "rbmk_graph", "hbm:rbmk_graph", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_receiver, "B", "hbm:rbmk_display_blank", "C", ItemIds.crt_display);
        shaped(NS, "rbmk_lever", "hbm:rbmk_lever", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_sender, "B", "hbm:rbmk_display_blank", "C", TagIds.ingots_copper);
        shaped(NS, "rbmk_indicator", "hbm:rbmk_indicator", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_receiver, "B", "hbm:rbmk_display_blank", "C", ItemIds.coil_tungsten);
        shaped(NS, "rbmk_terminal", "hbm:rbmk_terminal", rows("R ", "CD", "B "),
                "R", ItemIds.radio_torch_sender, "B", "hbm:rbmk_display_blank", "C", ItemIds.circuit_analog, "D", ItemIds.crt_display);
    }

    /** The OBJ each panel item draws: 1.7 RBMK panel renderers' mesh, skin and animation (graph reuses numitron). */
    private static void items() {
        panel("rbmk_display_blank", "gauge", "hbm:textures/blocks/rbmk/rbmk_display.png", "rbmk_panel");
        panel("rbmk_display", "gauge", "hbm:textures/blocks/rbmk/rbmk_display.png", "rbmk_panel");
        panel("rbmk_key_pad", "button", "hbm:textures/models/network/keypad.png", "rbmk_key_pad");
        panel("rbmk_lever", "lever", "hbm:textures/models/network/lever.png", "rbmk_lever");
        panel("rbmk_numitron", "numitron", "hbm:textures/models/network/numitron.png", "rbmk_numitron");
        panel("rbmk_graph", "numitron", "hbm:textures/models/network/numitron.png", "rbmk_numitron");
        panel("rbmk_indicator", "indicator", "hbm:textures/models/network/indicator.png", "rbmk_indicator");

        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", "hbm:models/rbmk/autoloader.obj");
        model.addProperty("texture", "hbm:textures/models/machines/rbmk_autoloader.png");
        model.addProperty("scale", 1.0);
        model.addProperty("yaw", 180.0);
        model.addProperty("rawLegacyTransform", true);
        model.add("guiTranslate", numbers(0, -6, 0));
        model.addProperty("guiScale", 1.75);
        model.add("itemTranslate", numbers(0, 0, 0));
        model.add("itemPreTranslate", numbers(0, 0, 0));
        model.addProperty("ty", 0.0);
        JsonArray parts = new JsonArray();
        parts.add("Base");
        parts.add("Piston");
        model.add("parts", parts);
        Dejson.item(NS, "rbmk_autoloader").shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/rbmk_autoloader").add("model", model);
        Dejson.itemDefinition("hbm:item/rbmk_autoloader").addProperty("oversized_in_gui", true);
    }

    private static void panel(String id, String mesh, String texture, String animation) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", "hbm:models/rbmk/" + mesh + ".obj");
        model.addProperty("texture", texture);
        model.addProperty("animation", animation);
        model.addProperty("rawLegacyTransform", true);
        model.addProperty("scale", 1.0);
        model.addProperty("yaw", 0.0);
        model.addProperty("ty", 0.0);
        JsonObject partTextures = new JsonObject();
        partTextures.addProperty("Panel", "hbm:textures/blocks/rbmk/rbmk_display.png");
        model.add("partTextures", partTextures);
        JsonObject special = new JsonObject();
        special.addProperty("type", "minecraft:special");
        special.addProperty("base", "hbm:item/machine_obj_base");
        special.add("model", model);
        Dejson.item(NS, id).shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/" + id).add("model", special);
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }
}
