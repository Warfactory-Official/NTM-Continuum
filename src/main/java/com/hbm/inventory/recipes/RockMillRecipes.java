package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;

/** 1.7 {@code RockMillRecipes}: the datapack table {@code hbm:rock_mill}. */
public final class RockMillRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final RockMillRecipes INSTANCE = new RockMillRecipes();

    private RockMillRecipes() {
        super(Constants.MOD_ID, "rock_mill");
    }

    public static void declare() {}
}
