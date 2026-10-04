package com.hbm.inventory.material;

import dev.golem.core.dejson.Tags;

/**
 * Every {@code c:} convention tag hbm's materials belong to, declared through Golem Core's
 * {@code dejson} vocabulary instead of written as JSON.
 *
 * <p>The {@value #FILES_REPLACED} tag files these declarations stand in for have been deleted.
 * 578 of them said the only thing a material tag usually says — that {@code c:ingots/iron} contains
 * {@code hbm:ingot_iron} — which is not a fact about iron so much as a restatement of
 * {@link MaterialShapes}, one file at a time. Those are now a single line per material listing the
 * shapes it comes in, and the item name is derived from the shape exactly as the item's own
 * registration derives it.
 *
 * <p>Those derivable lines, and the tags the registrations imply that are not derivable (an item whose
 * name does not follow its shape, {@code c:ingots/carbon} being {@code hbm:ingot_graphite}; a tag that also
 * admits a vanilla item; the {@code any_*} aggregates, which are tags of tags), are written by
 * {@code port-tools/gen_material_tags.py} into {@link ModMaterialTagsGenerated}. Re-run the generator after a
 * content wave; do not edit that class. What is left here are the tags no registration implies: dyes, tars,
 * glass, the tiny dusts named differently from their items, and the one block tag.
 *
 * <p>{@link com.hbm.gametest.MaterialTagGameTests} is the other half: it asks the live item
 * registry which {@code <prefix>_<material>} items actually exist and fails if one of them is in no
 * tag, which is the failure this file cannot see on its own — a silently untagged item makes a
 * recipe reject a valid ingredient and nothing anywhere reports it.
 */
public final class ModMaterialTags {

    private static final String NS = "c";

    /** How many tag files the declarations below stand in for; quoted in this class's own javadoc. */
    static final int FILES_REPLACED = 794;

    private static boolean declared;

    private ModMaterialTags() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        // Original ModSword's steel material repaired with ingot_steel; this tag also gates every
        // legacy crate-opening path through BlockCrateSupply.CROWBAR.
        Tags.item("hbm", "crowbar", "hbm:crowbar");

        ModMaterialTagsGenerated.declare();
        named();
    }

    private static void tag(String path, String... values) {
        Tags.item(NS, path, values);
    }

    private static void blockTag(String path, String... values) {
        Tags.block(NS, path, values);
    }

    /** The tags no registration implies; see the class comment. */
    private static void named() {
        blockTag("ores/uranium", "hbm:ore_uranium", "hbm:ore_uranium_scorched", "hbm:ore_gneiss_uranium",
                "hbm:ore_gneiss_uranium_scorched", "hbm:ore_nether_uranium", "hbm:ore_nether_uranium_scorched");
        tag("cobblestones", "minecraft:cobblestone");
        tag("cokes", "hbm:coke_coal", "hbm:coke_lignite", "hbm:coke_petroleum");
        tag("concretes", "hbm:concrete", "hbm:concrete_smooth", "hbm:concrete_asbestos", "hbm:ducrete",
                "hbm:ducrete_smooth", "hbm:concrete_colored", "hbm:concrete_colored_ext");
        tag("dusts/actinium_tiny", "hbm:powder_actinium_tiny");
        tag("dusts/ash", "hbm:powder_ash_wood", "hbm:powder_ash_coal", "hbm:powder_ash_misc", "hbm:powder_ash_fly",
                "hbm:powder_ash_soot");
        tag("dusts/boron_tiny", "hbm:powder_boron_tiny");
        tag("dusts/lanthanium_tiny", "hbm:powder_lanthanium_tiny");
        tag("dusts/meteorite_tiny", "hbm:powder_meteorite_tiny");
        tag("dusts/steel_tiny", "hbm:powder_steel_tiny");
        tag("dyes/black", "hbm:chemical_dye_black", "hbm:crayon_black", "hbm:powder_ash_coal",
                "hbm:powder_ash_soot", "hbm:powder_coal", "hbm:oil_tar_crack", "hbm:oil_tar_crude",
                "minecraft:black_dye");
        tag("dyes/blue", "hbm:chemical_dye_blue", "hbm:crayon_blue", "hbm:powder_lapis", "minecraft:blue_dye");
        tag("dyes/brown", "hbm:chemical_dye_brown", "hbm:crayon_brown", "hbm:powder_ash_fly", "hbm:powder_lignite",
                "minecraft:brown_dye");
        tag("dyes/cyan", "hbm:chemical_dye_cyan", "hbm:crayon_cyan", "hbm:oil_tar_wax", "minecraft:cyan_dye");
        tag("dyes/gray", "hbm:chemical_dye_gray", "hbm:crayon_gray", "hbm:oil_tar_coal", "hbm:powder_ash_misc",
                "minecraft:gray_dye");
        tag("dyes/green", "hbm:chemical_dye_green", "hbm:crayon_green", "minecraft:green_dye");
        tag("dyes/light_blue", "hbm:chemical_dye_lightblue", "hbm:crayon_lightblue", "minecraft:light_blue_dye");
        tag("dyes/light_gray", "hbm:chemical_dye_silver", "hbm:crayon_silver", "hbm:powder_ash_wood",
                "hbm:powder_titanium", "minecraft:light_gray_dye");
        tag("dyes/lime", "hbm:chemical_dye_lime", "hbm:crayon_lime", "minecraft:lime_dye");
        tag("dyes/magenta", "hbm:chemical_dye_magenta", "hbm:crayon_magenta", "hbm:powder_ash_fullerene",
                "minecraft:magenta_dye");
        tag("dyes/orange", "hbm:chemical_dye_orange", "hbm:crayon_orange", "hbm:powder_cadmium",
                "minecraft:orange_dye");
        tag("dyes/pink", "hbm:chemical_dye_pink", "hbm:crayon_pink", "minecraft:pink_dye");
        tag("dyes/purple", "hbm:chemical_dye_purple", "hbm:crayon_purple", "minecraft:purple_dye");
        tag("dyes/red", "hbm:chemical_dye_red", "hbm:crayon_red", "hbm:cinnabar", "minecraft:red_dye");
        tag("dyes/white", "hbm:chemical_dye_white", "hbm:crayon_white", "hbm:fluorite", "hbm:oil_tar_paraffin",
                "minecraft:white_dye");
        tag("dyes/yellow", "hbm:chemical_dye_yellow", "hbm:crayon_yellow", "hbm:sulfur", "minecraft:yellow_dye");
        // 1.7 KEY_ANYGLASS "blockGlass": vanilla glass and every stained glass (stated here, like glass_panes, so 1.20.1
        // Forge, which has no conventional c:glass_blocks, matches the same items as the newer loaders)
        tag("glass_blocks", "hbm:glass_boron", "hbm:glass_lead",
                "minecraft:glass", "minecraft:white_stained_glass", "minecraft:orange_stained_glass",
                "minecraft:magenta_stained_glass", "minecraft:light_blue_stained_glass",
                "minecraft:yellow_stained_glass", "minecraft:lime_stained_glass", "minecraft:pink_stained_glass",
                "minecraft:gray_stained_glass", "minecraft:light_gray_stained_glass", "minecraft:cyan_stained_glass",
                "minecraft:purple_stained_glass", "minecraft:blue_stained_glass", "minecraft:brown_stained_glass",
                "minecraft:green_stained_glass", "minecraft:red_stained_glass", "minecraft:black_stained_glass");
        tag("glass_panes", "minecraft:glass_pane", "minecraft:white_stained_glass_pane",
                "minecraft:orange_stained_glass_pane", "minecraft:magenta_stained_glass_pane",
                "minecraft:light_blue_stained_glass_pane", "minecraft:yellow_stained_glass_pane",
                "minecraft:lime_stained_glass_pane", "minecraft:pink_stained_glass_pane",
                "minecraft:gray_stained_glass_pane", "minecraft:light_gray_stained_glass_pane",
                "minecraft:cyan_stained_glass_pane", "minecraft:purple_stained_glass_pane",
                "minecraft:blue_stained_glass_pane", "minecraft:brown_stained_glass_pane",
                "minecraft:green_stained_glass_pane", "minecraft:red_stained_glass_pane",
                "minecraft:black_stained_glass_pane");
        tag("glyphid_meats", "hbm:glyphid_meat", "hbm:glyphid_meat_grilled");
        tag("nuggets/gold", "minecraft:gold_nugget");
        tag("rods/wooden", "minecraft:stick");
        tag("storage_blocks/cdalloy", "hbm:block_cdalloy");
        tag("storage_blocks/lithium", "hbm:block_lithium");
        tag("storage_blocks/tcalloy", "hbm:block_tcalloy");
        tag("tars", "hbm:oil_tar_crude", "hbm:oil_tar_coal", "hbm:oil_tar_crack", "hbm:oil_tar_wood");
    }
}
