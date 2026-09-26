package com.hbm.api.content.recipes;

import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;

/** {@code e.outgasser.add(name, r => r.inputs('#c:ingots/lithium').fluidOutputs('1000x tritium').fusionOnly())} */
public final class OutgasserBuilder extends GenericBuilder {

    private boolean fusionOnly;

    public OutgasserBuilder() {
        duration(OutgasserRecipes.DURATION);
    }

    public OutgasserBuilder fusionOnly() {
        this.fusionOnly = true;
        return this;
    }

    @Override
    protected GenericRecipe create(String name) {
        OutgasserRecipes.OutgasserRecipe recipe = new OutgasserRecipes.OutgasserRecipe(name);
        return fusionOnly ? recipe.fusionOnly() : recipe;
    }
}
