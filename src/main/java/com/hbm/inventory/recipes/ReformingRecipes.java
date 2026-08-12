package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalytic reformer recipes — 1:1 port of the 1.12.2 {@code ReformingRecipes}. Per 100 mB input the
 * three fluid outputs (in mB). Consumed by the catalytic reformer (which additionally requires a
 * {@code catalytic_converter} in its catalyst slot and 20k HE + 100 mB per op).
 */
public final class ReformingRecipes {

    private ReformingRecipes() {}

    /** Three fluid outputs per 100 mB input. */
    public record ReformingRecipe(FluidStack out1, FluidStack out2, FluidStack out3) {}

    private static final Map<FluidType, ReformingRecipe> recipes = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static ReformingRecipe getOutput(FluidType type) {
        init();
        return recipes.get(type);
    }

    public static Map<FluidType, ReformingRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(recipes);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        recipes.put(Fluids.HEATINGOIL, new ReformingRecipe(
                new FluidStack(Fluids.NAPHTHA, 50), new FluidStack(Fluids.PETROLEUM, 15), new FluidStack(Fluids.HYDROGEN, 10)));
        recipes.put(Fluids.NAPHTHA, new ReformingRecipe(
                new FluidStack(Fluids.REFORMATE, 50), new FluidStack(Fluids.PETROLEUM, 15), new FluidStack(Fluids.HYDROGEN, 10)));
        recipes.put(Fluids.NAPHTHA_CRACK, new ReformingRecipe(
                new FluidStack(Fluids.REFORMATE, 50), new FluidStack(Fluids.AROMATICS, 10), new FluidStack(Fluids.HYDROGEN, 5)));
        recipes.put(Fluids.NAPHTHA_COKER, new ReformingRecipe(
                new FluidStack(Fluids.REFORMATE, 50), new FluidStack(Fluids.REFORMGAS, 10), new FluidStack(Fluids.HYDROGEN, 5)));
        recipes.put(Fluids.LIGHTOIL, new ReformingRecipe(
                new FluidStack(Fluids.AROMATICS, 50), new FluidStack(Fluids.REFORMGAS, 10), new FluidStack(Fluids.HYDROGEN, 15)));
        recipes.put(Fluids.LIGHTOIL_CRACK, new ReformingRecipe(
                new FluidStack(Fluids.AROMATICS, 50), new FluidStack(Fluids.REFORMGAS, 5), new FluidStack(Fluids.HYDROGEN, 20)));
        recipes.put(Fluids.PETROLEUM, new ReformingRecipe(
                new FluidStack(Fluids.UNSATURATEDS, 85), new FluidStack(Fluids.REFORMGAS, 10), new FluidStack(Fluids.HYDROGEN, 5)));
        recipes.put(Fluids.SOURGAS, new ReformingRecipe(
                new FluidStack(Fluids.SULFURIC_ACID, 75), new FluidStack(Fluids.PETROLEUM, 10), new FluidStack(Fluids.HYDROGEN, 15)));
        recipes.put(Fluids.CHOLESTEROL, new ReformingRecipe(
                new FluidStack(Fluids.ESTRADIOL, 50), new FluidStack(Fluids.REFORMGAS, 35), new FluidStack(Fluids.HYDROGEN, 15)));
    }
}
