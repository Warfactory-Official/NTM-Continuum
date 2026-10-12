package com.hbm.handler.jei;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** JEI category for the standard four-output centrifuge. */
public final class CentrifugeRecipeHandler extends JEIUniversalHandler<CentrifugeRecipes.CentrifugeRecipe> {

    public CentrifugeRecipeHandler(ViewerType<CentrifugeRecipes.CentrifugeRecipe> type,
                                   Component title, ItemStack... machines) {
        super(type, title, machines);
    }

    @Override
    protected List<List<ItemStack>> inputs(CentrifugeRecipes.CentrifugeRecipe recipe) {
        List<List<ItemStack>> result = new ArrayList<>();
        AStack input = recipe.input;
        List<ItemStack> stacks = input.getDisplayStacks();
        if (!stacks.isEmpty()) result.add(stacks);
        return result;
    }

    @Override
    protected List<List<ItemStack>> outputs(CentrifugeRecipes.CentrifugeRecipe recipe) {
        List<List<ItemStack>> result = new ArrayList<>();
        for (ItemStack output : recipe.output) {
            if (output != null && !output.isEmpty()) result.add(List.of(output));
        }
        return result;
    }

    @Override
    public void draw(CentrifugeRecipes.CentrifugeRecipe recipe, GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, graphics, mouseX, mouseY);
        text(graphics, "200 ticks", 8, 52);
        text(graphics, "200 HE/t", 101, 52);
    }
}
