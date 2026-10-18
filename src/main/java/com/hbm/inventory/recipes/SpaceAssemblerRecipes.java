package com.hbm.inventory.recipes;

import com.hbm.Constants;
import com.hbm.inventory.recipes.loader.DataGenericRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code SpaceAssemblerRecipes}, the space laboratory's 0G assemblers: the datapack table {@code hbm:space_assembler}. */
public final class SpaceAssemblerRecipes extends DataGenericRecipes<GenericRecipe> {

    public static final SpaceAssemblerRecipes INSTANCE = new SpaceAssemblerRecipes();

    private SpaceAssemblerRecipes() {
        super(Constants.MOD_ID, "space_assembler");
    }

    public static void declare() {}

    /** The row whose single input is {@code stack}, ignoring its count. */
    public @Nullable GenericRecipe getRecipe(ItemStack stack) {
        registerRecipes();
        for (GenericRecipe recipe : recipeOrderedList) {
            if (recipe.inputItem != null && recipe.inputItem.length == 1 && recipe.inputItem[0].matchesRecipe(stack, true)) return recipe;
        }
        return null;
    }

    public @Nullable GenericRecipe byName(String name) {
        registerRecipes();
        return recipeNameMap.get(name);
    }
}
