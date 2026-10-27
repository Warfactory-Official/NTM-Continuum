package com.hbm.handler.jei;

import com.hbm.inventory.recipes.SawmillRecipes;
import com.hbm.inventory.recipes.SawmillRecipes.SawmillRecipe;
import com.hbm.items.ModItems;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI view of the sawmill's primary and chance outputs. */
public class SawmillRecipeHandler extends JEIUniversalHandler<SawmillRecipe> {

    public SawmillRecipeHandler(ViewerType<SawmillRecipe> type,
                                Component title, ItemStack machine) {
        super(type, title, machine);
    }

    public static List<SawmillRecipe> buildRecipes() {
        return SawmillRecipes.getRecipes();
    }

    @Override
    protected List<List<ItemStack>> inputs(SawmillRecipe recipe) {
        return List.of(recipe.input());
    }

    @Override
    protected List<List<ItemStack>> outputs(SawmillRecipe recipe) {
        var outputs = new ArrayList<List<ItemStack>>();
        outputs.add(List.of(recipe.output()));
        if (recipe.sawdustChance() > 0F) outputs.add(List.of(new ItemStack(ModItems.powder_sawdust.get())));
        return outputs;
    }

    @Override
    protected float outputChance(SawmillRecipe recipe, int slot) {
        return slot == 1 ? recipe.sawdustChance() : 1F;
    }
}
