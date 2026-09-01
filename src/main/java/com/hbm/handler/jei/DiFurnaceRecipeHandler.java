package com.hbm.handler.jei;

//? if jei {

import com.hbm.blocks.machine.DiFurnaceContent;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Feature-local JEI category for the old alloy-furnace/DiFurnace recipe handler. */
public final class DiFurnaceRecipeHandler extends AbstractRecipeCategory<GenericRecipe> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(
            com.hbm.Constants.MOD_ID, "textures/gui/guidifurnace.png");

    private final IDrawable background;
    private final IDrawableAnimated power;
    private final IDrawableAnimated progress;
    private final IDrawableAnimated fuel;

    public DiFurnaceRecipeHandler(IGuiHelper helper, JeiRecipeType<GenericRecipe> type, Component title) {
        super(type.unwrap(), title,
                helper.createDrawableItemStack(new ItemStack(DiFurnaceContent.MACHINE_DIFURNACE.get())),
                150, 56);
        background = helper.createDrawable(TEXTURE, 6, 16, 150, 56);

        IDrawableStatic powerStatic = helper.createDrawable(TEXTURE, 176, 0, 14, 14);
        power = helper.createAnimatedDrawable(powerStatic, 48, IDrawableAnimated.StartDirection.TOP, true);
        IDrawableStatic progressStatic = helper.createDrawable(TEXTURE, 176, 14, 24, 17);
        progress = helper.createAnimatedDrawable(progressStatic, 48, IDrawableAnimated.StartDirection.LEFT, false);
        IDrawableStatic fuelStatic = helper.createDrawable(TEXTURE, 201, 0, 17, 54);
        fuel = helper.createAnimatedDrawable(fuelStatic, 480, IDrawableAnimated.StartDirection.TOP, true);
    }

    public static List<GenericRecipe> buildRecipes() {
        DiFurnaceRecipes.INSTANCE.registerRecipes();
        return List.copyOf(DiFurnaceRecipes.INSTANCE.recipeOrderedList);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GenericRecipe recipe, IFocusGroup focuses) {
        if (recipe.inputItem == null || recipe.inputItem.length < 2
                || recipe.outputItem == null || recipe.outputItem.length == 0) return;

        builder.addSlot(RecipeIngredientRole.INPUT, 73, 1)
                .addItemStacks(recipe.inputItem[0].getDisplayStacks());
        builder.addSlot(RecipeIngredientRole.INPUT, 73, 37)
                .addItemStacks(recipe.inputItem[1].getDisplayStacks());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 127, 19)
                .addItemStacks(List.of(recipe.outputItem[0]));
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 19)
                .addItemStacks(DiFurnaceRecipes.getAlloyFuels());
    }

    @Override
    public void draw(GenericRecipe recipe, IRecipeSlotsView slots, GuiGraphicsExtractor graphics,
                     double mouseX, double mouseY) {
        background.draw(graphics, 0, 0);
        power.draw(graphics, 56, 21);
        progress.draw(graphics, 95, 19);
        fuel.draw(graphics, 38, 1);
    }
}
//?}
