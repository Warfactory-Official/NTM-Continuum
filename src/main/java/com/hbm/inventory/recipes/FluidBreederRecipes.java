package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;

/**
 * The fusion breeder's fluid-to-fluid irradiation table (1.7 {@code FluidBreederRecipes}): the datapack table
 * {@code hbm:fluid_breeder}, one file per row in {@code data/hbm/recipe/fluid_breeder/}, converted once from the former
 * Java table and the source now.
 */
public final class FluidBreederRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final FluidBreederRecipes INSTANCE = new FluidBreederRecipes();

    private FluidBreederRecipes() {
        super(Constants.MOD_ID, "fluid_breeder");
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

    public GenericRecipe getRecipe(FluidType input) {
        registerRecipes();
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe.inputFluid != null && recipe.inputFluid.length == 1
                    && recipe.inputFluid[0].type == input) return recipe;
        }
        return null;
    }

    public FluidStack getOutput(FluidType input) {
        GenericRecipe recipe = getRecipe(input);
        return recipe == null || recipe.outputFluid == null || recipe.outputFluid.length == 0
                ? null : recipe.outputFluid[0];
    }
}
