package com.hbm.handler.jei;

import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 1.7 {@code SILEXRecipeHandler}: {@code gui_nei_silex.png}, the wavelength in the label box, the input at
 * {@code (12,24)} with its output multiplier under the arrow, and the outputs in one or two columns, each with its share
 * of the total weight.
 */
public class SILEXRecipeHandler extends HbmRecipeCategory<SILEXRecipes.SILEXRecipe> {

    public SILEXRecipeHandler(ViewerType<SILEXRecipes.SILEXRecipe> type, Component title, ItemStack machine) {
        super(type, title, machine, "gui_nei_silex.png");
    }

    public static List<SILEXRecipes.SILEXRecipe> buildRecipes() {
        SILEXRecipes.register();
        return SILEXRecipes.recipes;
    }

    /** 1.7 {@code RecipeSet}: output {@code i} of {@code count}, as an item position. */
    static int[] outputPos(int i, int count) {
        int sep = count > 4 ? 3 : 2;
        if (i < sep) return new int[]{68, 24 + i * 18 - 9 * ((Math.min(count, sep) + 1) / 2)};
        return new int[]{116, 24 + (i - sep) * 18 - 9 * ((Math.min(count - sep, sep) + 1) / 2)};
    }

    @Override
    public void setRecipe(SlotLayout builder, SILEXRecipes.SILEXRecipe recipe) {
        builder.addSlot(Role.INPUT, 12, 24).addItemStacks(recipe.input.getDisplayStacks());
        for (int i = 0; i < recipe.outputs.size(); i++) {
            int[] pos = outputPos(i, recipe.outputs.size());
            builder.addSlot(Role.OUTPUT, pos[0], pos[1]).addItemStack(recipe.outputs.get(i).stack());
        }
    }

    @Override
    public void draw(SILEXRecipes.SILEXRecipe recipe, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        super.draw(recipe, g, mouseX, mouseY);
        Font font = Minecraft.getInstance().font;

        double weight = recipe.totalWeight();
        for (int i = 0; i < recipe.outputs.size(); i++) {
            double chance = 100 * recipe.outputs.get(i).weight() / weight;
            int[] pos = outputPos(i, recipe.outputs.size());
            text(g, ((int) (chance * 10D) / 10D) + "%", pos[0] + 18, pos[1] + 4);
        }

        // 1.7 divides the ints before widening, so a 900/1000 recipe reads 0.0x
        double produced = recipe.fluidProduced / recipe.fluidConsumed;
        String am = ((int) (produced * 10D) / 10D) + "x";
        text(g, am, 52 - font.width(am) / 2, 43);

        EnumWavelengths laser = recipe.laserStrength;
        Component wavelength = laser == EnumWavelengths.NULL
                ? Component.literal("N/A").withStyle(ChatFormatting.WHITE)
                : Component.translatable(laser.name).withStyle(laser.textColor);
        GuiDraw.text(g, font, wavelength, 33 - font.width(wavelength) / 2, 8, 0xFF404040, false);
    }
}
