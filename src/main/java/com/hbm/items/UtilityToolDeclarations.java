package com.hbm.items;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.shapeless;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * Dejson declarations for {@link ModUtilityToolItems}: the 1.7 {@code ToolRecipes} / {@code CraftingManager} recipes
 * of every item that has one. The item models are stated on the registrations in {@code ModUtilityToolItems}, the
 * guide block's model and loot on its registration in {@code ModUtilityToolContent}. Touches no Minecraft class.
 *
 * <p>The holotape restoration and the restored-holotape-to-damaged recipes take one holotape variant only;
 * they are component shapeless recipes in {@link com.hbm.inventory.recipes.ModContainerRecipes}.
 */
public final class UtilityToolDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private UtilityToolDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        recipes();
    }

    private static void recipes() {
        // 1.7 ToolRecipes
        shaped(NS, "survey_scanner", "hbm:survey_scanner", rows("SWS", " G ", "PCP"),
                "W", "#c:wires/gold", "P", "#c:ingots/any_plastic", "C", "hbm:circuit_advanced",
                "S", "#c:plates/steel", "G", "#c:ingots/gold");
        shapeless(NS, "digamma_diagnostic", "hbm:digamma_diagnostic",
                "hbm:geiger_counter", "#c:billets/polonium", "#c:ingots/asbestos");
        shaped(NS, "pollution_detector", "hbm:pollution_detector", rows("SFS", "SCS", " S "),
                "S", "#c:plates/steel", "F", "hbm:filter_coal", "C", "hbm:circuit_vacuum_tube");
        shaped(NS, "ore_density_scanner", "hbm:ore_density_scanner", rows("VVV", "CSC", "GGG"),
                "V", "hbm:circuit_vacuum_tube", "C", "hbm:circuit_capacitor", "S", "hbm:circuit_controller_chassis",
                "G", "#c:plates/gold");
        shaped(NS, "coltan_tool", "hbm:coltan_tool", rows("ACA", "CXC", "ACA"),
                "A", "#c:ingots/copper", "C", "#c:gems/cinnabar", "X", "minecraft:compass");
        shaped(NS, "settings_tool", "hbm:settings_tool", rows(" P ", "PCP", "III"),
                "P", "#c:plates/iron", "C", "hbm:circuit_analog", "I", "hbm:plate_polymer");
        shaped(NS, "pipette", "hbm:pipette", rows("  L", " G ", "G  "),
                "L", "#c:ingots/any_rubber", "G", "#c:glass_blocks/colorless");
        shaped(NS, "pipette_boron", "hbm:pipette_boron", rows("  P", " B ", "B  "),
                "P", "#c:ingots/rubber", "B", "hbm:glass_boron");
        shaped(NS, "pipette_laboratory", "hbm:pipette_laboratory", rows("  C", " R ", "P  "),
                "C", "hbm:circuit_chip", "R", "#c:ingots/rubber", "P", "hbm:pipette_boron");
        shaped(NS, "power_net_tool", "hbm:power_net_tool", rows("WRW", " I ", " B "),
                "W", "#c:wires/red_copper", "R", "#c:dusts/redstone", "I", "#c:ingots/iron", "B", "hbm:battery_lead");
        shaped(NS, "analysis_tool", "hbm:analysis_tool", rows("  G", " S ", "S  "),
                "G", "#c:glass_panes", "S", "#c:ingots/steel");

        // 1.7 CraftingManager
        shaped(NS, "polaroid", "hbm:polaroid", rows(" C ", "RPY", " B "),
                "B", "#c:dusts/lapis", "C", "#c:dusts/coal", "R", "#c:dusts/red_copper", "Y", "#c:dusts/gold",
                "P", "minecraft:paper");
        shaped(NS, "containment_box", "hbm:containment_box", rows("LUL", "UCU", "LUL"),
                "L", "#c:plates/lead", "U", "#c:ingots/ferrouranium", "C", "hbm:crate_steel");
        shaped(NS, "rtty_pager", "hbm:rtty_pager", rows("R", "C", "S"),
                "R", "hbm:radio_torch_receiver", "C", "hbm:circuit_basic", "S", "#c:plates/steel");
        shaped(NS, "rtty_designator", "hbm:rtty_designator", rows("R", "C", "T"),
                "R", "hbm:radio_torch_reader", "C", "hbm:circuit_analog", "T", "hbm:rangefinder");
        shaped(NS, "fluid_identifier_multi", "hbm:fluid_identifier_multi", rows("D", "C", "P"),
                "D", "#c:dyes", "C", "hbm:circuit_vacuum_tube", "P", "#c:plates/iron");
        shapeless(NS, "book_guide_rbmk", "hbm:book_guide_rbmk",
                "minecraft:book", "minecraft:potato");
        shapeless(NS, "book_guide_starter", "hbm:book_guide_starter",
                "minecraft:book", "minecraft:iron_ingot");
        shaped(NS, "book_guide_block", "hbm:book_guide_block", rows("IBI", "LBL", "IBI"),
                "B", "minecraft:book", "I", "#c:dyes/black", "L", "#c:dyes/blue");
    }
}
