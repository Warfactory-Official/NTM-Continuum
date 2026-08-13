package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.helpers.IGuiHelper;
import dev.golem.core.jei.JeiRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the heat boiler — input droplet in, heated output droplet out. */
public class BoilingRecipeHandler extends JEIUniversalHandler<BoilingRecipeHandler.Entry> {

    public record Entry(FluidType input, HeatRecipes.HeatStep step) {}

    public BoilingRecipeHandler(IGuiHelper helper, JeiRecipeType<Entry> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
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
//?}
