package com.hbm.items;

import dev.golem.core.dejson.DataMaps;

/**
 * NeoForge's {@code neoforge:furnace_fuels} data map for hbm's fuels, declared through Golem Core's
 * {@code dejson} vocabulary instead of written as JSON. Touches no Minecraft class.
 *
 * <p>{@link #BURN_TIMES} is the single table: it holds item ids, not registry suppliers, so the plain-JVM Dejson
 * pass can read it, and {@link ModFuelItems#BURN_TIMES} resolves the same ids for Fabric and Forge.
 */
public final class FuelDataMapDeclarations {

    private static boolean declared;

    private FuelDataMapDeclarations() {}

    /**
     * Every HBM furnace fuel and its burn time in ticks (1.7 {@code FuelHandler}; vanilla coal = 1600). The one table: the
     * NeoForge {@code neoforge:furnace_fuels} data map below is declared from it, and Fabric and Forge register it at
     * start-up through Golem Core's {@code Fuels} ({@code ModFuelItems.BURN_TIMES}). An addon's fuel is a
     * {@code neoforge:furnace_fuels} entry of its own on NeoForge and a {@code Fuels.register} call elsewhere.
     */
    public static final java.util.Map<String, Integer> BURN_TIMES = burnTimes();

    private static java.util.Map<String, Integer> burnTimes() {
        java.util.Map<String, Integer> t = new java.util.LinkedHashMap<>();
        t.put("hbm:powder_lignite", 1200);
        t.put("hbm:powder_coal", 1600);
        t.put("hbm:powder_fire", 6400);
        t.put("hbm:biomass", 400);
        t.put("hbm:biomass_compressed", 800);
        t.put("hbm:coal_infernal", 4800);
        t.put("hbm:solid_fuel", 3200);
        t.put("hbm:solid_fuel_presto", 8000);
        t.put("hbm:solid_fuel_presto_triplet", 40000);
        t.put("hbm:rocket_fuel", 6400);
        t.put("hbm:lignite", 1200);
        t.put("hbm:crystal_coal", 6400);
        t.put("hbm:briquette_coal", 2000);
        t.put("hbm:briquette_lignite", 1600);
        t.put("hbm:briquette_wood", 400);
        t.put("hbm:solid_fuel_bf", 32000);
        t.put("hbm:solid_fuel_presto_bf", 80000);
        t.put("hbm:solid_fuel_presto_triplet_bf", 400000);
        t.put("hbm:scrap", 50);
        t.put("hbm:dust", 25);
        t.put("hbm:block_scrap", 400);
        t.put("hbm:coke_coal", 3200);
        t.put("hbm:block_coke_coal", 32000);
        t.put("hbm:coke_lignite", 3200);
        t.put("hbm:block_coke_lignite", 32000);
        t.put("hbm:coke_petroleum", 3200);
        t.put("hbm:block_coke_petroleum", 32000);
        t.put("hbm:book_guide_test", 200);
        t.put("hbm:book_guide_rbmk", 200);
        t.put("hbm:book_guide_hadron", 200);
        t.put("hbm:book_guide_starter", 200);
        t.put("hbm:coal_eternal", 3200);
        t.put("hbm:powder_sawdust", 100);
        t.put("hbm:powder_ash_wood", 100);
        t.put("hbm:powder_ash_coal", 200);
        t.put("hbm:powder_ash_misc", 100);
        t.put("hbm:powder_ash_fly", 200);
        t.put("hbm:powder_ash_soot", 100);
        return java.util.Collections.unmodifiableMap(t);
    }

    public static void declare() {
        if (declared) return;
        declared = true;

        var fuels = DataMaps.furnaceFuels();
        BURN_TIMES.forEach(fuels::burnTime);
        fuels.register();
    }
}
