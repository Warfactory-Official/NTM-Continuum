package com.hbm.handler.jei;

import com.hbm.Constants;
import dev.golem.core.Ids;
import dev.golem.core.viewer.Drawable;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import dev.golem.core.viewer.ViewerCategory;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import com.hbm.blocks.machine.ModMachineBlocks;

/** Feature-local recipe-viewer (JEI and REI) category for the old alloy-furnace/DiFurnace recipe handler. */
public final class DiFurnaceRecipeHandler extends ViewerCategory<GenericRecipe> {

    private static final Identifier TEXTURE = Ids.of(
            Constants.MOD_ID, "textures/gui/guidifurnace.png");

    private final Drawable background;
    private final Drawable power;
    private final Drawable progress;
    private final Drawable fuel;

    public DiFurnaceRecipeHandler(ViewerType<GenericRecipe> type, Component title) {
        super(type, title, new ItemStack(ModMachineBlocks.machine_difurnace_off.get()), 150, 56);
        background = Drawable.texture(TEXTURE, 6, 16, 150, 56);

        Drawable.Texture powerStatic = Drawable.texture(TEXTURE, 176, 0, 14, 14);
        power = Drawable.animated(powerStatic, 48, Drawable.Direction.TOP, true);
        Drawable.Texture progressStatic = Drawable.texture(TEXTURE, 176, 14, 24, 17);
        progress = Drawable.animated(progressStatic, 48, Drawable.Direction.LEFT, false);
        Drawable.Texture fuelStatic = Drawable.texture(TEXTURE, 201, 0, 17, 54);
        fuel = Drawable.animated(fuelStatic, 480, Drawable.Direction.TOP, true);
    }

    public static List<GenericRecipe> buildRecipes() {
        DiFurnaceRecipes.INSTANCE.registerRecipes();
        return DiFurnaceRecipes.INSTANCE.recipeOrderedList.stream()
                .filter(r -> !DiFurnaceRecipes.HIDDEN_METEOR_SWORD.equals(r.getInternalName())).toList();
    }

    @Override
    public void setRecipe(SlotLayout builder, GenericRecipe recipe) {
        if (recipe.inputItem == null || recipe.inputItem.length < 2
                || recipe.outputItem == null || recipe.outputItem.length == 0) return;

        builder.addSlot(Role.INPUT, 73, 1)
                .addItemStacks(recipe.inputItem[0].getDisplayStacks());
        builder.addSlot(Role.INPUT, 73, 37)
                .addItemStacks(recipe.inputItem[1].getDisplayStacks());
        builder.addSlot(Role.OUTPUT, 127, 19)
                .addItemStacks(List.of(recipe.outputItem[0]));
        builder.addSlot(Role.INPUT, 1, 19)
                .addItemStacks(DiFurnaceRecipes.getAlloyFuels());
    }

    @Override
    public void draw(GenericRecipe recipe, GuiGraphicsExtractor graphics,
                     double mouseX, double mouseY) {
        background.draw(graphics, 0, 0);
        power.draw(graphics, 56, 21);
        progress.draw(graphics, 95, 19);
        fuel.draw(graphics, 38, 1);
    }
}
