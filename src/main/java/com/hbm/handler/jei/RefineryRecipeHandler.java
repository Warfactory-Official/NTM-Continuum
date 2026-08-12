package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category for the refinery — universal grid: the hot-oil input droplet in, the 4 fraction droplets
 * plus the byproduct item out (matching the old {@code RefineryRecipeHandler} display).
 */
public class RefineryRecipeHandler extends JEIUniversalHandler<RefineryRecipeHandler.Entry> {

    /** One refinery recipe keyed on its input fluid. */
    public record Entry(FluidType input, RefineryRecipes.RefineryRecipe recipe) {}

    public RefineryRecipeHandler(IGuiHelper helper, IRecipeType<Entry> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : RefineryRecipes.getAll().entrySet()) {
            list.add(new Entry(e.getKey(), e.getValue()));
        }
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(recipe.input)));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        List<List<ItemStack>> out = new ArrayList<>();
        for (var frac : recipe.recipe.fractions()) {
            out.add(List.of(ItemFluidIcon.make(frac.type)));
        }
        ItemStack byproduct = recipe.recipe.byproduct();
        if (byproduct != null && !byproduct.isEmpty()) out.add(List.of(byproduct.copy()));
        return out;
    }
}
