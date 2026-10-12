package com.hbm.handler.jei;

import com.hbm.inventory.recipes.FuelPoolRecipes;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 1.7 {@code FuelPoolHandler}: the spent fuel pool drum's cooling table ({@link FuelPoolRecipes}) as an
 * {@code NEIUniversalHandler} page - one hot stack in, its cooled form out, the drum in the machine pill. Title 1.7's
 * "Spent Fuel Pool Drum", the block's own name.
 */
public final class FuelPoolRecipeHandler extends JEIUniversalHandler<FuelPoolRecipes.Entry> {

    public FuelPoolRecipeHandler(ViewerType<FuelPoolRecipes.Entry> type, Component title,
                                 ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<FuelPoolRecipes.Entry> buildRecipes() {
        return FuelPoolRecipes.displayRecipes();
    }

    @Override
    protected List<List<ItemStack>> inputs(FuelPoolRecipes.Entry recipe) {
        return List.of(List.of(recipe.input()));
    }

    @Override
    protected List<List<ItemStack>> outputs(FuelPoolRecipes.Entry recipe) {
        return List.of(List.of(recipe.output()));
    }
}
