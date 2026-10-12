package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Coolable.CoolingType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Read-only turbine projection of the {@link FT_Coolable} fluid trait, which is data ({@code coolable} in a fluid's
 * {@code fluid_property} file): a fluid with a turbine efficiency above 0 is a turbine input. Read on every call, so a
 * datapack reload of the fluid properties applies at once (1.7 turbines read the trait directly, too).
 */
public final class CoolingRecipes {

    public record CoolingRecipe(FluidType input, int inputAmount, FluidType output, int outputAmount, int heatEnergy) {}

    private CoolingRecipes() {}

    /** The cooling step for this input fluid, or null when it can't be cooled (no turbine efficiency). */
    public static CoolingRecipe get(FluidType input) {
        if (input == null) return null;
        FT_Coolable trait = input.getTrait(FT_Coolable.class);
        if (trait == null || trait.getEfficiency(CoolingType.TURBINE) <= 0D) return null;
        return new CoolingRecipe(input, trait.amountReq, trait.coolsTo, trait.amountProduced, trait.heatEnergy);
    }

    /** Read-only view for JEI display, in fluid registration order. */
    public static Map<FluidType, CoolingRecipe> getAll() {
        Map<FluidType, CoolingRecipe> all = new LinkedHashMap<>();
        for (FluidType input : Fluids.getAll()) {
            CoolingRecipe recipe = get(input);
            if (recipe != null) all.put(input, recipe);
        }
        return Collections.unmodifiableMap(all);
    }
}
