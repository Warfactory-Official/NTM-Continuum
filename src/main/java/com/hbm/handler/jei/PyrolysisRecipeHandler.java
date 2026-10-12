package com.hbm.handler.jei;

import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the pyro oven — optional {fluid, item} in → optional {item, fluid} out. */
public class PyrolysisRecipeHandler extends JEIUniversalHandler<PyrolysisRecipeHandler.Entry> {

    public record Entry(PyroOvenRecipe recipe) {}

    public PyrolysisRecipeHandler(ViewerType<Entry> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (PyroOvenRecipe rec : PyroOvenRecipes.getAll()) list.add(new Entry(rec));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        List<List<ItemStack>> in = new ArrayList<>();
        if (recipe.recipe.inputFluid != null) in.add(List.of(ItemFluidIcon.make(recipe.recipe.inputFluid)));
        if (recipe.recipe.inputItem != null) in.add(recipe.recipe.inputItem.getDisplayStacks());
        return in;
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        List<List<ItemStack>> out = new ArrayList<>();
        if (recipe.recipe.outputItem != null) out.add(List.of(recipe.recipe.outputItem.copy()));
        if (recipe.recipe.outputFluid != null) out.add(List.of(ItemFluidIcon.make(recipe.recipe.outputFluid)));
        return out;
    }
}
