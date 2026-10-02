package com.hbm.items;

import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;

/**
 * Dejson declarations of the 2026-09-26 gap batch A: the ten 1.7 {@code ItemBatterySC} variants (sprites
 * {@code battery_sc.<variant>.png}, copied as {@code battery_sc_<variant>.png}) and their 1.7 {@code CraftingManager}
 * recipes (lines 262-271). Touches no Minecraft class: it runs in a plain JVM from {@code dejson.gradle.kts}.
 */
public final class GapsADeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    /** {@code EnumBatterySC} order, and the 1.7 billet each filled variant takes two of (null for the empty one). */
    private static final String[][] SC = {
            {"empty", null}, {"waste", ItemIds.billet_nuclear_waste}, {"ra226", TagIds.billets_ra226},
            {"tc99", TagIds.billets_technetium}, {"co60", TagIds.billets_co60}, {"pu238", TagIds.billets_pu238},
            {"po210", TagIds.billets_polonium}, {"au198", TagIds.billets_au198}, {"pb209", TagIds.billets_pb209},
            {"am241", TagIds.billets_am241}};

    private GapsADeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // The ten sprites are stated on ModBatterySCItems' registrations (Reg).
        // 1.7 CraftingManager:262: the empty radiovoltaic cell
        shaped(NS, "battery_sc_empty", "hbm:battery_sc_empty", rows("PGP", "L L", "PGP"),
                "P", TagIds.ingots_any_plastic, "G", TagIds.wires_gold, "L", TagIds.plates_lead);
        // 1.7 CraftingManager:263-271: the empty cell and two billets of the isotope
        for (String[] row : SC) {
            if (row[1] == null) continue;
            String id = "battery_sc_" + row[0];
            shapeless(NS, id, "hbm:" + id, "hbm:battery_sc_empty", row[1], row[1]);
        }
    }
}
