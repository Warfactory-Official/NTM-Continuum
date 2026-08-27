package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.SawmillRecipes.SawmillRecipe;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI view of the sawmill's primary and chance outputs. */
public class SawmillRecipeHandler extends JEIUniversalHandler<SawmillRecipe> {

    public SawmillRecipeHandler(IGuiHelper helper, JeiRecipeType<SawmillRecipe> type,
                                Component title, ItemStack machine) {
        super(helper, type, title, machine);
    }

    public static List<SawmillRecipe> buildRecipes() {
        return SawmillRecipes.getRecipes();
    }

    @Override
    protected List<List<ItemStack>> inputs(SawmillRecipe recipe) {
        return List.of(recipe.input().getDisplayStacks());
    }

    @Override
    protected List<List<ItemStack>> outputs(SawmillRecipe recipe) {
        List<List<ItemStack>> outputs = new ArrayList<>();
        outputs.add(List.of(recipe.output()));
        if (!recipe.secondary().isEmpty()) outputs.add(List.of(recipe.secondary()));
        return outputs;
    }

}
//?}
