package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import net.minecraft.world.item.ItemStack;

/**
 * PUREX (1.7 {@code PUREXRecipes}): the datapack table {@code hbm:purex}, one file per row in
 * {@code data/hbm/recipe/purex/}, converted once from the former Java table and the source now. In 1.7's order: the
 * uranium-zirconium-hydride billet line, balefire gold / flashlead, the three CP-1 pile rods, the nine ZIRNOX and seven
 * plate-fuel reprocessing rows, the fifteen PWR ones, thorium molten-salt regeneration, the Watz pellets, ICF pellet
 * recovery, waste vitrification and the schrabidium extractions. ZIRNOX and plate spent fuel must first be cooled in a
 * waste drum ({@code hbm:cold} ingredients); PWR spent rods match their exact item data ({@code "exact": true}).
 *
 * <p>The two naquadah Watz rows are ordinary rows that need {@code #c:nuggets/naquadria} to have an item, as 1.7 registered
 * them only when another mod supplied {@code nuggetNaquadria}; the NQD row's first output is that tag's first item.
 */
public final class PUREXRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final PUREXRecipes INSTANCE = new PUREXRecipes();

    private PUREXRecipes() {
        super(Constants.MOD_ID, "purex");
    }

    /** Declares the table at mod construction (its recipe type and serializer register with HBM's content). */
    public static void declare() {}

    public boolean matchesInput(ItemStack stack) {
        registerRecipes();
        return recipeOrderedList.stream().anyMatch(recipe -> recipe.inputItem != null
                && recipe.inputItem.length > 0 && recipe.inputItem[0].matchesRecipe(stack, true));
    }
}
