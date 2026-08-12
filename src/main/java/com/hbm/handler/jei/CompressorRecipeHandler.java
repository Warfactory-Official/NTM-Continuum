package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.CompressorRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category for the compressor — the fluids that <b>change type</b> when compressed. The generic
 * "same type at pressure+1" path (which is what pressurises OIL for the vacuum refinery and HYDROGEN
 * for the hydrotreater) has no recipe object, so — like the old {@code CompressorRecipes.getRecipes}
 * — it is not listed here; the old skips entries whose input and output icon+pressure match.
 */
public class CompressorRecipeHandler extends JEIUniversalHandler<CompressorRecipeHandler.Entry> {

    public record Entry(CompressorRecipes.Key key, CompressorRecipes.CompressorRecipe recipe) {}

    public CompressorRecipeHandler(IGuiHelper helper, IRecipeType<Entry> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : CompressorRecipes.getAll().entrySet()) {
            // old: skip when the icon AND pressure are unchanged (nothing to show)
            CompressorRecipes.Key k = e.getKey();
            FluidStack out = e.getValue().output();
            if (k.type() == out.type && k.pressure() == out.pressure) continue;
            list.add(new Entry(k, e.getValue()));
        }
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(
                new FluidStack(recipe.key.type(), recipe.recipe.inputAmount(), recipe.key.pressure()))));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(recipe.recipe.output())));
    }
}
