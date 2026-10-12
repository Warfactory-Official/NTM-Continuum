package com.hbm.handler.jei;

import dev.golem.core.viewer.Role;
import dev.golem.core.viewer.SlotLayout;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.recipes.StorageDrumRecipes;
import dev.golem.core.viewer.ViewerType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The old 99x27 storage-drum recipe strip: waste in, depleted waste out. */
public final class StorageDrumRecipeHandler extends HbmRecipeCategory<StorageDrumRecipeHandler.Entry> {

    public record Entry(ItemStack input, ItemStack output) {}

    public StorageDrumRecipeHandler(ViewerType<Entry> type, Component title) {
        super(type, title, new ItemStack(ModMachineBlocks.machine_storage_drum.get()),
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
    public void setRecipe(SlotLayout builder, Entry recipe) {
        builder.addSlot(Role.INPUT, 5, 5).addItemStacks(List.of(recipe.input()));
        builder.addSlot(Role.OUTPUT, 78, 6).addItemStacks(List.of(recipe.output()));
    }

    @Override
    public void draw(Entry recipe, net.minecraft.client.gui.GuiGraphicsExtractor graphics,
                     double mouseX, double mouseY) {
        super.draw(recipe, graphics, mouseX, mouseY);
    }
}
