package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Dispatch;
import dev.golem.core.dejson.Drops;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Tints;
import dev.golem.core.dejson.Variants;
import com.hbm.items.machine.ItemDepletedFuel;

/**
 * Dejson declarations for the miscellaneous nuclear machines, the legacy graphite pile and their items.
 * Touches no Minecraft class: it runs in a plain JVM from {@code dejson.gradle.kts}.
 *
 * <p>The three OBJ machines get the shared empty folded blockstate here and an authored
 * {@code hbm:machine_obj} item definition under {@code assets/hbm/items/}.
 */
public final class NuclearMiscDeclarations {

    private static final String NS = "hbm";
    private static final String GRAPHITE = "hbm:blocks/block_graphite";
    private static boolean declared;

    private static final String[] ROD_TYPES = {
            "lithium", "tritium", "co", "co60", "th232", "thf", "u235", "np237", "u238", "pu238", "pu239",
            "rgp", "waste", "lead", "uranium", "ra226", "ac227"
    };

    private NuclearMiscDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // Renderer-drawn folded machines: the blockstate exists only because vanilla demands one.
        for (String folded : new String[]{"machine_radiolysis", "machine_reactor", "machine_reactor_small"}) {
            Dejson.block(NS, folded).rendered("hbm:block/machine_dummy_empty")
                    .states(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))).register();
            Drops.self(NS, folded);
        }

        // 1.7 icons: top for up/down, front on the facing, back opposite, side elsewhere.
        Dejson.block(NS, "machine_controller")
                .models(Models.parented("minecraft:block/cube", Slots.of("particle", "hbm:blocks/machine_controller_side")
                        .and("up", "hbm:blocks/machine_controller_top").and("down", "hbm:blocks/machine_controller_top")
                        .and("north", "hbm:blocks/machine_controller").and("south", "hbm:blocks/machine_controller_back")
                        .and("east", "hbm:blocks/machine_controller_side").and("west", "hbm:blocks/machine_controller_side")))
                .states(Variants.facing())
                .blockItemDirect()
                .register();
        Drops.self(NS, "machine_controller");

        // The legacy pile: channel faces carry the block's own icon (or its aluminium-shrouded variant),
        // the other four faces plain graphite; drops are computed in code (ingots + shroud + rod).
        graphite("block_graphite_drilled", "block_graphite_drilled", null);
        graphite("block_graphite_fuel", "block_graphite_fuel", null);
        graphite("block_graphite_plutonium", "block_graphite_plutonium", null);
        graphite("block_graphite_source", "block_graphite_source", null);
        graphite("block_graphite_lithium", "block_graphite_lithium", null);
        graphite("block_graphite_tritium", "block_graphite_tritium", null);
        graphite("block_graphite_rod", "block_graphite_rod_in", "block_graphite_rod_out");
        graphite("block_graphite_detector", "block_graphite_detector", "block_graphite_detector_out");

        // Items.
        for (String rod : new String[]{"pile_rod_uranium", "pile_rod_pu239", "pile_rod_plutonium", "pile_rod_source",
                "pile_rod_boron", "pile_rod_lithium", "pile_rod_detector", "ingot_u238m2", "demon_core_open",
                "quartz_plutonium", "reactor_sensor"}) {
            Dejson.item(NS, rod).models(Models.generated("hbm:items/" + rod)).register();
        }
        for (String size : new String[]{"rod", "rod_dual", "rod_quad"}) {
            for (String type : ROD_TYPES) {
                Dejson.item(NS, size + "_" + type).models(Models.generated("hbm:items/" + size + "_" + type)).register();
            }
        }
        // 1.7 ItemDepletedFuel: subtype 1 (hot) tinted 0xFFBFA5, carried as custom model data here.
        wastePlate("waste_plate_u233", "waste_plate_uranium");
        wastePlate("waste_plate_u235", "waste_plate_uranium");
        wastePlate("waste_plate_mox", "waste_plate_mox");
        wastePlate("waste_plate_pu239", "waste_plate_mox");
        wastePlate("waste_plate_sa326", "waste_plate_sa326");
        wastePlate("waste_plate_ra226be", "waste_plate_ra226be");
        wastePlate("waste_plate_pu238be", "waste_plate_pu238be");
    }

    private static void wastePlate(String id, String texture) {
        Dejson.item(NS, id)
                .models(Models.generated("hbm:items/" + texture))
                .tints(Tints.customModelData(0, 0xFFFFFF))
                .register();
    }

    /** {@code icon} on the channel faces; {@code outIcon} (may be null) replaces it while {@code out} is set. */
    private static void graphite(String id, String icon, String outIcon) {
        Models.Group group = Models.group()
                .and("", Models.column("hbm:blocks/" + icon, GRAPHITE))
                .and("_aluminum", Models.column("hbm:blocks/" + icon + "_aluminum", GRAPHITE));
        Dispatch out;
        if (outIcon != null) {
            group = group.and("_out", Models.column("hbm:blocks/" + outIcon, GRAPHITE))
                    .and("_out_aluminum", Models.column("hbm:blocks/" + outIcon + "_aluminum", GRAPHITE));
            out = Variants.select("out", "false", "", "true", "_out");
        } else {
            out = Variants.uniform("out", "false", "true");
        }
        Dejson.block(NS, id)
                .models(group)
                .states(Variants.product(Variants.axis(), out, Variants.select("aluminum", "false", "", "true", "_aluminum")))
                .blockItemDirect()
                .register();
        Drops.nothing(NS, id);
    }
}
