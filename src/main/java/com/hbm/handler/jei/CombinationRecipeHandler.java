package com.hbm.handler.jei;

import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CombinationRecipes.CombinationRecipe;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 1.7 {@code CombinationHandler}: one input, then the item output and the fluid output as a fluid icon. */
public class CombinationRecipeHandler extends JEIUniversalHandler<CombinationRecipe> {

    public CombinationRecipeHandler(ViewerType<CombinationRecipe> type, Component title, ItemStack machine) {
        super(type, title, machine);
    }

    public static List<CombinationRecipe> buildRecipes() {
        return CombinationRecipes.getRecipes();
    }

    @Override
    protected List<List<ItemStack>> inputs(CombinationRecipe recipe) {
        return List.of(recipe.input().getDisplayStacks());
    }

    @Override
    protected List<List<ItemStack>> outputs(CombinationRecipe recipe) {
        List<List<ItemStack>> out = new ArrayList<>();
        if (recipe.itemOutput() != null) out.add(List.of(recipe.itemOutput().copy()));
        if (recipe.fluidOutput() != null) out.add(List.of(ItemFluidIcon.make(recipe.fluidOutput())));
        return out;
    }
}
