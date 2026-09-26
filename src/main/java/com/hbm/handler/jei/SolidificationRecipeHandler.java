package com.hbm.handler.jei;

//? if jei {

import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.helpers.IGuiHelper;
import dev.golem.core.jei.JeiRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the solidifier — input droplet in, output item out. */
public class SolidificationRecipeHandler extends JEIUniversalHandler<SolidificationRecipeHandler.Entry> {

    public record Entry(FluidType input, SolidificationRecipes.SolidRecipe recipe) {}

    public SolidificationRecipeHandler(IGuiHelper helper, JeiRecipeType<Entry> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
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
//?}
