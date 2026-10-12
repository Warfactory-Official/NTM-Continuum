package com.hbm.recipes.tables;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CompressorRecipes;
import com.hbm.inventory.recipes.CompressorRecipes.CompressorRecipe;
import com.hbm.inventory.recipes.CompressorRecipes.Key;
import com.hbm.inventory.recipes.CompressorRecipes.Row;
import dev.golem.core.world.RecipeSource;

/** 1.7 {@code CompressorRecipes.registerDefaults}. */
public final class CompressorTable extends RecipeSource<Row> {

    public CompressorTable() {
        super(CompressorRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        put(new Key(Fluids.PETROLEUM, 0), new CompressorRecipe(2_000, new FluidStack(Fluids.PETROLEUM, 2_000, 1), 20));
        put(new Key(Fluids.PETROLEUM, 1), new CompressorRecipe(2_000, new FluidStack(Fluids.LPG, 1_000, 0), 20));

        put(new Key(Fluids.BLOOD, 3), new CompressorRecipe(1_000, new FluidStack(Fluids.HEAVYOIL, 250, 0), 200));

        put(new Key(Fluids.PERFLUOROMETHYL, 0), new CompressorRecipe(1_000, new FluidStack(Fluids.PERFLUOROMETHYL, 1_000, 1), 50));
        put(new Key(Fluids.PERFLUOROMETHYL, 1), new CompressorRecipe(1_000, new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 1_000, 0), 50));
    }

    /** 1.7 {@code recipes.put(new Pair(fluid, pressure), recipe)}; the id is {@code <fluid>_<pressure>}. */
    private void put(Key key, CompressorRecipe recipe) {
        register(key.type().getKey() + "_" + key.pressure(),
                new Row(key.type(), key.pressure(), recipe.inputAmount(), recipe.output(), recipe.duration()));
    }
}
