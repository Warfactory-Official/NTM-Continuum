package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.ReformingRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the catalytic reformer — 1 input droplet → 3 output droplets. */
public class ReformingRecipeHandler extends JEIUniversalHandler<ReformingRecipeHandler.Entry> {

    public record Entry(FluidType input, ReformingRecipes.ReformingRecipe recipe) {}

    public ReformingRecipeHandler(IGuiHelper helper, IRecipeType<Entry> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : ReformingRecipes.getAll().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(new FluidStack(recipe.input, 100))));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(
                List.of(ItemFluidIcon.make(recipe.recipe.out1())),
                List.of(ItemFluidIcon.make(recipe.recipe.out2())),
                List.of(ItemFluidIcon.make(recipe.recipe.out3())));
    }
}
