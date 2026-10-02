package com.hbm.blocks;

import dev.golem.core.dejson.Crafting;

/**
 * The terrain family's packing recipes and grass tint; every block's models, blockstate, item model, loot and
 * tags are stated on its registration in {@link ModTerrainBlocks} / {@code ModTerrainItems} (Reg). Touches no
 * Minecraft class (it runs in a plain JVM from {@code dejson.gradle.kts}).
 *
 * <p>Authored recipes: the painted-waste, rope and strong-radaway recipes under {@code data/hbm/recipe/}.
 */
public final class TerrainDeclarations {

    private static final String NS = "hbm";
    /**
     * 1.7 {@code ColorizerGrass.getGrassColor(0.5, 1.0)} for the tobacco/hemp tint, as a constant: 26.1
     * tints are state-keyed and cannot read the biome (see {@code dev.golem.core.client.BlockTint}).
     */
    public static final int GRASS_TINT = 0x63A948;

    private static boolean declared;

    private TerrainDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;
        recipes();
    }

    /** {@code MineralRecipes.add1To9Pair} and the block/nine-item pairs of this family. */
    private static void recipes() {
        pair("block_tcalloy", "hbm:ingot_tcalloy");
        pair("block_cdalloy", "hbm:ingot_cdalloy");
        pair("block_coke_coal", "hbm:coke_coal");
        pair("block_coke_lignite", "hbm:coke_lignite");
        pair("block_coke_petroleum", "hbm:coke_petroleum");
        pair("block_fallout", "hbm:falloutitem");
        pair("block_waste", "hbm:nuclear_waste");
        pair("block_australium", "hbm:ingot_australium");
        pair("block_yellowcake", "hbm:powder_yellowcake");
        pair("block_lithium", "hbm:lithium");
    }

    private static void pair(String block, String item) {
        Crafting.compress(NS, "terrain_" + block, item, "hbm:" + block);
        Crafting.decompress(NS, "terrain_" + block + "_unpack", "hbm:" + block, item);
    }
}
