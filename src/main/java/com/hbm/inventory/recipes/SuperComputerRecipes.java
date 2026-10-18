package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;

/** 1.7 {@code SuperComputerRecipes}: the datapack table {@code hbm:supercomputer}. */
public final class SuperComputerRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final SuperComputerRecipes INSTANCE = new SuperComputerRecipes();

    private SuperComputerRecipes() {
        super(Constants.MOD_ID, "supercomputer");
    }

    public static void declare() {}
}
