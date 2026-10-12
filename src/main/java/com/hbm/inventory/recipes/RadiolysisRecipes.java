package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.FluidRecipeCodecs;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Radiolysis chamber recipes, 1.7 {@code RadiolysisRecipes}: the chamber's own rows are the datapack table
 * {@code hbm:radiolysis} ({@code data/hbm/recipe/radiolysis/}, same shape as cracking: {@code input}, {@code left},
 * optional {@code right}; HBM ships water into peroxide and hydrogen), followed by <b>a copy of every cracking row</b>.
 * The copy stays a rule in code on purpose: 1.7 adds it so "the numbers and types stay consistent" with the cracker, so a
 * datapack or addon that changes {@code hbm:cracking} changes radiolysis too, and a cracking row of the same input replaces
 * a radiolysis row (1.7 puts them afterwards). The machine consumes 100 mB of input per cycle.
 */
public final class RadiolysisRecipes {

    private RadiolysisRecipes() {}

    /** The two outputs per 100 mB input; {@code right} may be {@code NONE 0} (kerosene). */
    public record RadiolysisRecipe(FluidStack left, FluidStack right) {}

    public static final FluidRecipeCodecs.ByFluid<CrackingRecipes.Row, RadiolysisRecipe> TABLE = new FluidRecipeCodecs.ByFluid<>(
            "radiolysis", () -> CrackingRecipes.CODEC, CrackingRecipes.Row::input, row -> new RadiolysisRecipe(row.left(), row.right()));

    private static volatile Map<FluidType, RadiolysisRecipe> merged;

    static {
        TABLE.changed.register(() -> merged = null);
        CrackingRecipes.TABLE.changed.register(() -> merged = null);
    }

    /** Declares the table at mod construction. */
    public static void declare() {}

    public static RadiolysisRecipe getRadiolysis(FluidType input) {
        return input == null ? null : getAll().get(input);
    }

    public static Map<FluidType, RadiolysisRecipe> getAll() {
        Map<FluidType, RadiolysisRecipe> current = merged;
        if (current != null) return current;
        Map<FluidType, RadiolysisRecipe> next = new LinkedHashMap<>(TABLE.all());
        // 1.7: "automatically add cracking recipes to the radiolysis recipe list"
        for (Map.Entry<FluidType, CrackingRecipes.CrackingRecipe> recipe : CrackingRecipes.getAll().entrySet()) {
            next.put(recipe.getKey(), new RadiolysisRecipe(recipe.getValue().left(), recipe.getValue().right()));
        }
        current = Collections.unmodifiableMap(next);
        merged = current;
        return current;
    }
}
