package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import dev.golem.core.jei.JeiRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * JEI category for the soldering station, on the universal handler like the 1.12.2
 * {@code SolderingStationRecipeHandler}/{@code JeiRecipes.SolderingRecipe}: toppings + PCB + solder
 * ingredients (plus the optional fluid as a droplet) in the universal grid, and the duration/consumption
 * stat lines drawn right-aligned at the old positions.
 */
public class SolderingStationRecipeHandler extends JEIUniversalHandler<SolderingRecipes.SolderingRecipe> {

    public SolderingStationRecipeHandler(IGuiHelper helper, JeiRecipeType<SolderingRecipes.SolderingRecipe> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    @Override
    protected List<List<ItemStack>> inputs(SolderingRecipes.SolderingRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        addAll(list, recipe.toppings);
        addAll(list, recipe.pcb);
        addAll(list, recipe.solder);
        if (recipe.fluid != null) {
            ItemStack icon = ItemFluidIcon.make(recipe.fluid);
            if (!icon.isEmpty()) list.add(List.of(icon));
        }
        return list;
    }

    @Override
    protected List<List<ItemStack>> outputs(SolderingRecipes.SolderingRecipe recipe) {
        return List.of(List.of(recipe.output));
    }

    private static void addAll(List<List<ItemStack>> inputs, AStack[] stacks) {
        if (stacks == null) return;
        for (AStack a : stacks) {
            List<ItemStack> vars = a.getDisplayStacks();
            if (!vars.isEmpty()) inputs.add(vars);
        }
    }

    @Override
    public void draw(SolderingRecipes.SolderingRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        String duration = String.format(Locale.US, "%,d", recipe.duration) + " ticks";
        String consumption = String.format(Locale.US, "%,d", recipe.consumption) + " HE/t";
        text(g, duration, 160 - textWidth(duration), 43);
        text(g, consumption, 160 - textWidth(consumption), 55);
    }
}
//?}
