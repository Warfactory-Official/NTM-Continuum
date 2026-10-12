package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;

/** JEI category for the hydrotreater — {input 100, hydrogen} → {out1, out2}. */
public class HydrotreatingRecipeHandler extends JEIUniversalHandler<HydrotreatingRecipeHandler.Entry> {

    public record Entry(FluidType input, HydrotreatingRecipes.HydrotreatingRecipe recipe) {}

    public HydrotreatingRecipeHandler(ViewerType<Entry> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : HydrotreatingRecipes.getAll().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(
                List.of(ItemFluidIcon.make(new FluidStack(recipe.input, 100))),
                List.of(ItemFluidIcon.make(recipe.recipe.hydrogen())));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(
                List.of(ItemFluidIcon.make(recipe.recipe.out1())),
                List.of(ItemFluidIcon.make(recipe.recipe.out2())));
    }
}
