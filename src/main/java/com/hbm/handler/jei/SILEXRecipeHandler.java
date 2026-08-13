package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.recipes.SILEXRecipes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import dev.golem.core.jei.JeiRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category for the SILEX, on the universal handler.
 *
 * <p>Upstream split this into <b>six</b> categories — a general one plus one per wavelength — so that
 * clicking a laser crystal showed exactly the recipes that crystal unlocks. That split existed because
 * 1.12.2 JEI had no way to filter within a category. Here it is a single category and each recipe
 * states its own requirement on the stat lines, which is both simpler and more honest: a recipe listed
 * under "IR" also runs on every stronger wavelength.
 *
 * <p>Stat lines mirror the old {@code JeiRecipes} wrapper: the required wavelength, and the
 * multiplier {@code fluidProduced / fluidConsumed} — how many output rolls one input item yields.
 * Per-output percentages come from the weights.
 */
public class SILEXRecipeHandler extends JEIUniversalHandler<SILEXRecipes.SILEXRecipe> {

    public SILEXRecipeHandler(IGuiHelper helper, JeiRecipeType<SILEXRecipes.SILEXRecipe> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    @Override
    protected List<List<ItemStack>> inputs(SILEXRecipes.SILEXRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        List<ItemStack> input = recipe.input.getDisplayStacks();
        if (!input.isEmpty()) list.add(input);
        return list;
    }

    @Override
    protected List<List<ItemStack>> outputs(SILEXRecipes.SILEXRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        for (SILEXRecipes.WeightedOutput out : recipe.outputs) {
            list.add(List.of(out.stack()));
        }
        return list;
    }

    @Override
    public void draw(SILEXRecipes.SILEXRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        text(g, Component.translatable(recipe.laserStrength.name).getString() + " laser or stronger", 8, 44);
        int rolls = recipe.fluidConsumed > 0 ? recipe.fluidProduced / recipe.fluidConsumed : 0;
        if (rolls > 0) text(g, rolls + "x output per input", 8, 54);

        int total = recipe.totalWeight();
        if (total <= 0) return;
        StringBuilder chances = new StringBuilder();
        for (SILEXRecipes.WeightedOutput out : recipe.outputs) {
            if (!chances.isEmpty()) chances.append(", ");
            chances.append(Math.round(out.weight() * 100F / total)).append('%');
        }
        text(g, chances.toString(), 8, 64);
    }
}
//?}
