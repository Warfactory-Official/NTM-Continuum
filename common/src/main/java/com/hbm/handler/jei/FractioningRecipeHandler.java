package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the fractioning tower — input droplet in, two fraction droplets out. */
public class FractioningRecipeHandler extends JEIUniversalHandler<FractioningRecipeHandler.Entry> {

    public record Entry(FluidType input, FractionRecipes.FractionPair pair) {}

    public FractioningRecipeHandler(IGuiHelper helper, IRecipeType<Entry> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : FractionRecipes.getAll().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(recipe.input)));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(recipe.pair.left().type)),
                       List.of(ItemFluidIcon.make(recipe.pair.right().type)));
    }
}
