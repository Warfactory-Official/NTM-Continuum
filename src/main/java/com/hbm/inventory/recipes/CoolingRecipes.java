package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only turbine projection of the authoritative {@link FT_Coolable} fluid traits. */
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

        Fluids.init();
        for (FluidType input : Fluids.getAll()) {
            FT_Coolable trait = input.getTrait(FT_Coolable.class);
            if (trait != null && trait.getEfficiency(CoolingType.TURBINE) > 0D) {
                put(new CoolingRecipe(input, trait.amountReq, trait.coolsTo, trait.amountProduced, trait.heatEnergy));
            }
        }
    }

    private static void put(CoolingRecipe recipe) {
        COOLING.put(recipe.input(), recipe);
    }
}
