package com.hbm.handler.jei;

import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.items.machine.ItemFluidIcon;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category for the ore acidizer (crystallizer), on the universal handler like the 1.12.2
 * {@code CrystallizerRecipeHandler}/{@code JeiRecipes.CrystallizerRecipe}: acid droplet + input item in
 * the universal grid (acid first, matching the old input order) and the old effectiveness stat line.
 */
public class CrystallizerRecipeHandler extends JEIUniversalHandler<CrystallizerRecipes.CrystallizerRecipe> {

    public CrystallizerRecipeHandler(IGuiHelper helper, IRecipeType<CrystallizerRecipes.CrystallizerRecipe> type, Component title, ItemStack... machines) {
        super(helper, type, title, machines);
    }

    @Override
    protected List<List<ItemStack>> inputs(CrystallizerRecipes.CrystallizerRecipe recipe) {
        List<List<ItemStack>> list = new ArrayList<>();
        ItemStack acid = ItemFluidIcon.make(recipe.fluid);
        if (!acid.isEmpty()) list.add(List.of(acid));
        List<ItemStack> item = recipe.input.getDisplayStacks();
        if (!item.isEmpty()) list.add(item);
        return list;
    }

    @Override
    protected List<List<ItemStack>> outputs(CrystallizerRecipes.CrystallizerRecipe recipe) {
        return List.of(List.of(recipe.output));
    }

    @Override
    public void draw(CrystallizerRecipes.CrystallizerRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, slots, g, mouseX, mouseY);
        int productivity = (int) (recipe.productivity * 100);
        if (productivity > 0) {
            text(g, "Effectiveness: +" + Math.min(productivity, 99) + "% per level", 8, 52);
        }
    }
}
