package com.hbm.items;

import dev.golem.core.dejson.DataMaps;

/**
 * NeoForge's {@code neoforge:furnace_fuels} data map for hbm's fuels, declared through Golem Core's
 * {@code dejson} vocabulary instead of written as JSON. Touches no Minecraft class.
 *
 * <p>The same burn times reach Fabric and Forge at registration from {@link ModFuelItems#BURN_TIMES}; this
 * is the NeoForge half, which is data. The two cannot share one table because that one holds registry
 * suppliers, which a plain JVM cannot load. <b>Keep the two in sync</b>: an item added there without a line
 * here burns everywhere but NeoForge.
 */
public final class FuelDataMapDeclarations {

    private static boolean declared;

    private FuelDataMapDeclarations() {}

    public static void declare() {
        if (declared) return;
        declared = true;

        DataMaps.furnaceFuels()
                .burnTime("hbm:powder_lignite", 1200)
                .burnTime("hbm:powder_coal", 1600)
                .burnTime("hbm:powder_fire", 6400)
                .burnTime("hbm:biomass", 400)
                .burnTime("hbm:biomass_compressed", 800)
                .burnTime("hbm:coal_infernal", 4800)
                .burnTime("hbm:solid_fuel", 3200)
                .burnTime("hbm:solid_fuel_presto", 8000)
                .burnTime("hbm:solid_fuel_presto_triplet", 40000)
                .burnTime("hbm:rocket_fuel", 6400)
                .burnTime("hbm:lignite", 1200)
                .burnTime("hbm:crystal_coal", 6400)
                .burnTime("hbm:briquette_coal", 2000)
                .burnTime("hbm:briquette_lignite", 1600)
                .burnTime("hbm:briquette_wood", 400)
                .burnTime("hbm:solid_fuel_bf", 32000)
                .burnTime("hbm:solid_fuel_presto_bf", 80000)
                .burnTime("hbm:solid_fuel_presto_triplet_bf", 400000)
                .burnTime("hbm:scrap", 50)
                .burnTime("hbm:dust", 25)
                .burnTime("hbm:block_scrap", 400)
                .burnTime("hbm:coke_coal", 3200)
                .burnTime("hbm:block_coke_coal", 32000)
                .burnTime("hbm:coke_lignite", 3200)
                .burnTime("hbm:block_coke_lignite", 32000)
                .burnTime("hbm:coke_petroleum", 3200)
                .burnTime("hbm:block_coke_petroleum", 32000)
                .burnTime("hbm:book_guide", 200)
                .burnTime("hbm:coal_eternal", 3200)
                .burnTime("hbm:powder_sawdust", 100)
                .burnTime("hbm:powder_ash_wood", 100)
                .burnTime("hbm:powder_ash_coal", 200)
                .burnTime("hbm:powder_ash_misc", 100)
                .burnTime("hbm:powder_ash_fly", 200)
                .burnTime("hbm:powder_ash_soot", 100)
                .register();
    }
}
