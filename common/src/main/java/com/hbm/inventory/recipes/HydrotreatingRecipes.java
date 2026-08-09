package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hydrotreater recipes — 1:1 port of the 1.12.2 {@code HydrotreatingRecipes}. Per 100 mB input +
 * {@code hydrogen} mB (at pressure 1) → the two outputs {@code out1}/{@code out2} (in mB). The machine
 * additionally requires a {@code catalytic_converter} and 20k HE per op (every 2 ticks).
 */
public final class HydrotreatingRecipes {

    private HydrotreatingRecipes() {}

    /** Hydrogen requirement (pressurized) + two outputs, per 100 mB input. */
    public record HydrotreatingRecipe(FluidStack hydrogen, FluidStack out1, FluidStack out2) {}

    private static final Map<FluidType, HydrotreatingRecipe> recipes = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static HydrotreatingRecipe getOutput(FluidType type) {
        init();
        return recipes.get(type);
    }

    public static Map<FluidType, HydrotreatingRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(recipes);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        recipes.put(Fluids.OIL, new HydrotreatingRecipe(
                new FluidStack(Fluids.HYDROGEN, 5, 1), new FluidStack(Fluids.OIL_DS, 90), new FluidStack(Fluids.SOURGAS, 15)));
        recipes.put(Fluids.CRACKOIL, new HydrotreatingRecipe(
                new FluidStack(Fluids.HYDROGEN, 5, 1), new FluidStack(Fluids.CRACKOIL_DS, 90), new FluidStack(Fluids.SOURGAS, 15)));
        recipes.put(Fluids.GAS, new HydrotreatingRecipe(
                new FluidStack(Fluids.HYDROGEN, 5, 1), new FluidStack(Fluids.PETROLEUM, 80), new FluidStack(Fluids.SOURGAS, 15)));
        recipes.put(Fluids.DIESEL_CRACK, new HydrotreatingRecipe(
                new FluidStack(Fluids.HYDROGEN, 10, 1), new FluidStack(Fluids.DIESEL, 80), new FluidStack(Fluids.SOURGAS, 30)));
        recipes.put(Fluids.DIESEL_CRACK_REFORM, new HydrotreatingRecipe(
                new FluidStack(Fluids.HYDROGEN, 10, 1), new FluidStack(Fluids.DIESEL_REFORM, 80), new FluidStack(Fluids.SOURGAS, 30)));
        recipes.put(Fluids.COALOIL, new HydrotreatingRecipe(
                new FluidStack(Fluids.HYDROGEN, 10, 1), new FluidStack(Fluids.COALGAS, 80), new FluidStack(Fluids.SOURGAS, 15)));
    }
}
