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

/**
 * Dejson declarations for the satellite family: the ID manager cube, the cargo landing pad (renderer-drawn, its
 * item the 1.7 {@code ItemRenderLibrary} entry), the chip sprites, drops and the 1.7 crafting-table recipes
 * ({@code CraftingManager} sat_dock / machine_satlinker / sat_coord, {@code ToolRecipes} sat_designator /
 * sat_relay). Touches no Minecraft class: it runs in a plain JVM from {@code dejson.gradle.kts}.
 */
public final class SatelliteDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private SatelliteDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        blocks();
        items();
        drops();
        recipes();
    }

    private static void blocks() {
        // 1.7 MachineSatLinker.getIcon: machine_satlinker_top on top AND bottom, _side around
        Dejson.block(NS, "machine_satlinker")
                .models(Models.cubeBottomTop("hbm:blocks/machine_satlinker_top", "hbm:blocks/machine_satlinker_side",
                        "hbm:blocks/machine_satlinker_top"))
                .states(Variants.simple()).blockItemDirect().register();

        Dejson.block(NS, "sat_dock").rendered("hbm:block/machine_dummy_empty")
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("cell", "false", "true"))).register();
    }

    private static void items() {
        for (String sprite : new String[]{"sat_mapper", "sat_scanner", "sat_radar", "sat_laser", "sat_foeq",
                "sat_resonator", "sat_miner", "sat_lunar_miner", "sat_gerald", "sat_chip", "sat_coord", "sat_relay"}) {
            Dejson.item(NS, sprite).models(Models.generated("hbm:items/" + sprite)).register();
        }
        // 1.7 setFull3D
        Dejson.item(NS, "sat_designator").models(Models.handheld("hbm:items/sat_designator")).register();

        // ItemRenderLibrary sat_dock: inventory S(3); common Ry(90 about -Y), renderAll
        {
            JsonObject model = new JsonObject();
            model.addProperty("type", "hbm:machine_obj");
            model.addProperty("obj", "hbm:models/sat_dock.obj");
            model.addProperty("texture", "hbm:textures/models/sat_dock.png");
            model.addProperty("scale", 1.0);
            model.addProperty("yaw", -90.0);
            model.addProperty("rawLegacyTransform", true);
            model.add("guiTranslate", numbers(0, 0, 0));
            model.addProperty("guiScale", 3.0);
            Dejson.item(NS, "sat_dock").shares("hbm:item/machine_obj_base").register();
            Dejson.itemDefinition("hbm:item/sat_dock").add("model", model);
            Dejson.itemDefinition("hbm:item/sat_dock").addProperty("oversized_in_gui", true);
        }
    }

    private static void drops() {
        Drops.self(NS, "machine_satlinker");
        Drops.self(NS, "sat_dock");
    }

    private static void recipes() {
        // 1.7 CraftingManager:485
        shaped(NS, "sat_dock", "hbm:sat_dock", rows("SSS", "PCP"),
                "S", TagIds.ingots_steel, "P", TagIds.ingots_any_plastic, "C", ItemIds.crate_iron);
        // 1.7 CraftingManager:605
        shaped(NS, "machine_satlinker", "hbm:machine_satlinker", rows("PSP", "SCS", "PSP"),
                "P", TagIds.ingots_any_resistant_alloy, "S", TagIds.ingots_any_plastic, "C", "hbm:sat_chip");
        // 1.7 CraftingManager:608
        shaped(NS, "sat_coord", "hbm:sat_coord", rows("SII", "SCA", "SPP"),
                "I", TagIds.ingots_steel, "S", TagIds.ingots_starmetal, "P", ItemIds.plate_polymer,
                "C", "hbm:sat_chip", "A", ItemIds.circuit_advanced);
        // 1.7 ToolRecipes:122-123
        shaped(NS, "sat_designator", "hbm:sat_designator", rows("RRD", "PIC", "  P"),
                "P", TagIds.plates_gold, "R", VanillaIds.redstone, "C", ItemIds.circuit_advanced,
                "D", "hbm:sat_chip", "I", TagIds.ingots_gold);
        shapeless(NS, "sat_relay", "hbm:sat_relay", "hbm:sat_chip", ItemIds.ducttape, ItemIds.radar_linker);
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }
}
