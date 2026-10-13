package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the vacuum distiller: 1 hot-oil input droplet to the 4 vacuum fraction droplets. */
public class VacuumRecipeHandler extends JEIUniversalHandler<VacuumRecipeHandler.Entry> {

    public record Entry(FluidType input, RefineryRecipes.VacuumRecipe recipe) {}

    public VacuumRecipeHandler(ViewerType<Entry> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : RefineryRecipes.getAllVacuum().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(new FluidStack(recipe.input, 100))));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        List<List<ItemStack>> out = new ArrayList<>();
        for (FluidStack frac : recipe.recipe.fractions()) out.add(List.of(ItemFluidIcon.make(frac)));
        return out;
    }
}
