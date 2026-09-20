package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;

/**
 * Dejson declarations for the energy-network family. Touches no Minecraft class: it runs in a plain JVM
 * at build time. What Dejson cannot say (the classic cable's multipart, the paintable cable's four
 * variants, element models, OBJ item definitions) is authored JSON under {@code src/main/resources}.
 */
public final class EnergyNetDeclarations {

    private EnergyNetDeclarations() {}

    private static final String NS = "hbm";
    private static final String EMPTY = "hbm:block/machine_dummy_empty";

    private static final String[] FOLDED = {
            "red_pylon", "red_pylon_steel", "red_pylon_medium_wood", "red_pylon_medium_wood_transformer",
            "red_pylon_medium_steel", "red_pylon_medium_steel_transformer", "red_pylon_large", "substation"
    };

    private static final String[] SELF_DROPS = {
            "red_pylon", "red_pylon_steel", "red_pylon_medium_wood", "red_pylon_medium_wood_transformer",
            "red_pylon_medium_steel", "red_pylon_medium_steel_transformer", "red_pylon_large", "substation",
            "red_connector", "red_connector_super", "red_cable_classic", "red_cable_gauge", "red_cable_paintable",
            "cable_switch", "cable_detector", "cable_diode", "machine_detector", "capacitor_bus",
            "capacitor_tantalium", "capacitor_schrabidate", "machine_battery_potato", "machine_lithium_battery",
            "machine_schrabidium_battery", "machine_dineutronium_battery", "hev_battery", "tesla"
    };

    public static void declare() {
        // Renderer-drawn blocks: the blockstate is the shared empty placeholder; the item is an authored OBJ definition.
        for (String name : FOLDED) {
            Dejson.block(NS, name).rendered(EMPTY)
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).register();
        }
        Dejson.block(NS, "red_connector").rendered(EMPTY).states(Variants.uniform("facing", "down", "up", "north", "south", "west", "east")).register();
        Dejson.block(NS, "red_connector_super").rendered(EMPTY).states(Variants.uniform("facing", "down", "up", "north", "south", "west", "east")).register();
        Dejson.block(NS, "hev_battery").rendered(EMPTY).states(Variants.simple()).register();
        Dejson.block(NS, "tesla").rendered(EMPTY).states(Variants.simple()).register();

        // Hand-built element models over regular blockstates.
        Dejson.block(NS, "red_cable_gauge").statesOnly().states(Variants.facingAll()).register();
        Dejson.item(NS, "red_cable_gauge").shares("hbm:block/red_cable_gauge").register();
        Dejson.block(NS, "cable_diode").statesOnly().states(Variants.facingAll()).register();
        Dejson.item(NS, "cable_diode").shares("hbm:block/cable_diode").register();
        Dejson.block(NS, "capacitor_tantalium").statesOnly().states(Variants.facingUp()).register();
        Dejson.item(NS, "capacitor_tantalium").shares("hbm:block/capacitor_tantalium").register();
        Dejson.block(NS, "capacitor_schrabidate").statesOnly().states(Variants.facingUp()).register();
        Dejson.item(NS, "capacitor_schrabidate").shares("hbm:block/capacitor_schrabidate").register();

        // Plain cubes.
        Dejson.block(NS, "cable_switch")
                .models(Models.group()
                        .and("", Models.cubeAll("hbm:blocks/cable_switch_off"))
                        .and("_on", Models.cubeAll("hbm:blocks/cable_switch_on")))
                .states(Variants.flag("on", "_on")).blockItemDirect().register();
        Dejson.block(NS, "cable_detector")
                .models(Models.group()
                        .and("", Models.cubeAll("hbm:blocks/cable_detector_off"))
                        .and("_on", Models.cubeAll("hbm:blocks/cable_detector_on")))
                .states(Variants.flag("on", "_on")).blockItemDirect().register();
        Dejson.block(NS, "machine_detector")
                .models(Models.group()
                        .and("", Models.cubeAll("hbm:blocks/machine_detector_off"))
                        .and("_on", Models.cubeAll("hbm:blocks/machine_detector")))
                .states(Variants.flag("on", "_on")).blockItemDirect().register();
        Dejson.block(NS, "capacitor_bus")
                .models(Models.cubeBottomTop("hbm:blocks/capacitor_bus_side", "hbm:blocks/capacitor_bus_side", "hbm:blocks/capacitor_bus_out"))
                .states(Variants.facingUp()).blockItemDirect().register();

        battery("machine_battery_potato", "battery_potato");
        battery("machine_lithium_battery", "battery_lithium");
        battery("machine_schrabidium_battery", "battery_schrabidium");
        battery("machine_dineutronium_battery", "battery_dineutronium");

        Dejson.item(NS, "wiring_red_copper").models(Models.generated("hbm:items/wiring_red_copper")).register();

        for (String name : SELF_DROPS) Drops.self(NS, name);
    }

    private static void battery(String id, String tex) {
        Dejson.block(NS, id)
                .models(Models.orientable("hbm:blocks/" + tex + "_front", "hbm:blocks/" + tex + "_side", "hbm:blocks/" + tex + "_top"))
                .states(Variants.facing()).blockItemDirect().register();
    }
}
