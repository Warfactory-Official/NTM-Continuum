package com.hbm.blocks;

import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;

/**
 * The 1.7 crafting recipes of the RBMK console peripheral family (CraftingManager 770-782, the gauge and terminal
 * included). The peripherals' own models, blockstates, item definitions and loot are stated on their registrations
 * in {@code ModRBMKBlocks} (Reg); recipes are not part of that migration. Touches no Minecraft class.
 */
public final class RBMKPeripheralDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private RBMKPeripheralDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // --- 1.7 CraftingManager 770, 774-782 ---
        shaped(NS, "rbmk_loader", "hbm:rbmk_loader", rows("SCS", "CBC", "SCS"),
                "S", TagIds.plates_steel, "C", TagIds.ingots_copper, "B", ItemIds.tank_steel);
        shaped(NS, "rbmk_display_blank", "hbm:rbmk_display_blank", 8, rows("B", "D"),
                "B", TagIds.ingots_boron, "D", ItemIds.concrete_asbestos);
        shaped(NS, "rbmk_display", "hbm:rbmk_display", rows("C", "B"),
                "C", ItemIds.crt_display, "B", "hbm:rbmk_display_blank");
        shaped(NS, "rbmk_key_pad", "hbm:rbmk_key_pad", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_sender, "B", "hbm:rbmk_display_blank", "C", ItemIds.circuit_vacuum_tube);
        shaped(NS, "rbmk_gauge", "hbm:rbmk_gauge", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_receiver, "B", "hbm:rbmk_display_blank", "C", ItemIds.circuit_vacuum_tube);
        shaped(NS, "rbmk_numitron", "hbm:rbmk_numitron", rows(" R ", "CCC", " B "),
                "R", ItemIds.radio_torch_receiver, "B", "hbm:rbmk_display_blank", "C", ItemIds.circuit_numitron);
        shaped(NS, "rbmk_graph", "hbm:rbmk_graph", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_receiver, "B", "hbm:rbmk_display_blank", "C", ItemIds.crt_display);
        shaped(NS, "rbmk_lever", "hbm:rbmk_lever", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_sender, "B", "hbm:rbmk_display_blank", "C", TagIds.ingots_copper);
        shaped(NS, "rbmk_indicator", "hbm:rbmk_indicator", rows("R", "C", "B"),
                "R", ItemIds.radio_torch_receiver, "B", "hbm:rbmk_display_blank", "C", ItemIds.coil_tungsten);
        shaped(NS, "rbmk_terminal", "hbm:rbmk_terminal", rows("R ", "CD", "B "),
                "R", ItemIds.radio_torch_sender, "B", "hbm:rbmk_display_blank", "C", ItemIds.circuit_analog, "D", ItemIds.crt_display);
    }
}
