package com.hbm.handler.jei;

import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI category for anvil smithing, a port of the 1.12.2 {@code AnvilSmithingRecipeHandler}:
 * its own {@code gui_nei_smithing.png} background, left input at {@code (39,24)}, right input at
 * {@code (75,24)}, output at {@code (111,24)} (each with JEI's standard slot frame, as the original used
 * {@code getSlotDrawable()}), the "Tier N" line drawn at {@code (52,43)} and repeated dark-gray in the
 * output tooltip.
 */
public class AnvilSmithingRecipeHandler extends HbmRecipeCategory<AnvilSmithingRecipe> {

    public AnvilSmithingRecipeHandler(ViewerType<AnvilSmithingRecipe> type, Component title, ItemStack icon) {
        super(type, title, icon, "gui_nei_smithing.png");
    }

    public static List<AnvilSmithingRecipe> buildRecipes() {
        AnvilRecipes.register();
        return AnvilRecipes.getSmithing();
    }

    @Override
    public void setRecipe(SlotLayout builder, AnvilSmithingRecipe recipe) {
        builder.addSlot(Role.INPUT, 39, 24)   // old init 38,23
                .addItemStacks(recipe.getLeft()).setStandardSlotBackground();
        builder.addSlot(Role.INPUT, 75, 24)   // old init 74,23
                .addItemStacks(recipe.getRight()).setStandardSlotBackground();
        builder.addSlot(Role.OUTPUT, 111, 24) // old init 110,23
                .addItemStacks(List.of(recipe.getSimpleOutput())).setStandardSlotBackground()
                .addTooltipCallback((shown, tooltip) ->
                        tooltip.add(Component.literal("Tier " + recipe.tier).withStyle(ChatFormatting.DARK_GRAY)));
    }

    @Override
    public void draw(AnvilSmithingRecipe recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, g, mouseX, mouseY);
        text(g, "Tier " + recipe.tier, 52, 43);
    }
}
