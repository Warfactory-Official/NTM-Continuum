package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Tints;
import dev.golem.core.dejson.Variants;

/**
 * Dejson declarations (models, item definitions, loot) for the fluid-network family. Runs in a plain
 * JVM: nothing here may touch a Minecraft class. What Dejson cannot say is authored under
 * {@code src/main/resources}: the multipart blockstates of the ducts (seven boolean properties), the
 * foundry tank (ten), the pipe anchor's element models, and the OBJ item definitions.
 */
public final class FluidNetDeclarations {

    private static final String NS = "hbm";
    private static final String PLACEHOLDER = "hbm:block/machine_dummy_empty";
    private static final String[] BOX_MATERIALS = {"silver", "copper", "white"};

    private static boolean declared;

    private FluidNetDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        blocks();
        items();
        drops();
    }

    private static void blocks() {
        // Redstone valve / counter valve: the fluid_valve pair of cubes, keyed by "open".
        cubePair("fluid_switch", "hbm:blocks/fluid_switch_off", "hbm:blocks/fluid_switch_on");
        cubePair("fluid_counter_valve", "hbm:blocks/fluid_counter_valve_off", "hbm:blocks/fluid_counter_valve_on");

        // Renderer-drawn blocks: the blockstate exists because vanilla demands one.
        Dejson.block(NS, "fluid_pump").rendered(PLACEHOLDER)
                .states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        for (String barrel : new String[]{"barrel_plastic", "barrel_corroded", "barrel_steel", "barrel_tcalloy", "barrel_antimatter"}) {
            Dejson.block(NS, barrel).rendered(PLACEHOLDER).states(Variants.simple()).register();
        }
        for (String barrel : new String[]{"red_barrel", "pink_barrel", "yellow_barrel", "vitrified_barrel", "lox_barrel", "taint_barrel"}) {
            Dejson.block(NS, barrel).rendered(PLACEHOLDER).states(Variants.uniform("armed", "false", "true")).register();
        }

        // The static fluid blocks: still texture on the ends, flowing on the sides (1.7 getIcon).
        fluidCube("acid_block", "acid");
        fluidCube("toxic_block", "toxic");
        fluidCube("schrabidic_block", "schrabidic_acid");
        fluidCube("sulfuric_acid_block", "sulfuric_acid");
        fluidCube("corium_block", "corium");
        fluidCube("volcanic_lava_block", "volcanic_lava");
        fluidCube("rad_lava_block", "rad_lava");
    }

    private static void cubePair(String id, String off, String on) {
        Dejson.block(NS, id)
                .models(Models.group().and("_off", Models.cubeAll(off)).and("_on", Models.cubeAll(on)))
                .states(Variants.select("open", "false", "_off", "true", "_on"))
                .register();
    }

    private static void fluidCube(String id, String texture) {
        Dejson.block(NS, id)
                .models(Models.renderType("minecraft:translucent",
                        Models.cubeBottomTop("hbm:blocks/" + texture + "_still", "hbm:blocks/" + texture + "_flowing", "hbm:blocks/" + texture + "_still")))
                .states(Variants.simple())
                .register();
    }

    private static void items() {
        for (String sprite : new String[]{"bucket_acid", "bucket_mud", "bucket_schrabidic_acid", "bucket_sulfuric_acid", "bucket_toxic",
                "siphon", "inf_water", "inf_water_mk2", "chlorine_pinwheel", "fluid_barrel_infinite"}) {
            Dejson.item(NS, sprite).models(Models.generated("hbm:items/" + sprite)).register();
        }
        // Tinted containers: base pinned white, overlay tinted by the stack's custom_model_data colour.
        tinted("canister_full", "hbm:items/canister_empty", "hbm:items/canister_overlay");
        tinted("fluid_pack_full", "hbm:items/fluid_pack", "hbm:items/fluid_pack_overlay");
        tinted("fluid_tank_lead_full", "hbm:items/fluid_tank_lead", "hbm:items/fluid_tank_lead_overlay");
        // The gas tank has a third, label layer; 1.7 tinted it by its own label colour, but a stack
        // carries one colour, so the label stays white here.
        Dejson.item(NS, "gas_full")
                .models(Models.generated("hbm:items/gas_empty", "hbm:items/gas_bottle", "hbm:items/gas_label"))
                .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF), Tints.constant(-1))
                .register();

        // Box-duct items: a plain cube in the duct's straight texture (1.7 drew the straight run).
        for (String material : BOX_MATERIALS) {
            for (int size = 0; size < 5; size++) {
                Dejson.item(NS, "fluid_duct_box_" + material + "_" + size)
                        .models(Models.parented(Models.CUBE_ALL, Slots.of("all", "hbm:blocks/boxduct_" + material + "_straight")))
                        .register();
            }
        }
        for (int size = 0; size < 5; size++) {
            Dejson.item(NS, "fluid_duct_exhaust_" + size)
                    .models(Models.parented(Models.CUBE_ALL, Slots.of("all", "hbm:blocks/boxduct_exhaust_straight")))
                    .register();
        }
    }

    private static void tinted(String item, String base, String overlay) {
        Dejson.item(NS, item)
                .models(Models.generated(base, overlay))
                .tints(Tints.constant(-1), Tints.customModelData(0, 0xFFFFFF))
                .register();
    }

    private static void drops() {
        for (String block : new String[]{"fluid_duct_neo", "fluid_duct_neo_silver", "fluid_duct_neo_colored",
                "fluid_duct_paintable_block_exhaust", "pipe_anchor", "fluid_duct_paintable", "fluid_duct_gauge",
                "fluid_switch", "fluid_counter_valve", "fluid_pump", "foundry_tank", "barrel_corroded",
                "red_barrel", "pink_barrel", "yellow_barrel", "vitrified_barrel", "lox_barrel", "taint_barrel"}) {
            Drops.self(NS, block);
        }
        for (String material : BOX_MATERIALS) {
            for (int size = 0; size < 5; size++) Drops.self(NS, "fluid_duct_box_" + material + "_" + size);
        }
        for (int size = 0; size < 5; size++) Drops.self(NS, "fluid_duct_exhaust_" + size);
        // A non-empty barrel keeps its tank in the item (1.7 IPersistentNBT).
        for (String barrel : new String[]{"barrel_plastic", "barrel_steel", "barrel_tcalloy", "barrel_antimatter"}) {
            Drops.container(NS, barrel, "minecraft:custom_data", "minecraft:custom_name");
        }
        // The fluid blocks drop nothing (1.7 fluids never did).
    }
}
