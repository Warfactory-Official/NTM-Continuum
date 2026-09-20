package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Variants;

/**
 * Dejson declarations for the production machines (group A). Touches no Minecraft class: it runs in a
 * plain JVM from {@code dejson.gradle.kts}.
 *
 * <p>The OBJ-rendered machines (rotary furnace, thresher, microwave, conveyor press) get the shared empty
 * blockstate placeholder here and an authored {@code hbm:machine_obj} item definition under
 * {@code assets/hbm/items/}; the two plain cubes are fully declared.
 */
public final class ProductionDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private ProductionDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // 1.7 icons: machine_autocrafter_top / _side / _bottom.
        Dejson.block(NS, "machine_autocrafter")
                .models(Models.cubeBottomTop("hbm:blocks/machine_autocrafter_bottom",
                        "hbm:blocks/machine_autocrafter_side", "hbm:blocks/machine_autocrafter_top"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();
        // 1.7: one animated texture on every face.
        Dejson.block(NS, "press_preheater")
                .models(Models.cubeAll("hbm:blocks/press_preheater"))
                .states(Variants.simple())
                .blockItemDirect()
                .register();

        // Renderer-drawn machines: the blockstate exists only because vanilla demands one.
        for (String folded : new String[]{"machine_rotary_furnace", "machine_conveyor_press"}) {
            Dejson.block(NS, folded).rendered("hbm:block/machine_dummy_empty")
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).register();
        }
        for (String single : new String[]{"machine_thresher", "machine_microwave"}) {
            Dejson.block(NS, single).rendered("hbm:block/machine_dummy_empty")
                    .states(Variants.uniform("facing", "north", "east", "south", "west")).register();
        }

        for (String block : new String[]{"machine_rotary_furnace", "machine_thresher", "machine_microwave",
                "machine_autocrafter", "machine_conveyor_press", "press_preheater"}) {
            Drops.self(NS, block);
        }
    }
}
