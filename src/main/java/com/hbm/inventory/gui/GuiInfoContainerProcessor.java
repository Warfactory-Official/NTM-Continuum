package com.hbm.inventory.gui;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.module.machine.ModuleMachineBase;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/** 1.7 {@code GuiInfoContainerProcessor}: the recipe selector buttons, their tooltips, the input ghosts and the LEDs. */
public abstract class GuiInfoContainerProcessor<T extends AbstractContainerMenu> extends CompatContainerScreen<T> {

    protected GuiInfoContainerProcessor(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    /** One {x, y, template slot} per recipe selector, index-aligned with {@link #processorModules}. */
    public abstract int[][] getSelectorPositions();

    protected abstract ModuleMachineBase @Nullable [] processorModules();

    protected abstract BlockPos machinePos();

    public abstract Identifier getTexture();

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ModuleMachineBase[] modules = processorModules();
        if (modules == null) return;
        int[][] selectors = getSelectorPositions();
        for (int i = 0; i < selectors.length; i++) {
            if (!GuiUtil.hover(mouseX, mouseY, leftPos + selectors[i][0], topPos + selectors[i][1], 18, 18)) continue;
            GenericRecipe recipe = modules[i].getRecipe();
            List<Component> lines = recipe != null ? recipe.print()
                    : List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW));
            GuiDraw.tooltip(graphics, this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        ModuleMachineBase[] modules = processorModules();
        if (modules == null) return false;
        int[][] selectors = getSelectorPositions();
        for (int i = 0; i < selectors.length; i++) {
            if (!GuiUtil.hover(event.x(), event.y(), leftPos + selectors[i][0], topPos + selectors[i][1], 18, 18)) continue;
            GuiUtil.playClickSound();
            ModuleMachineBase module = modules[i];
            GUIScreenRecipeSelector.openSelector(module.getRecipeSet(), machinePos(), module.recipe, 0,
                    ItemBlueprints.grabPool(this.menu.getSlot(selectors[i][2]).getItem()), this);
            return true;
        }
        return false;
    }

    /** The two 3×6 LEDs, five pixels apart: lit while processing, dim when a recipe is set (and powered, for the right one). */
    protected void renderStandardLEDs(GuiGraphicsExtractor graphics, boolean didProcess, @Nullable GenericRecipe recipe, long power, int lX, int lY, int sX, int sY) {
        if (didProcess) led(graphics, lX, lY, sX, sY);
        else if (recipe != null) led(graphics, lX, lY, sX - 3, sY);

        if (didProcess) led(graphics, lX + 5, lY, sX, sY);
        else if (recipe != null && power >= recipe.power) led(graphics, lX + 5, lY, sX - 3, sY);
    }

    private void led(GuiGraphicsExtractor graphics, int x, int y, int u, int v) {
        GuiDraw.blit(graphics, getTexture(), leftPos + x, topPos + y, (float) u, (float) v, 3, 6, 256, 256);
    }

    /** The selected recipe's icon on each selector and half-opacity previews of its inputs in empty slots. */
    protected void renderRecipeIcons(GuiGraphicsExtractor graphics) {
        ModuleMachineBase[] modules = processorModules();
        if (modules == null) return;
        int[][] selectors = getSelectorPositions();
        for (int i = 0; i < selectors.length; i++) {
            ModuleMachineBase module = modules[i];
            GenericRecipe recipe = module.getRecipe();
            ItemStack icon = recipe != null ? recipe.getIcon() : new ItemStack(ModItems.template_folder.get());
            GuiDraw.item(graphics, icon, leftPos + selectors[i][0] + 1, topPos + selectors[i][1] + 1);

            if (recipe == null || recipe.inputItem == null) continue;
            for (int j = 0; j < Math.min(recipe.inputItem.length, module.inputSlots.length); j++) {
                Slot slot = this.menu.getSlot(module.inputSlots[j]);
                if (slot.hasItem()) continue;
                GuiDraw.item(graphics, recipe.inputItem[j].getCyclingDisplayStack(), leftPos + slot.x, topPos + slot.y);
                GuiDraw.blit(graphics, getTexture(), leftPos + slot.x, topPos + slot.y, (float) slot.x, (float) slot.y, 16, 16, 256, 256, 0x80FFFFFF);
            }
        }
    }
}
