package com.hbm.blocks;

import dev.golem.core.dejson.Dejson;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Tint;
import dev.golem.core.dejson.Tints;

import java.util.ArrayList;
import java.util.List;

import static dev.golem.core.dejson.Crafting.rows;
import static dev.golem.core.dejson.Crafting.shaped;

/**
 * Dejson declarations for the bedrock ore grades and the energy extras' 1.7 recipes. Touches no Minecraft class: it
 * runs in a plain JVM from {@code dejson.gradle.kts}, so the 1.7 grade table is restated here as data. The energy
 * extras' own models, blockstates, loot and tags are stated on their registrations in {@link ModEnergyBedrockBlocks}
 * (Reg); the grade items are registered by {@code ModBedrockOreItems}, whose chains state their models through
 * {@link #gradeModel}.
 *
 * <p>A bedrock ore grade draws the type's greyscale {@code bedrock_ore_<prefix>_<type>} art recoloured on the atlas
 * into the type's band (the sprites are computed, see {@code EnergyBedrockClientRegistrations}), multiplied by the
 * grade tint, with one {@code bedrock_ore_overlay.<trait>} layer per processing trait on top, untinted.
 */
public final class EnergyBedrockDeclarations {

    private static final String NS = "hbm";
    private static boolean declared;

    /** 1.7 {@code BedrockOreType} suffixes, in ordinal order. */
    public static final String[] TYPES = {"light", "heavy", "rare", "actinide", "nonmetal", "crystal"};

    /** 1.7 {@code BedrockOreGrade}: name, texture prefix, tint, traits. BASE (ordinal 0) is the slopper's. */
    public static final Object[][] GRADES = {
            {"base", "base", 0xFFFFFF, new String[]{}},
            {"base_roasted", "base", 0xCFCFCF, new String[]{"roasted"}},
            {"base_washed", "base", 0xDBE2CB, new String[]{"washed"}},
            {"primary", "primary", 0xFFFFFF, new String[]{"centrifuged"}},
            {"primary_roasted", "primary", 0xCFCFCF, new String[]{"roasted"}},
            {"primary_sulfuric", "primary", 0xFFFFD3, new String[]{"sulfuric"}},
            {"primary_nosulfuric", "primary", 0xD3D4FF, new String[]{"centrifuged", "sulfuric"}},
            {"primary_solvent", "primary", 0xD3F0FF, new String[]{"solvent"}},
            {"primary_nosolvent", "primary", 0xFFDED3, new String[]{"centrifuged", "solvent"}},
            {"primary_rad", "primary", 0xECFFD3, new String[]{"rad"}},
            {"primary_norad", "primary", 0xEBD3FF, new String[]{"centrifuged", "rad"}},
            {"primary_first", "primary", 0xFFD3D4, new String[]{"centrifuged"}},
            {"primary_second", "primary", 0xD3FFEB, new String[]{"centrifuged"}},
            {"crumbs", "crumbs", 0xFFFFFF, new String[]{"centrifuged"}},
            {"sulfuric_byproduct", "sulfuric", 0xFFFFFF, new String[]{"centrifuged", "sulfuric"}},
            {"sulfuric_roasted", "sulfuric", 0xCFCFCF, new String[]{"roasted", "sulfuric"}},
            {"sulfuric_arc", "sulfuric", 0xC3A2A2, new String[]{"arc", "sulfuric"}},
            {"sulfuric_washed", "sulfuric", 0xDBE2CB, new String[]{"washed", "sulfuric"}},
            {"solvent_byproduct", "solvent", 0xFFFFFF, new String[]{"centrifuged", "solvent"}},
            {"solvent_roasted", "solvent", 0xCFCFCF, new String[]{"roasted", "solvent"}},
            {"solvent_arc", "solvent", 0xC3A2A2, new String[]{"arc", "solvent"}},
            {"solvent_washed", "solvent", 0xDBE2CB, new String[]{"washed", "solvent"}},
            {"rad_byproduct", "rad", 0xFFFFFF, new String[]{"centrifuged", "rad"}},
            {"rad_roasted", "rad", 0xCFCFCF, new String[]{"roasted", "rad"}},
            {"rad_arc", "rad", 0xC3A2A2, new String[]{"arc", "rad"}},
            {"rad_washed", "rad", 0xDBE2CB, new String[]{"washed", "rad"}},
    };

    /** The five greyscale prefixes that are recoloured (BASE ships pre-coloured art under its own ids). */
    public static final String[] PREFIXES = {"primary", "crumbs", "sulfuric", "solvent", "rad"};

    /** The permutation suffix of a recoloured bedrock ore sprite. */
    public static final String REMAPPED = "colored";

    private EnergyBedrockDeclarations() {}

    /** The atlas id of a type's recoloured art for a grade prefix. */
    public static String sprite(String prefix, String type) {
        if (prefix.equals("base")) return NS + ":items/bedrock_ore_base_" + type;
        return NS + ":items/bedrock_ore/" + prefix + "_" + type + "_" + REMAPPED;
    }

    public static void declare() {
        if (declared) return;
        declared = true;
        bedrockOres();
        recipes();
    }

    private static void bedrockOres() {
        for (String prefix : PREFIXES) {
            for (String type : TYPES) {
                Dejson.generatedSprites(NS + ":items/bedrock_ore/" + prefix + "_" + type, "_", List.of(REMAPPED));
            }
        }
        // The grade items' own models are on their registrations (ModBedrockOreItems, through gradeModel).
    }

    /**
     * The model of {@code bedrock_ore_<grade>_<type>}: the type's art for the grade's prefix, tinted by the grade,
     * then one untinted overlay per processing trait. Called from {@code ModBedrockOreItems}' registration chain.
     */
    public static Dejson.ItemDeclaration gradeModel(Dejson.ItemDeclaration item, String grade, String type) {
        for (int g = 1; g < GRADES.length; g++) {
            if (!GRADES[g][0].equals(grade)) continue;
            String prefix = (String) GRADES[g][1];
            int tint = (Integer) GRADES[g][2];
            String[] traits = (String[]) GRADES[g][3];
            List<String> layers = new ArrayList<>();
            List<Tint> tints = new ArrayList<>();
            layers.add(sprite(prefix, type));
            tints.add(Tints.constant(0xFF000000 | tint));
            for (String trait : traits) {
                layers.add(NS + ":items/bedrock_ore_overlay." + trait);
                tints.add(Tints.constant(-1));
            }
            return item.models(Models.generated(layers.toArray(new String[0]))).tints(tints.toArray(new Tint[0]));
        }
        throw new IllegalArgumentException("not a processed bedrock ore grade: " + grade);
    }

    private static void recipes() {
        // 1.7 CraftingManager
        shaped(NS, "red_wire_coated", "hbm:red_wire_coated", 16, rows("WRW", "RIR", "WRW"),
                "W", "hbm:plate_polymer", "I", "#c:ingots/red_copper", "R", "#c:wires/red_copper");
        shaped(NS, "machine_converter_he_rf", "hbm:machine_converter_he_rf", rows("RRR", "WWW", "III"),
                "R", "hbm:circuit_capacitor", "W", "#c:dusts/redstone", "I", "#c:ingots/steel");
        shaped(NS, "machine_converter_rf_he", "hbm:machine_converter_rf_he", rows("RRR", "WWW", "III"),
                "R", "#c:dusts/redstone", "W", "#c:wires/red_copper", "I", "#c:ingots/steel");
        shaped(NS, "charger", "hbm:charger", rows("G", "S", "C"),
                "G", "minecraft:glowstone_dust", "S", "#c:ingots/steel", "C", "hbm:coil_copper");
        shaped(NS, "charger_16", "hbm:charger", 16, rows("G", "S", "C"),
                "G", "minecraft:glowstone", "S", "#c:storage_blocks/steel", "C", "hbm:coil_copper_torus");
    }
}
