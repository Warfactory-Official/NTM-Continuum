package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.HydrotreatingRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.ArrayList;

/** JEI category for the hydrotreater; 1.7 shows each row per 1,000 mB of input, ten times the per-operation table. */
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
                List.of(ItemFluidIcon.make(recipe.input, 1000)),
                List.of(ItemFluidIcon.make(recipe.recipe.hydrogen(), 10)));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(
                List.of(ItemFluidIcon.make(recipe.recipe.out1(), 10)),
                List.of(ItemFluidIcon.make(recipe.recipe.out2(), 10)));
    }
}
