package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;

/**
 * Dejson declarations for the 2026-09 Norwood machine wave (Chicago Pile, dark fusion core, Albion
 * accelerator, satellites and ground station, Soyuz complex, geiger, cargo elevator, RBMK terminal):
 * the plain cubes, facing cubes, sprites, shared block items and self-drops that shipped as authored
 * JSON in the branch. Authored OBJ item definitions and hand-built element models stay as files, as
 * everywhere else in the tree. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}.
 */
public final class NorwoodMachineDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private NorwoodMachineDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // --- blocks ---
        Dejson.block(NS, "cargo_elevator").statesOnly().states(Variants.simple()).register();
        Dejson.block(NS, "dfc_core").models(Models.cubeAll("hbm:blocks/dfc_core")).states(Variants.simple()).register();
        Dejson.block(NS, "dfc_emitter").models(Models.cubeAll("hbm:blocks/dfc_emitter")).states(Variants.facingAll()).register();
        Dejson.block(NS, "dfc_injector").models(Models.cubeAll("hbm:blocks/dfc_injector")).states(Variants.facingAll()).register();
        Dejson.block(NS, "dfc_receiver").models(Models.cubeAll("hbm:blocks/dfc_receiver")).states(Variants.facingAll()).register();
        Dejson.block(NS, "dfc_stabilizer").models(Models.cubeAll("hbm:blocks/dfc_stabilizer")).states(Variants.facingAll()).register();
        Dejson.block(NS, "machine_satlink").models(Models.cubeAll("hbm:blocks/machine_satlinker_side")).states(Variants.simple()).register();
        Dejson.block(NS, "pa_beamline").models(Models.cubeAll("hbm:blocks/block_steel")).states(Variants.simple()).register();
        Dejson.block(NS, "pa_detector").models(Models.cubeAll("hbm:blocks/block_steel")).states(Variants.simple()).register();
        Dejson.block(NS, "pa_dipole").models(Models.cubeAll("hbm:blocks/block_steel")).states(Variants.simple()).register();
        Dejson.block(NS, "pa_quadrupole").models(Models.cubeAll("hbm:blocks/block_steel")).states(Variants.simple()).register();
        Dejson.block(NS, "pa_rfc").models(Models.cubeAll("hbm:blocks/block_steel")).states(Variants.simple()).register();
        Dejson.block(NS, "pa_source").models(Models.cubeAll("hbm:blocks/block_steel")).states(Variants.simple()).register();
        Dejson.block(NS, "rbmk_terminal").statesOnly().states(Variants.facing()).register();
        Dejson.block(NS, "soyuz_launcher").models(Models.cubeAll("hbm:blocks/soyuz_launcher")).states(Variants.simple()).register();
        Dejson.block(NS, "struct_soyuz_core").models(Models.cubeAll("hbm:blocks/struct_soyuz_core")).states(Variants.simple()).register();

        // Chicago Pile: the graphite brick is a per-face cube; the assembled cell dispatches its META
        // (solid/channel/edge, core, fuel in/out, air in/out, control) onto five bottom-top cubes; the
        // three docking devices are one invisible block (renderer-drawn) across twelve meta values.
        Dejson.block(NS, "pile_brick")
                .models(Models.parented("minecraft:block/cube", dev.golem.core.dejson.Slots
                        .of("up", "hbm:blocks/pile_brick_top", "down", "hbm:blocks/pile_brick_top")
                        .and("north", "hbm:blocks/pile_brick").and("south", "hbm:blocks/pile_brick")
                        .and("west", "hbm:blocks/pile_brick_side").and("east", "hbm:blocks/pile_brick_side")
                        .and("particle", "hbm:blocks/pile_brick")))
                .states(Variants.simple()).register();
        Dejson.block(NS, "pile_block")
                .models(Models.group()
                        .and("", Models.cubeBottomTop("hbm:blocks/pile_block_top", "hbm:blocks/pile_block", "hbm:blocks/pile_block_top"))
                        .and("_core", Models.cubeBottomTop("hbm:blocks/pile_block_top", "hbm:blocks/pile_block_core", "hbm:blocks/pile_block_top"))
                        .and("_input", Models.cubeBottomTop("hbm:blocks/pile_block_top", "hbm:blocks/pile_block_input", "hbm:blocks/pile_block_top"))
                        .and("_output", Models.cubeBottomTop("hbm:blocks/pile_block_top", "hbm:blocks/pile_block_output", "hbm:blocks/pile_block_top"))
                        .and("_control", Models.cubeBottomTop("hbm:blocks/pile_block_control_top", "hbm:blocks/pile_block", "hbm:blocks/pile_block_control_top")))
                .states(Variants.select("meta", "0", "", "1", "_core", "2", "", "3", "_input", "4", "_output",
                        "5", "_input", "6", "_output", "7", "_control", "8", ""))
                .register();
        Dejson.block(NS, "pile_device").statesOnly().states(Variants.uniformRange("meta", 0, 11)).register();

        // --- items ---
        Dejson.item(NS, "ams_core_eyeofharmony").models(Models.generated("hbm:items/ams_core_eyeofharmony")).register();
        Dejson.item(NS, "ams_core_sing").models(Models.generated("hbm:items/ams_core_sing")).register();
        Dejson.item(NS, "ams_core_thingy").models(Models.generated("hbm:items/ams_core_thingy")).register();
        Dejson.item(NS, "ams_core_wormhole").models(Models.generated("hbm:items/ams_core_wormhole")).register();
        Dejson.item(NS, "ams_lens").models(Models.generated("hbm:items/ams_lens")).register();
        Dejson.item(NS, "missile_soyuz").models(Models.group()
                .and("", Models.generated("hbm:items/missile_soyuz_normal"))
                .and("_lunar", Models.generated("hbm:items/missile_soyuz_lunar"))
                .and("_post_war", Models.generated("hbm:items/missile_soyuz_post_war")))
                .selects(NS + ":item_variant").register();
        Dejson.item(NS, "pa_coil_bscco").models(Models.generated("hbm:items/pa_coil_bscco")).register();
        Dejson.item(NS, "pa_coil_chlorophyte").models(Models.generated("hbm:items/pa_coil_chlorophyte")).register();
        Dejson.item(NS, "pa_coil_gold").models(Models.generated("hbm:items/pa_coil_gold")).register();
        Dejson.item(NS, "pa_coil_niobium").models(Models.generated("hbm:items/pa_coil_niobium")).register();
        Dejson.item(NS, "pile_block").shares("hbm:block/pile_block").register();
        Dejson.item(NS, "pile_brick").shares("hbm:block/pile_brick").register();
        Dejson.item(NS, "pile_rod").models(Models.group()
                .and("", Models.generated("hbm:items/pile_rod_ra226be"))
                .and("_po210be", Models.generated("hbm:items/pile_rod_po210be"))
                .and("_zr", Models.generated("hbm:items/pile_rod_zr"))
                .and("_nu", Models.generated("hbm:items/pile_rod_nu"))
                // 1.7 pile_rod.pu239: the plain _pu239 name is the deprecated old-style rod's model and texture
                .and("_mk2_pu239", Models.generated("hbm:items/pile_rod_mk2_pu239"))
                .and("_rgp", Models.generated("hbm:items/pile_rod_rgp"))
                .and("_waste", Models.generated("hbm:items/pile_rod_waste"))
                .and("_thorium", Models.generated("hbm:items/pile_rod_thorium"))
                .and("_thorium_fuel", Models.generated("hbm:items/pile_rod_thorium_fuel")))
                .selects(NS + ":item_variant").register();
        Dejson.item(NS, "rbmk_terminal").shares("hbm:block/rbmk_terminal").register();
        Dejson.item(NS, "satellite").models(Models.group()
                .and("", Models.generated("hbm:items/satellite_spy"))
                .and("_scanner", Models.generated("hbm:items/satellite_scanner"))
                .and("_radar", Models.generated("hbm:items/satellite_radar"))
                .and("_miner_astro", Models.generated("hbm:items/satellite_miner_astro"))
                .and("_miner_lunar", Models.generated("hbm:items/satellite_miner_lunar"))
                .and("_precision_laser", Models.generated("hbm:items/satellite_precision_laser"))
                .and("_death_ray", Models.generated("hbm:items/satellite_death_ray"))
                .and("_xenium_resonator", Models.generated("hbm:items/satellite_xenium_resonator"))
                .and("_relay", Models.generated("hbm:items/satellite_relay"))
                .and("_detector", Models.generated("hbm:items/satellite_detector"))
                .and("_ray_scan", Models.generated("hbm:items/satellite_ray_scan"))
                .and("_science", Models.generated("hbm:items/satellite_science"))
                .and("_science_assembler", Models.generated("hbm:items/satellite_science_assembler"))
                .and("_science_sensor", Models.generated("hbm:items/satellite_science_sensor")))
                .selects(NS + ":item_variant").register();
        Dejson.item(NS, "soyuz_launcher").shares("hbm:block/soyuz_launcher").register();
        Dejson.item(NS, "struct_soyuz_core").shares("hbm:block/struct_soyuz_core").register();

        // --- drops ---
        Drops.self(NS, "dfc_core");
        Drops.self(NS, "dfc_emitter");
        Drops.self(NS, "dfc_injector");
        Drops.self(NS, "dfc_receiver");
        Drops.self(NS, "dfc_stabilizer");
        Drops.self(NS, "machine_satlink");
        Drops.self(NS, "pa_beamline");
        Drops.self(NS, "pa_detector");
        Drops.self(NS, "pa_dipole");
        Drops.self(NS, "pa_quadrupole");
        Drops.self(NS, "pa_rfc");
        Drops.self(NS, "pa_source");
        Drops.self(NS, "struct_soyuz_core");
        Drops.self(NS, "pile_brick");
        Drops.self(NS, "pile_device");
    }
}
