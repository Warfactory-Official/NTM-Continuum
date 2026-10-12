package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import net.minecraft.world.item.ItemStack;

/**
 * The blast furnace's recipes (1.7 {@code BlastFurnaceRecipesNT}, plus the meteor rows of
 * {@code AbilityToolMachineRecipes}): the datapack table {@code hbm:blast_furnace}, one file per row in
 * {@code data/hbm/recipe/blast_furnace/}, converted once from the former Java table and the source now.
 */
public final class BlastFurnaceRecipesNT extends DataGenericRecipes<GenericRecipe> {

    public static final BlastFurnaceRecipesNT INSTANCE = new BlastFurnaceRecipesNT();

    private BlastFurnaceRecipesNT() {
        super(Constants.MOD_ID, "blast_furnace");
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

    /** Matches either furnace ingredient order, as the old two-slot machine did. */
    public GenericRecipe getRecipe(ItemStack first, ItemStack second) {
        registerRecipes();
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe.inputItem == null || recipe.inputItem.length != 2) continue;
            if (recipe.inputItem[0].matchesRecipe(first, false)
                    && recipe.inputItem[1].matchesRecipe(second, false)) return recipe;
            if (recipe.inputItem[1].matchesRecipe(first, false)
                    && recipe.inputItem[0].matchesRecipe(second, false)) return recipe;
        }
        return null;
    }
}
