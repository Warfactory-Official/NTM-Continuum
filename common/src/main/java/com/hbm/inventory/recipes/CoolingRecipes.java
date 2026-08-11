package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The small, explicit subset of the old fluid cooling traits used by steam machinery. Keeping this
 * data here avoids restoring the old Forge-fluid trait framework just for turbines and condensers.
 */
public final class CoolingRecipes {

    public record CoolingRecipe(FluidType input, int inputAmount, FluidType output, int outputAmount, int heatEnergy) {}

    private CoolingRecipes() {}

    private static final Map<FluidType, CoolingRecipe> COOLING = new LinkedHashMap<>();
    private static boolean initialized = false;

    /** The cooling step for this input fluid, or null when it can't be cooled (no turbine efficiency). */
    public static CoolingRecipe get(FluidType input) {
        init();
        return COOLING.get(input);
    }

    /** Read-only view for JEI display. */
    public static Map<FluidType, CoolingRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(COOLING);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        put(new CoolingRecipe(Fluids.STEAM, 100, Fluids.SPENTSTEAM, 1, 200));
        put(new CoolingRecipe(Fluids.HOTSTEAM, 1, Fluids.STEAM, 10, 2));
        put(new CoolingRecipe(Fluids.SUPERHOTSTEAM, 1, Fluids.HOTSTEAM, 10, 18));
        put(new CoolingRecipe(Fluids.ULTRAHOTSTEAM, 1, Fluids.SUPERHOTSTEAM, 10, 120));
    }

    private static void put(CoolingRecipe recipe) {
        COOLING.put(recipe.input(), recipe);
    }
}
