package com.hbm.handler.jei;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the heat boiler — input droplet in, heated output droplet out. */
public class BoilingRecipeHandler extends JEIUniversalHandler<BoilingRecipeHandler.Entry> {

    public record Entry(FluidType input, HeatRecipes.HeatStep step) {}

    public BoilingRecipeHandler(ViewerType<Entry> type, Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (var e : HeatRecipes.getAll().entrySet()) list.add(new Entry(e.getKey(), e.getValue()));
        return list;
    }

    @Override
    protected List<List<ItemStack>> inputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(recipe.input)));
    }

    @Override
    protected List<List<ItemStack>> outputs(Entry recipe) {
        return List.of(List.of(ItemFluidIcon.make(recipe.step.output())));
    }
}
