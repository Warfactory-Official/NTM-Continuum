package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes.AmmoPressRecipe;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category matching the old AmmoPressHandler's compact non-null input layout. */
public class AmmoPressRecipeHandler extends JEIUniversalHandler<AmmoPressRecipe> {

    public AmmoPressRecipeHandler(IGuiHelper helper, JeiRecipeType<AmmoPressRecipe> type,
                                  Component title, ItemStack machine) {
        super(helper, type, title, machine);
    }

    @Override
    protected List<List<ItemStack>> inputs(AmmoPressRecipe recipe) {
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (AStack input : recipe.input) {
            if (input != null) inputs.add(input.getDisplayStacks());
        }
        return inputs;
    }

    @Override
    protected List<List<ItemStack>> outputs(AmmoPressRecipe recipe) {
        return List.of(List.of(recipe.output.copy()));
    }
}
//?}
