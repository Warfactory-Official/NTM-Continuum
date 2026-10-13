package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import dev.golem.core.world.RecipeFields;

/**
 * The fusion plasma forge (1.7 {@code PlasmaForgeRecipes}, with its ICF block, {@code ICFRecipes}): the datapack table
 * {@code hbm:plasma_forge}, one file per row in {@code data/hbm/recipe/plasma_forge/}, converted once from the former
 * Java table and the source now. A row adds ntm-next's {@code ignition_temp} ({@link PlasmaForgeRecipe#codec}); 1.7's
 * expensive-mode inputs and 528 pools are its {@code input_items_expensive} and {@code blueprint_pools_528}.
 */
public final class PlasmaForgeRecipes extends DataGenericRecipes<PlasmaForgeRecipe> {

    public static final PlasmaForgeRecipes INSTANCE = new PlasmaForgeRecipes();

    private PlasmaForgeRecipes() {
        super(Constants.MOD_ID, "plasma_forge", PlasmaForgeRecipe::codec, RecipeFields.of()
                .add(RecipeFields.field("ignition_temp", RecipeFields.Kind.LONG).optional().alias("ignitionTemp")));
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}
}
