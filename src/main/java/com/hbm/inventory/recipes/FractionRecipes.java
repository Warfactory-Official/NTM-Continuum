package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fraction tower recipes — 1:1 port of the full 1.12.2 {@code FractionRecipes} table (all 19 entries).
 * Each input (100 mB per fractionate op) splits into two fluids (mB amounts as given). All output
 * fluids exist in the port's 157-fluid registry, so nothing is skipped.
 */
public final class FractionRecipes {

    private FractionRecipes() {}

    public record FractionPair(FluidStack left, FluidStack right) {}

    private static final Map<FluidType, FractionPair> fractions = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static FractionPair getFractions(FluidType input) {
        init();
        return fractions.get(input);
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, FractionPair> getAll() {
        init();
        return Collections.unmodifiableMap(fractions);
    }

    private static void put(FluidType in, FluidType l, int lAmt, FluidType r, int rAmt) {
        fractions.put(in, new FractionPair(new FluidStack(l, lAmt), new FluidStack(r, rAmt)));
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        put(Fluids.HEAVYOIL, Fluids.BITUMEN, 30, Fluids.SMEAR, 70);
        put(Fluids.HEAVYOIL_VACUUM, Fluids.SMEAR, 40, Fluids.HEATINGOIL_VACUUM, 60);
        put(Fluids.SMEAR, Fluids.HEATINGOIL, 60, Fluids.LUBRICANT, 40);
        put(Fluids.NAPHTHA, Fluids.HEATINGOIL, 40, Fluids.DIESEL, 60);
        put(Fluids.NAPHTHA_DS, Fluids.XYLENE, 60, Fluids.DIESEL_REFORM, 40);
        put(Fluids.NAPHTHA_CRACK, Fluids.HEATINGOIL, 30, Fluids.DIESEL_CRACK, 70);
        put(Fluids.LIGHTOIL, Fluids.DIESEL, 40, Fluids.KEROSENE, 60);
        put(Fluids.LIGHTOIL_DS, Fluids.DIESEL_REFORM, 60, Fluids.KEROSENE_REFORM, 40);
        put(Fluids.LIGHTOIL_CRACK, Fluids.KEROSENE, 70, Fluids.PETROLEUM, 30);
        put(Fluids.COALOIL, Fluids.COALGAS, 30, Fluids.OIL, 70);
        put(Fluids.COALCREOSOTE, Fluids.COALOIL, 10, Fluids.BITUMEN, 90);
        put(Fluids.REFORMATE, Fluids.AROMATICS, 40, Fluids.XYLENE, 60);
        put(Fluids.LIGHTOIL_VACUUM, Fluids.KEROSENE, 70, Fluids.REFORMGAS, 30);
        put(Fluids.EGG, Fluids.CHOLESTEROL, 50, Fluids.RADIOSOLVENT, 50);
        put(Fluids.OIL_COKER, Fluids.CRACKOIL, 30, Fluids.HEATINGOIL, 70);
        put(Fluids.NAPHTHA_COKER, Fluids.NAPHTHA_CRACK, 75, Fluids.LIGHTOIL_CRACK, 25);
        put(Fluids.GAS_COKER, Fluids.AROMATICS, 25, Fluids.CARBONDIOXIDE, 75);
        put(Fluids.CHLOROCALCITE_MIX, Fluids.CHLOROCALCITE_CLEANED, 50, Fluids.COLLOID, 50);
        put(Fluids.BAUXITE_SOLUTION, Fluids.REDMUD, 50, Fluids.SODIUM_ALUMINATE, 50);
        dev.golem.core.content.Replay.apply(FractionRecipes.class);
    }

    public static void put(FluidType in, FractionPair pair) {
        init();
        fractions.put(in, pair);
    }

    public static boolean remove(FluidType in) {
        init();
        return fractions.remove(in) != null;
    }

    public static synchronized void reset() {
        fractions.clear();
        initialized = false;
    }
}
