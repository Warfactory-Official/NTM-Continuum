package com.hbm.blocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tints;
import dev.golem.core.dejson.Variants;

/**
 * Dejson declarations for the ICF. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}.
 *
 * <p>1.7 icons: the vessel components use {@code icf_component} and its {@code .vessel}, {@code .vessel_welded},
 * {@code .structure} and {@code .structure_bolted} variants; the laser parts {@code icf_casing}, {@code icf_port},
 * {@code icf_cell}, {@code icf_emitter}, and for the capacitor and turbocharger their own side over
 * {@code icf_capacitor_top} on both ends; the controller {@code icf_controller} on its front over {@code icf_casing};
 * the pellet maker {@code machine_icf_press_top} on both ends. The assembled shell draws {@code icf_block} or
 * {@code icf_block_port} (1.7's connected-texture variants are not ported). The reactor itself is drawn by its
 * renderer; its item is 1.7's {@code ItemRenderBase}: inventory {@code T(0,-1.5,0) S(2.125)}, common
 * {@code S(0.5) Ry(90)}.
 */
public final class IcfDeclarations {

    private static final String NS = "hbm";
    /** 1.7 {@code ItemICFPellet.getColorFromItemStack} for a pellet with no NBT: the deuterium/tritium average. */
    private static final int DEFAULT_PELLET_TINT = 0x1414AE;
    private static boolean declared;

    private IcfDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        blocks();
        items();
    }

    private static void blocks() {
        Dejson.block(NS, "icf").rendered("hbm:block/machine_dummy_empty")
                .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                        Variants.uniform("cell", "false", "true"))).register();
        Drops.self(NS, "icf");

        Dejson.block(NS, "struct_icf_core").models(Models.cubeAll("hbm:blocks/struct_icf_core"))
                .states(Variants.uniform("facing", "north", "east", "south", "west")).blockItemDirect().register();
        Drops.self(NS, "struct_icf_core");

        Dejson.block(NS, "machine_icf_press")
                .models(Models.column("hbm:blocks/machine_icf_press_top", "hbm:blocks/machine_icf_press_side"))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, "machine_icf_press");

        Dejson.block(NS, "icf_controller")
                .models(Models.orientable("hbm:blocks/icf_controller", "hbm:blocks/icf_casing", "hbm:blocks/icf_casing"))
                .states(Variants.facing()).blockItemDirect().register();
        Drops.self(NS, "icf_controller");

        Dejson.block(NS, "icf_block")
                .models(Models.group()
                        .and("", Models.cubeAll("hbm:blocks/icf_block"))
                        .and("_port", Models.cubeAll("hbm:blocks/icf_block_port")))
                .states(Variants.flag("port", "_port"))
                .register();
        Drops.nothing(NS, "icf_block");

        cube("icf_component", "icf_component");
        cube("icf_component_vessel", "icf_component.vessel");
        cube("icf_component_vessel_welded", "icf_component.vessel_welded");
        cube("icf_component_structure", "icf_component.structure");
        cube("icf_component_structure_bolted", "icf_component.structure_bolted");

        cube("icf_laser_component_casing", "icf_casing");
        cube("icf_laser_component_port", "icf_port");
        cube("icf_laser_component_cell", "icf_cell");
        cube("icf_laser_component_emitter", "icf_emitter");
        column("icf_laser_component_capacitor", "icf_capacitor_top", "icf_capacitor_side");
        column("icf_laser_component_turbo", "icf_capacitor_top", "icf_turbocharger");
    }

    private static void cube(String id, String texture) {
        Dejson.block(NS, id).models(Models.cubeAll("hbm:blocks/" + texture))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, id);
    }

    private static void column(String id, String end, String side) {
        Dejson.block(NS, id).models(Models.column("hbm:blocks/" + end, "hbm:blocks/" + side))
                .states(Variants.simple()).blockItemDirect().register();
        Drops.self(NS, id);
    }

    private static void items() {
        // 1.7 two passes: pass 0 the tinted icf_pellet_bg, pass 1 the untinted icf_pellet shell over it
        Dejson.item(NS, "icf_pellet")
                .models(Models.generated("hbm:items/icf_pellet_bg", "hbm:items/icf_pellet"))
                .tints(Tints.customModelData(0, DEFAULT_PELLET_TINT), Tints.constant(-1))
                .register();

        JsonObject model = new JsonObject();
        model.addProperty("type", "hbm:machine_obj");
        model.addProperty("obj", "hbm:models/reactors/icf.obj");
        model.addProperty("texture", "hbm:textures/models/machines/icf.png");
        model.addProperty("scale", 0.5);
        model.addProperty("yaw", 90.0);
        model.addProperty("rawLegacyTransform", true);
        model.add("guiTranslate", numbers(0, -1.5, 0));
        model.addProperty("guiScale", 2.125);
        Dejson.item(NS, "icf").shares("hbm:item/machine_obj_base").register();
        Dejson.itemDefinition("hbm:item/icf").add("model", model);
        Dejson.itemDefinition("hbm:item/icf").addProperty("oversized_in_gui", true);
    }

    private static JsonArray numbers(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }
}
