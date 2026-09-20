package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Radiolysis chamber recipes — 1:1 port of the 1.7 {@code RadiolysisRecipes}: water into peroxide and
 * hydrogen, plus every cracking recipe (the numbers and types are deliberately shared with the cracker).
 * The machine consumes 100 mB of input per cycle.
 */
public final class RadiolysisRecipes {

    private RadiolysisRecipes() {}

    /** The two outputs per 100 mB input; {@code right} may be {@code NONE 0} (kerosene). */
    public record RadiolysisRecipe(FluidStack left, FluidStack right) {}

    private static final Map<FluidType, RadiolysisRecipe> radiolysis = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static RadiolysisRecipe getRadiolysis(FluidType input) {
        init();
        return radiolysis.get(input);
    }

    public static Map<FluidType, RadiolysisRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(radiolysis);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        radiolysis.put(Fluids.WATER, new RadiolysisRecipe(new FluidStack(Fluids.PEROXIDE, 80), new FluidStack(Fluids.HYDROGEN, 20)));

        // automatically add cracking recipes to the radiolysis recipe list
        // we want the numbers and types to stay consistent anyway and this will save us a lot of headache later on
        Map<FluidType, CrackingRecipes.CrackingRecipe> cracking = CrackingRecipes.getAll();
        if (cracking.isEmpty()) {
            throw new IllegalStateException("CrackingRecipes.getAll has yielded an empty map while registering the radiolysis recipes! Either the load order is broken or cracking recipes have been removed!");
        }
        for (Map.Entry<FluidType, CrackingRecipes.CrackingRecipe> recipe : cracking.entrySet()) {
            radiolysis.put(recipe.getKey(), new RadiolysisRecipe(recipe.getValue().left(), recipe.getValue().right()));
        }
    }
}
