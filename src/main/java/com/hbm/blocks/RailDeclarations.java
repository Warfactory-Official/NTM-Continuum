package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Variants;

/**
 * Dejson declarations for the folded rail family.  The blockstate is deliberately empty: the rail
 * BER owns the OBJ, while the state still has to enumerate every {@code cell/facing/waterlogged}
 * combination so the client never falls back to a missing model.
 */
public final class RailDeclarations {

    private static final String NS = "hbm";
    private static final String EMPTY = "hbm:block/machine_dummy_empty";
    private static final String[] RAILS = {
            "rail_large_straight", "rail_large_straight_short", "rail_large_curve", "rail_large_curve_7",
            "rail_large_curve_9", "rail_large_ramp", "rail_large_buffer", "rail_large_switch",
            "rail_large_switch_flipped", "rail_narrow_straight", "rail_narrow_curve",
    };
    private static boolean declared;

    private RailDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        var states = Variants.product(
                Variants.uniform("cell", "false", "true"),
                Variants.uniform("facing", "north", "east", "south", "west"),
                Variants.uniform("waterlogged", "false", "true"));
        for (String rail : RAILS) {
            Dejson.block(NS, rail).rendered(EMPTY).states(states).register();
        }
    }
}
