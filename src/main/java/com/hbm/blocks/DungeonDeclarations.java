package com.hbm.blocks;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;

/**
 * The dungeon family's recipes; every block's models, blockstate, item model, loot and tags are stated on its
 * registration in {@link ModDungeonBlocks} (Reg), the rebar placer's item model on its own in {@code ModDungeonItems}.
 * Touches no Minecraft class: it runs in a plain JVM from {@code dejson.gradle.kts}.
 */
public final class DungeonDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private DungeonDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        recipes();
    }

    private static void recipes() {
        // 1.7 CraftingManager
        shaped(NS, "rebar", "hbm:rebar", 8, rows("BB", "BB"), "B", "#c:bolts/steel");
        shaped(NS, "rail_wood", "hbm:rail_wood", 16, rows("S S", "SRS", "S S"),
                "S", "minecraft:stick", "R", "hbm:plant_item_rope");
        shaped(NS, "rail_narrow", "hbm:rail_narrow", 64, rows("S S", "S S", "S S"), "S", "hbm:steel_beam");
        shaped(NS, "rail_highspeed", "hbm:rail_highspeed", 16, rows("S S", "SIS", "S S"),
                "S", "#c:ingots/steel", "I", "#c:plates/iron");
        shaped(NS, "rail_booster", "hbm:rail_booster", 6, rows("S S", "CIC", "SRS"),
                "S", "#c:ingots/steel", "I", "#c:plates/iron", "R", "#c:ingots/red_copper", "C", "hbm:coil_copper");
        // 1.7 ToolRecipes
        shaped(NS, "rebar_placer", "hbm:rebar_placer", rows("RDR", "DWD", "RDR"),
                "R", "hbm:rebar", "D", "hbm:ducttape", "W", "hbm:wrench");
    }
}
