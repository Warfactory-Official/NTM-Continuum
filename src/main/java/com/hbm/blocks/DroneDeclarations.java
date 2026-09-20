package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;

/**
 * Dejson declarations for the delivery drone blocks. Touches no Minecraft class: it runs in a plain JVM from
 * {@code dejson.gradle.kts}. The crates are bottom/top/side cubes over the 1.7 sprites; the two waypoints ride
 * the radio torch's element model over their own sprite, mounted on any side.
 */
public final class DroneDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private DroneDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        for (String block : new String[]{"drone_crate", "drone_dock", "drone_crate_provider", "drone_crate_requester"}) {
            Dejson.block(NS, block)
                    .models(Models.cubeBottomTop("hbm:blocks/" + block + "_bottom", "hbm:blocks/" + block + "_side",
                            "hbm:blocks/" + block + "_top"))
                    .states(Variants.simple())
                    .blockItemDirect()
                    .register();
            Drops.self(NS, block);
        }

        for (String block : new String[]{"drone_waypoint", "drone_waypoint_request"}) {
            Dejson.block(NS, block)
                    .models(Models.parented("hbm:block/radio_torch_template", Slots.of("torch", "hbm:blocks/" + block)))
                    .states(Variants.facingUp())
                    .register();
            Drops.self(NS, block);
            Dejson.item(NS, block).models(Models.generated("hbm:blocks/" + block)).register();
        }

        Dejson.item(NS, "drone_linker").models(Models.generated("hbm:items/drone_linker")).register();
    }
}
