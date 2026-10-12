package com.hbm.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericTableSource;

/** 1.7 {@code FluidBreederRecipes.registerDefaults}. 1.7's rows have no names; the port names them {@code fluid_breeder.<input>}. */
public final class FluidBreederTable extends GenericTableSource<GenericRecipe> {

    public FluidBreederTable() {
        super(FluidBreederRecipes.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        register(new FluidStack(Fluids.GAS, 1_000), new FluidStack(Fluids.SYNGAS, 1_000));
        register(new FluidStack(Fluids.LIGHTOIL, 1_000), new FluidStack(Fluids.REFORMGAS, 1_000));
        register(new FluidStack(Fluids.LIGHTOIL_CRACK, 1_000), new FluidStack(Fluids.REFORMGAS, 1_000));
    }

    /** 1.7 {@code register(input, output)}. */
    private void register(FluidStack input, FluidStack output) {
        register(new GenericRecipe("fluid_breeder." + input.type.getKey()).inputFluids(input).outputFluids(output));
    }
}
