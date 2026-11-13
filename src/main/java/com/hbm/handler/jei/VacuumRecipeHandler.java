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

/** JEI category for the vacuum distiller; 1.7 shows each row per 1,000 mB of input at 2 PU, ten times the per-operation table. */
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
        return List.of(List.of(ItemFluidIcon.make(recipe.input, 1000, 2)));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        List<List<ItemStack>> out = new ArrayList<>();
        for (FluidStack frac : recipe.recipe.fractions()) out.add(List.of(ItemFluidIcon.make(frac, 10)));
        return out;
    }
}
