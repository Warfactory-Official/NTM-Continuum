package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineAssemblyMachine;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

public class GUIMachineAssemblyMachine extends AbstractContainerScreen<ContainerMachineAssemblyMachine> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/processing/gui_assembler.png");

    public GUIMachineAssemblyMachine(ContainerMachineAssemblyMachine menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineAssemblyMachine te = this.menu.getAssembler();
        if (te == null) return;

        long max = te.maxPower;
        int p = max <= 0L ? 0 : (int) (te.power * 61L / max);
        if (p > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 152, this.topPos + 79 - p, 176.0F, (float) (61 - p), 16, p, 256, 256);
        }

        double progress = te.assemblerModule.progress;
        if (progress > 0) {
            int j = (int) Math.ceil(70 * progress);
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 62, this.topPos + 126, 176.0F, 61.0F, j, 16, 256, 256);
        }

        GenericRecipe recipe = te.assemblerModule.getRecipe();
        boolean configured = recipe != null || ItemBlueprints.grabPool(te.getItem(1)) != null;
        boolean powered = recipe != null ? te.power >= recipe.power : te.power > 0;
        boolean processing = te.didProcess;

        if (processing) {
            led(graphics, 51, 195);
        } else if (configured) {
            led(graphics, 51, 192);
        }

        if (processing) {
            led(graphics, 56, 195);
        } else if (configured && powered) {
            led(graphics, 56, 192);
        }

        graphics.item(recipe != null ? recipe.getIcon() : new ItemStack(ModItems.template_folder.get()),
                this.leftPos + 8, this.topPos + 126);

        if (recipe != null && recipe.inputItem != null) {
            for (int i = 0; i < Math.min(recipe.inputItem.length, 12); i++) {
                if (this.menu.slots.get(4 + i).hasItem()) continue;
                int sx = 8 + (i % 3) * 18, sy = 18 + (i / 3) * 18;

                graphics.item(recipe.inputItem[i].getCyclingDisplayStack(), this.leftPos + sx, this.topPos + sy);
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + sx, this.topPos + sy,
                        (float) sx, (float) sy, 16, 16, 256, 256, 0x80FFFFFF);
            }
        }
    }

    private static final int BTN_X = 7, BTN_Y = 125, BTN_W = 18, BTN_H = 18;

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        TileEntityMachineAssemblyMachine te = this.menu.getAssembler();
        if (te != null && this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            GUIScreenRecipeSelector.openSelector(AssemblyMachineRecipes.INSTANCE, te.getBlockPos(),
                    te.assemblerModule.recipe, 0, ItemBlueprints.grabPool(te.getItem(1)), this);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void led(GuiGraphicsExtractor graphics, int x, int u) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + x, this.topPos + 121, (float) u, 0.0F, 3, 6, 256, 256);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);

        TileEntityMachineAssemblyMachine te = this.menu.getAssembler();
        if (te == null) return;
        if (this.isHovering(152, 18, 16, 61, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, GuiUtil.energyTooltip(te.power, te.maxPower),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {

            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(te.assemblerModule.recipe);
            if (recipe != null) {
                graphics.setTooltipForNextFrame(this.font, recipe.print(), Optional.empty(), mouseX, mouseY);
            } else {
                graphics.setTooltipForNextFrame(this.font,
                        List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW)),
                        Optional.empty(), mouseX, mouseY);
            }
        }
    }
}
