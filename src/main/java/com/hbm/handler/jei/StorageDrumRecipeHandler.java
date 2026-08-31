package com.hbm.handler.jei;

//? if jei {

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.recipes.StorageDrumRecipes;
import dev.golem.core.jei.JeiRecipeType;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The old 99x27 storage-drum recipe strip: waste in, depleted waste out. */
public final class StorageDrumRecipeHandler extends HbmRecipeCategory<StorageDrumRecipeHandler.Entry> {

    public record Entry(ItemStack input, ItemStack output) {}

    public StorageDrumRecipeHandler(IGuiHelper helper, JeiRecipeType<Entry> type, Component title) {
        super(helper, type, title, new ItemStack(ModMachineBlocks.machine_storage_drum.get()),
                "gui_nei_wastedrum.png", 99, 27, 38, 29);
    }

    public static List<Entry> buildRecipes() {
        List<Entry> entries = new ArrayList<>();
        for (StorageDrumRecipes.Recipe recipe : StorageDrumRecipes.getAll()) {
            entries.add(new Entry(recipe.input(), recipe.output()));
        }
        return entries;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 5, 5).addItemStacks(List.of(recipe.input()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 78, 6).addItemStacks(List.of(recipe.output()));
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView slots, net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                     double mouseX, double mouseY) {
        super.draw(recipe, slots, graphics, mouseX, mouseY);
    }
}
//?}
