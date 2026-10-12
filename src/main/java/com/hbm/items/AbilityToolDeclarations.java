package com.hbm.items;

import com.hbm.lib.ItemIds;
import com.hbm.lib.TagIds;
import com.hbm.lib.VanillaIds;
import dev.golem.core.dejson.Tags;

import static dev.golem.core.dejson.Crafting.componentSmelt;
import static dev.golem.core.dejson.Crafting.item;
import static dev.golem.core.dejson.Crafting.requires;
import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;
import static dev.golem.core.dejson.Crafting.smelt;
import static dev.golem.core.dejson.Crafting.stack;

/**
 * Dejson declarations for the ability tool family ({@link ModAbilityToolItems}): the repair tags of the 1.7 tool
 * materials and the crafting and furnace recipes of 1.7 {@code ToolRecipes}, {@code WeaponRecipes},
 * {@code CraftingManager} and {@code SmeltingRecipes}. The item models and the vanilla tool-class item tags
 * (enchanting and anvil applicability) are stated on the registrations. Touches no Minecraft class.
 */
public final class AbilityToolDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    private AbilityToolDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        // The tool-class tags' members are stated on the registrations in ModAbilityToolItems; minecraft:swords also
        // nests the WeaponSpecial family's tag, whose members ModMeleeKitItems states.
        Tags.item("minecraft", "swords", "#hbm:melee_kit_swords");

        // Repair items of the 1.7 materials that the existing repair tags do not already name.
        Tags.item(NS, "repairs/cmb", ItemIds.ingot_combine_steel);
        Tags.item(NS, "repairs/desh", ItemIds.ingot_desh);
        Tags.item(NS, "repairs/bismuth", ItemIds.ingot_bismuth);
        Tags.item(NS, "repairs/chlorophyte", ItemIds.powder_chlorophyte);
        Tags.item(NS, "repairs/copper", ItemIds.ingot_copper);
        Tags.item(NS, "repairs/cobalt_ingot", ItemIds.ingot_cobalt);
        Tags.item(NS, "repairs/starmetal_ingot", ItemIds.ingot_starmetal);

        recipes();
    }

    private static void recipes() {
        String stick = VanillaIds.stick;

        // 1.7 ToolRecipes: regular tools
        toolSet("steel", TagIds.ingots_steel, stick);
        toolSet("titanium", TagIds.ingots_titanium, stick);
        toolSet("cobalt", TagIds.ingots_cobalt, stick);
        toolSet("cmb", TagIds.ingots_combine_steel, stick);
        toolSet("desh", TagIds.ingots_desh, stick);

        String plastic = TagIds.ingots_any_plastic;
        String duraBolt = TagIds.bolts_dura_steel;
        shaped(NS, "elec_sword", "hbm:elec_sword", rows("RPR", "RPR", " B "),
                "P", plastic, "R", duraBolt, "B", ItemIds.battery_lead);
        shaped(NS, "elec_pickaxe", "hbm:elec_pickaxe", rows("RDM", " PB", " P "),
                "P", plastic, "D", TagIds.ingots_dura_steel, "R", duraBolt, "M", ItemIds.motor, "B", ItemIds.battery_lead);
        shaped(NS, "elec_axe", "hbm:elec_axe", rows(" DP", "RRM", " PB"),
                "P", plastic, "D", TagIds.ingots_dura_steel, "R", duraBolt, "M", ItemIds.motor, "B", ItemIds.battery_lead);
        shaped(NS, "elec_shovel", "hbm:elec_shovel", rows("  P", "RRM", "  B"),
                "P", plastic, "R", duraBolt, "M", ItemIds.motor, "B", ItemIds.battery_lead);
        shaped(NS, "smashing_hammer", "hbm:smashing_hammer", rows("STS", "SPS", " P "),
                "S", TagIds.storage_blocks_steel, "T", TagIds.storage_blocks_tungsten, "P", plastic);
        shaped(NS, "meteorite_sword", "hbm:meteorite_sword", rows("  B", "GB ", "SG "),
                "B", "hbm:blade_meteorite", "G", TagIds.plates_gold, "S", stick);
        shaped(NS, "dwarven_pickaxe", "hbm:dwarven_pickaxe", rows("CIC", " S ", " S "),
                "C", TagIds.ingots_copper, "I", TagIds.ingots_iron, "S", stick);

        // Super pickaxes and axes
        for (String kind : new String[]{"pickaxe", "axe"}) {
            shaped(NS, "bismuth_" + kind, "hbm:bismuth_" + kind, rows(" BM", "BPB", "TB "),
                    "B", TagIds.ingots_bismuth, "M", "hbm:ingot_meteorite", "P", "hbm:starmetal_" + kind, "T", TagIds.bolts_tungsten);
            shaped(NS, "volcanic_" + kind, "hbm:volcanic_" + kind, rows(" BM", "BPB", "TB "),
                    "B", ItemIds.gem_volcanic, "M", "hbm:ingot_meteorite", "P", "hbm:starmetal_" + kind, "T", TagIds.bolts_tungsten);
            shaped(NS, "chlorophyte_" + kind, "hbm:chlorophyte_" + kind, rows(" SD", "APS", "FA "),
                    "S", ItemIds.blades_steel, "D", ItemIds.powder_chlorophyte, "A", TagIds.ingots_fiberglass,
                    "P", "hbm:bismuth_" + kind, "F", duraBolt);
            shaped(NS, "chlorophyte_" + kind + "_1", "hbm:chlorophyte_" + kind, rows(" SD", "APS", "FA "),
                    "S", ItemIds.blades_steel, "D", ItemIds.powder_chlorophyte, "A", TagIds.ingots_fiberglass,
                    "P", "hbm:volcanic_" + kind, "F", duraBolt);
            shaped(NS, "mese_" + kind, "hbm:mese_" + kind, rows(" SD", "APS", "FA "),
                    "S", ItemIds.blades_desh, "D", ItemIds.powder_dineutronium, "A", ItemIds.plate_paa,
                    "P", "hbm:chlorophyte_" + kind, "F", ItemIds.shimmer_handle);
        }

        // Chainsaw: crafted empty (1.7 ItemToolAbilityFueled.getEmptyTool). 1.7's ModBlocks.chain is the
        // port's chain tag.
        shaped(NS, "chainsaw", stack("hbm:chainsaw").with("hbm:tool_fuel", 0), rows("CCH", "BBP", "CCE"),
                "H", TagIds.shells_steel, "B", ItemIds.blades_steel, "P", ItemIds.piston_selenium,
                "C", TagIds.chains, "E", ItemIds.canister_empty);

        // Configged: the default (non-LBSM) upgrade recipes
        String star = TagIds.ingots_starmetal;
        String ring = ItemIds.ring_starmetal;
        requires(shaped(NS, "starmetal_sword", "hbm:starmetal_sword", rows(" I ", " B ", "ISI"),
                "I", star, "S", ring, "B", "hbm:cobalt_decorated_sword"), "!lbsm_simple_tool");
        requires(shaped(NS, "starmetal_pickaxe", "hbm:starmetal_pickaxe", rows("ISI", " B ", " I "),
                "I", star, "S", ring, "B", "hbm:cobalt_decorated_pickaxe"), "!lbsm_simple_tool");
        requires(shaped(NS, "starmetal_axe", "hbm:starmetal_axe", rows("IS", "IB", " I"),
                "I", star, "S", ring, "B", "hbm:cobalt_decorated_axe"), "!lbsm_simple_tool");
        requires(shaped(NS, "starmetal_shovel", "hbm:starmetal_shovel", rows("I", "B", "I"),
                "I", star, "B", "hbm:cobalt_decorated_shovel"), "!lbsm_simple_tool");
        requires(shaped(NS, "starmetal_hoe", "hbm:starmetal_hoe", rows("IS", " B", " I"),
                "I", star, "S", ring, "B", "hbm:cobalt_decorated_hoe"), "!lbsm_simple_tool");
        String schrabBlock = TagIds.storage_blocks_schrabidium;
        requires(shaped(NS, "schrabidium_sword", "hbm:schrabidium_sword", rows("I", "W", "S"),
                "I", schrabBlock, "W", "hbm:desh_sword", "S", plastic), "!lbsm_simple_tool");
        requires(shaped(NS, "schrabidium_pickaxe", "hbm:schrabidium_pickaxe", rows("BSB", " W ", " P "),
                "B", ItemIds.blades_desh, "S", schrabBlock, "W", "hbm:desh_pickaxe", "P", plastic), "!lbsm_simple_tool");
        requires(shaped(NS, "schrabidium_axe", "hbm:schrabidium_axe", rows("BS", "BW", " P"),
                "B", ItemIds.blades_desh, "S", schrabBlock, "W", "hbm:desh_axe", "P", plastic), "!lbsm_simple_tool");
        requires(shaped(NS, "schrabidium_shovel", "hbm:schrabidium_shovel", rows("B", "W", "P"),
                "B", schrabBlock, "W", "hbm:desh_shovel", "P", plastic), "!lbsm_simple_tool");
        requires(shaped(NS, "schrabidium_hoe", "hbm:schrabidium_hoe", rows("IW", " S", " S"),
                "I", TagIds.ingots_schrabidium, "W", "hbm:desh_hoe", "S", plastic), "!lbsm_simple_tool");

        // 1.7 CraftingManager:97
        shaped(NS, "redstone_sword", "hbm:redstone_sword", rows("R", "R", "S"),
                "R", TagIds.storage_blocks_redstone, "S", stick);

        // old-hbm WeaponRecipes:334 — new ItemStack(crucible, 1, 3): item damage 3 and no charges (an empty crucible).
        shaped(NS, "crucible", stack("hbm:crucible").with("minecraft:damage", 3), rows("MEM", "YDY", "YCY"),
                "M", "hbm:ingot_meteorite_forged", "E", TagIds.ingots_euphemium, "Y", ItemIds.billet_yharonite,
                "D", ItemIds.demon_core_closed, "C", ItemIds.ingot_chainsteel);

        // 1.7 SmeltingRecipes:168-171: the furnace reheats the meteorite materials (ItemHot.heatUp), and
        // sears the sword. heatUp on the seared sword (not an ItemHot) did nothing, so that one is plain.
        // 1.7 ItemHot.heat is static, so the last constructor (ingot_chainsteel, 100) wins and heatUp writes 100 for
        // every hot item although the meteorite items were constructed with 200 (History §19ez; LEGACY_HEAT).
        for (String id : new String[]{"ingot_meteorite", "ingot_meteorite_forged", "blade_meteorite"}) {
            componentSmelt(NS, id + "_heat", item("hbm:" + id),
                    stack("hbm:" + id).with("hbm:heat", com.hbm.items.special.ItemHot.LEGACY_HEAT), 0.0);
        }
        smelt(NS, "meteorite_sword_seared", "hbm:meteorite_sword", "hbm:meteorite_sword_seared", 0.0);
    }

    /** 1.7 {@code ToolRecipes.addSword/addPickaxe/addAxe/addShovel/addHoe}: X the material, # a stick. */
    private static void toolSet(String set, String ingot, String stick) {
        shaped(NS, set + "_sword", "hbm:" + set + "_sword", rows("X", "X", "#"), "X", ingot, "#", stick);
        shaped(NS, set + "_pickaxe", "hbm:" + set + "_pickaxe", rows("XXX", " # ", " # "), "X", ingot, "#", stick);
        shaped(NS, set + "_axe", "hbm:" + set + "_axe", rows("XX", "X#", " #"), "X", ingot, "#", stick);
        shaped(NS, set + "_shovel", "hbm:" + set + "_shovel", rows("X", "#", "#"), "X", ingot, "#", stick);
        shaped(NS, set + "_hoe", "hbm:" + set + "_hoe", rows("XX", " #", " #"), "X", ingot, "#", stick);
    }
}
