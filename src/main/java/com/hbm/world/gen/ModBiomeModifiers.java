package com.hbm.world.gen;

import static dev.golem.core.dejson.BiomeModifiers.LOCAL_MODIFICATIONS;
import static dev.golem.core.dejson.BiomeModifiers.UNDERGROUND_ORES;
import static dev.golem.core.dejson.BiomeModifiers.addFeatures;

/**
 * Where hbm's world features are added to biomes on the forge-family loaders, declared through
 * Golem Core's {@code dejson} vocabulary instead of written as JSON.
 *
 * <p>The {@value #FILES_REPLACED} files these 9 declarations stand in for have been deleted: the
 * same nine modifiers written once under {@code neoforge} and once under {@code forge}, differing
 * only in the namespace on their {@code type}. Nothing checked that the two copies agreed, so a
 * feature added at the wrong step on one loader would have generated differently there and looked
 * like a worldgen bug rather than a missed edit.
 *
 * <p>Fabric has no biome modifiers; ore placement there is wired in code, and these files were
 * inert on that target before and are inert on it now.
 */
public final class ModBiomeModifiers {

    private static final String NS = "hbm";

    /** How many data files the declarations below stand in for; quoted in this class's own javadoc. */
    static final int FILES_REPLACED = 18;

    private static boolean declared;

    private ModBiomeModifiers() {}

    public static void declare() {
        if (declared) {
            return;
        }
        declared = true;

        addFeatures(NS, "bedrock_oil", "#minecraft:is_overworld", "hbm:bedrock_oil", UNDERGROUND_ORES);
        addFeatures(NS, "end_ores", "#minecraft:is_end", "hbm:end_ores", UNDERGROUND_ORES);
        addFeatures(NS, "nether_ores", "#minecraft:is_nether", "hbm:nether_ores", UNDERGROUND_ORES);
        addFeatures(NS, "oil_bubble", "#minecraft:is_overworld", "hbm:oil_bubble", UNDERGROUND_ORES);
        addFeatures(NS, "oil_sand_bubble", "#minecraft:is_overworld", "hbm:oil_sand_bubble", UNDERGROUND_ORES);
        addFeatures(NS, "ore_caves", "#minecraft:is_overworld", "hbm:ore_caves", LOCAL_MODIFICATIONS);
        addFeatures(NS, "ore_layers", "#minecraft:is_overworld", "hbm:ore_layers", LOCAL_MODIFICATIONS);
        addFeatures(NS, "overworld_ores", "#minecraft:is_overworld", "hbm:overworld_ores", UNDERGROUND_ORES);
        addFeatures(NS, "schist_stratum", "#minecraft:is_overworld", "hbm:schist_stratum", LOCAL_MODIFICATIONS);
    }
}
