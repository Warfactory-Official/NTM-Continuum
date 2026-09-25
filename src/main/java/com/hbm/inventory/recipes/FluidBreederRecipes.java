package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;

/** The fusion breeder's fluid-to-fluid irradiation table. */
public final class FluidBreederRecipes extends GenericRecipes {

    public static final FluidBreederRecipes INSTANCE = new FluidBreederRecipes();

    private FluidBreederRecipes() {}

    @Override
    protected void populate() {
        register(new GenericRecipe("fluid_breeder.gas")
                .inputFluids(new FluidStack(Fluids.GAS, 1_000))
                .outputFluids(new FluidStack(Fluids.SYNGAS, 1_000)));
        register(new GenericRecipe("fluid_breeder.lightoil")
                .inputFluids(new FluidStack(Fluids.LIGHTOIL, 1_000))
                .outputFluids(new FluidStack(Fluids.REFORMGAS, 1_000)));
        register(new GenericRecipe("fluid_breeder.lightoil_crack")
                .inputFluids(new FluidStack(Fluids.LIGHTOIL_CRACK, 1_000))
                .outputFluids(new FluidStack(Fluids.REFORMGAS, 1_000)));
    }

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
