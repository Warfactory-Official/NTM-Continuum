package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the solidifier — input droplet in, output item out. */
public class SolidificationRecipeHandler extends JEIUniversalHandler<SolidificationRecipeHandler.Entry> {

    public record Entry(FluidType input, SolidificationRecipes.SolidRecipe recipe) {}

    public SolidificationRecipeHandler(ViewerType<Entry> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : SolidificationRecipes.getAll().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(recipe.input)));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(List.of(recipe.recipe.output().copy()));
    }
}
