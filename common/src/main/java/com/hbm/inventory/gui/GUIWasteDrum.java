package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerWasteDrum;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Basic player-facing inventory for the spent-fuel pool drum. */
public class GUIWasteDrum extends AbstractContainerScreen<ContainerWasteDrum> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    public GUIWasteDrum(ContainerWasteDrum menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 8, 6, 0x404040, false);
        graphics.text(font, playerInventoryTitle, 8, 128, 0x404040, false);
    }
}
