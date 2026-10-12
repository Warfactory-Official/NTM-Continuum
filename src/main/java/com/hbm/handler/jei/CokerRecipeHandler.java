package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the coker — 1 input fluid droplet → {output item, optional byproduct fluid}. */
public class CokerRecipeHandler extends JEIUniversalHandler<CokerRecipeHandler.Entry> {

    public record Entry(FluidType input, CokerRecipes.CokerRecipe recipe) {}

    public CokerRecipeHandler(ViewerType<Entry> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : CokerRecipes.getAll().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(new FluidStack(recipe.input, recipe.recipe.amount()))));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        List<List<ItemStack>> out = new ArrayList<>();
        ItemStack item = recipe.recipe.output();
        if (item != null && !item.isEmpty()) out.add(List.of(item.copy()));
        FluidStack by = recipe.recipe.byproduct();
        if (by != null) out.add(List.of(ItemFluidIcon.make(by)));
        return out;
    }
}
