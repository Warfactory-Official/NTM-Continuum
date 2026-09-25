package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compressor recipes — 1:1 port of the 1.12.2 {@code CompressorRecipes}. Keyed on the <b>input fluid +
 * its pressure</b> ({@link Key}), because the compressor's input pressure is user-selectable.
 *
 * <p>Most fluids have <b>no</b> recipe: the machine then takes the generic path — 1000 mB in → 1000 mB
 * out of the <i>same</i> type at {@code pressure + 1}. That generic path is what pressurises OIL for the
 * vacuum refinery (0→1→2, i.e. two compressor stages) and HYDROGEN for the hydrotreater (0→1). The
 * explicit entries below are the fluids that <i>change type</i> when compressed.
 */
public final class CompressorRecipes {

    private CompressorRecipes() {}

    /** Input fluid + the pressure it's compressed FROM. */
    public record Key(FluidType type, int pressure) {}

    public record CompressorRecipe(int inputAmount, FluidStack output, int duration) {}

    private static final Map<Key, CompressorRecipe> recipes = new LinkedHashMap<>();
    private static boolean initialized = false;

    public static CompressorRecipe getRecipe(FluidType type, int pressure) {
        init();
        return recipes.get(new Key(type, pressure));
    }

    public static Map<Key, CompressorRecipe> getAll() {
        init();
        return Collections.unmodifiableMap(recipes);
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        recipes.put(new Key(Fluids.PETROLEUM, 0), new CompressorRecipe(2_000, new FluidStack(Fluids.PETROLEUM, 2_000, 1), 20));
        recipes.put(new Key(Fluids.PETROLEUM, 1), new CompressorRecipe(2_000, new FluidStack(Fluids.LPG, 1_000, 0), 20));

        recipes.put(new Key(Fluids.BLOOD, 3), new CompressorRecipe(1_000, new FluidStack(Fluids.HEAVYOIL, 250, 0), 200));

        recipes.put(new Key(Fluids.PERFLUOROMETHYL, 0), new CompressorRecipe(1_000, new FluidStack(Fluids.PERFLUOROMETHYL, 1_000, 1), 50));
        recipes.put(new Key(Fluids.PERFLUOROMETHYL, 1), new CompressorRecipe(1_000, new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 1_000, 0), 50));
        com.hbm.scripting.ScriptedRecipes.apply(CompressorRecipes.class);
    }

    public static void put(FluidType type, int pressure, CompressorRecipe recipe) {
        init();
        recipes.put(new Key(type, pressure), recipe);
    }

    public static boolean remove(FluidType type, int pressure) {
        init();
        return recipes.remove(new Key(type, pressure)) != null;
    }

    public static synchronized void reset() {
        recipes.clear();
        initialized = false;
    }
}
