package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.special;

/**
 * Dejson declarations for the custom machine system. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}. The variant arrays repeat {@link ModCustomMachineBlocks}' on purpose, since that class
 * is a registry holder and cannot load here.
 *
 * <p>Models follow 1.7's icons: {@code BlockCM} drew {@code <texture>_<variant>} on every face; the receivers are
 * {@code BlockPillar}s ({@code _top} on both ends); the controller and the anchor draw their terminal front on the
 * face turned to the placer. Recipes are 1.7 {@code CraftingManager}'s; {@code KEY_ANYGLASS} ("blockGlass") is the
 * {@code c:glass_blocks} tag and the two {@code ANY_*} alloy groups are their {@code c:} aggregate tags.
 */
public final class CustomMachineDeclarations {

    private static final String NS = "hbm";
    private static final String[] MATERIALS = {"steel", "alloy", "desh", "tcalloy"};
    private static final String[] ENGINES = {"standard", "desh", "bismuth"};
    private static final String[] CIRCUITS = {"aluminium", "copper", "red_copper", "gold", "schrabidium"};
    private static boolean declared;

    private CustomMachineDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        blocks();
        items();
        drops();
        recipes();
    }

    private static void cube(String id) {
        Dejson.block(NS, id).models(Models.cubeAll("hbm:blocks/" + id)).states(Variants.simple()).blockItemDirect().register();
    }

    private static void blocks() {
        Dejson.block(NS, "custom_machine")
                .models(Models.orientable("hbm:blocks/cm_terminal_front", "hbm:blocks/cm_terminal_side", "hbm:blocks/cm_terminal_side"))
                .states(Variants.facing()).blockItemDirect().register();

        for (String m : MATERIALS) cube("cm_block_" + m);
        for (String m : MATERIALS) cube("cm_sheet_" + m);
        for (String e : ENGINES) cube("cm_engine_" + e);
        // 1.7 BlockCMGlass: see-through, with the faces between neighbouring tanks skipped
        for (String m : MATERIALS) {
            Dejson.block(NS, "cm_tank_" + m).models(Models.renderType("cutout", Models.cubeAll("hbm:blocks/cm_tank_" + m)))
                    .states(Variants.simple()).blockItemDirect().register();
        }
        for (String c : CIRCUITS) cube("cm_circuit_" + c);
        for (String m : MATERIALS) cube("cm_port_" + m);

        Dejson.block(NS, "cm_flux").models(Models.column("hbm:blocks/cm_flux_top", "hbm:blocks/cm_flux_side"))
                .states(Variants.simple()).blockItemDirect().register();
        Dejson.block(NS, "cm_heat").models(Models.column("hbm:blocks/cm_heat_top", "hbm:blocks/cm_heat_side"))
                .states(Variants.simple()).blockItemDirect().register();

        Dejson.block(NS, "custom_machine_anchor")
                .models(Models.orientable("hbm:blocks/cmt_terminal_front", "hbm:blocks/cmt_terminal_side", "hbm:blocks/cmt_terminal_side"))
                .states(Variants.facing()).blockItemDirect().register();
    }

    private static void items() {
        // setFull3D
        Dejson.item(NS, "structure_custommachine").models(Models.handheld("hbm:items/structure_custommachine")).register();
    }

    private static void drops() {
        // BlockCustomMachine.getDrops hands back the configured controller; the table is not consulted
        Drops.nothing(NS, "custom_machine");
        for (String m : MATERIALS) {
            Drops.self(NS, "cm_block_" + m);
            Drops.self(NS, "cm_sheet_" + m);
            Drops.self(NS, "cm_tank_" + m);
            Drops.self(NS, "cm_port_" + m);
        }
        for (String e : ENGINES) Drops.self(NS, "cm_engine_" + e);
        for (String c : CIRCUITS) Drops.self(NS, "cm_circuit_" + c);
        Drops.self(NS, "cm_flux");
        Drops.self(NS, "cm_heat");
        Drops.self(NS, "custom_machine_anchor");
    }

    private static void recipes() {
        // 1.7 CraftingManager: the four casings, metas 0, 2, 3, 1 in that order
        shaped(NS, "cm_block_steel", "hbm:cm_block_steel", 4, rows(" I ", "IPI", " I "),
                "I", "#c:ingots/steel", "P", "#c:plates_cast/steel");
        shaped(NS, "cm_block_desh", "hbm:cm_block_desh", 4, rows(" I ", "IPI", " I "),
                "I", "#c:ingots/desh", "P", "#c:plates_cast/desh");
        shaped(NS, "cm_block_tcalloy", "hbm:cm_block_tcalloy", 4, rows(" I ", "IPI", " I "),
                "I", "#c:ingots/any_resistant_alloy", "P", "#c:plates_cast/any_resistant_alloy");
        shaped(NS, "cm_block_alloy", "hbm:cm_block_alloy", 4, rows(" I ", "IPI", " I "),
                "I", "#c:ingots/any_bismoid_bronze", "P", "#c:plates_cast/any_bismoid_bronze");

        for (String m : MATERIALS) {
            String casing = "hbm:cm_block_" + m;
            shaped(NS, "cm_sheet_" + m, "hbm:cm_sheet_" + m, 16, rows("BB", "BB"), "B", casing);
            shaped(NS, "cm_tank_" + m, "hbm:cm_tank_" + m, 4, rows(" B ", "BGB", " B "),
                    "B", casing, "G", "#c:glass_blocks");
            shaped(NS, "cm_port_" + m, "hbm:cm_port_" + m, rows("P", "B", "P"), "B", casing, "P", "#c:plates/iron");
        }

        shaped(NS, "cm_engine_standard", "hbm:cm_engine_standard", rows(" I ", "IMI", " I "),
                "I", "#c:ingots/steel", "M", "hbm:motor");
        shaped(NS, "cm_engine_desh", "hbm:cm_engine_desh", rows(" I ", "IMI", " I "),
                "I", "#c:ingots/steel", "M", "hbm:motor_desh");
        shaped(NS, "cm_engine_bismuth", "hbm:cm_engine_bismuth", rows(" I ", "IMI", " I "),
                "I", "#c:ingots/steel", "M", "hbm:motor_bismuth");
        String[] circuitItems = {"hbm:circuit_vacuum_tube", "hbm:circuit_analog", "hbm:circuit_basic",
                "hbm:circuit_advanced", "hbm:circuit_bismoid"};
        for (int i = 0; i < CIRCUITS.length; i++) {
            shaped(NS, "cm_circuit_" + CIRCUITS[i], "hbm:cm_circuit_" + CIRCUITS[i], rows(" I ", "IMI", " I "),
                    "I", "#c:ingots/steel", "M", circuitItems[i]);
        }
        shaped(NS, "cm_flux", "hbm:cm_flux", rows("NNN", "ZCZ", "NNN"),
                "Z", "#c:plates_cast/zirconium", "N", "hbm:neutron_reflector", "C", "hbm:reactor_core");
        shaped(NS, "cm_heat", "hbm:cm_heat", rows("PCP", "PCP", "PCP"),
                "P", "hbm:plate_polymer", "C", "#c:ingots/copper");

        // 1.7 CustomMachineConfigJSON.readConfig: one recipe per configured machine, matched at crafting time
        special(NS, "custom_machine", "hbm:custom_machine");
    }
}
