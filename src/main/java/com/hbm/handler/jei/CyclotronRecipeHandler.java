package com.hbm.handler.jei;

//? if jei {

import com.hbm.inventory.recipes.CyclotronRecipes;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** The legacy three-slot cyclotron JEI category. */
public final class CyclotronRecipeHandler extends HbmRecipeCategory<CyclotronRecipes.CyclotronRecipe> {
    private final IDrawableAnimated arrow;

    public CyclotronRecipeHandler(IGuiHelper helper, JeiRecipeType<CyclotronRecipes.CyclotronRecipe> type,
                                  Component title, ItemStack machine) {
        super(helper, type, title, machine, "gui_nei_cyclotron.png", 127, 53, 24, 16);
        IDrawableStatic arrowStatic = helper.createDrawable(tex("gui_nei_cyclotron.png"), 100, 118, 24, 18);
        arrow = helper.createAnimatedDrawable(arrowStatic, 48, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CyclotronRecipes.CyclotronRecipe recipe,
                          IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 18)
                .addItemStacks(recipe.particle.getDisplayStacks());
        builder.addSlot(RecipeIngredientRole.INPUT, 55, 18)
                .addItemStacks(recipe.input.getDisplayStacks());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 109, 18)
                .addItemStacks(List.of(recipe.output));
    }

    @Override
    public void draw(CyclotronRecipes.CyclotronRecipe recipe, IRecipeSlotsView slots,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        super.draw(recipe, slots, graphics, mouseX, mouseY);
        arrow.draw(graphics, 25, 18);
    }
}
//?}
