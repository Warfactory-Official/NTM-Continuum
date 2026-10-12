package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the catalytic cracker — {input 100, STEAM 200} → {left, right (unless NONE),
 * SPENTSTEAM 2}. */
public class CrackingRecipeHandler extends JEIUniversalHandler<CrackingRecipeHandler.Entry> {

    public record Entry(FluidType input, CrackingRecipes.CrackingRecipe recipe) {}

    public CrackingRecipeHandler(ViewerType<Entry> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : CrackingRecipes.getAll().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(
                List.of(ItemFluidIcon.make(new FluidStack(recipe.input, 100))),
                List.of(ItemFluidIcon.make(new FluidStack(Fluids.STEAM, 200))));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        List<List<ItemStack>> out = new ArrayList<>();
        out.add(List.of(ItemFluidIcon.make(recipe.recipe.left())));
        if (recipe.recipe.right().type != Fluids.NONE) out.add(List.of(ItemFluidIcon.make(recipe.recipe.right())));
        out.add(List.of(ItemFluidIcon.make(new FluidStack(Fluids.SPENTSTEAM, 2))));
        return out;
    }
}
