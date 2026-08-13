package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerWasteDrum;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Basic player-facing inventory for the spent-fuel pool drum. */
public class GUIWasteDrum extends dev.golem.core.gui.CompatContainerScreen<ContainerWasteDrum> {

    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

    public GUIWasteDrum(ContainerWasteDrum menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    //? if >=26.1 {
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
    *///?}
        //? if >=26.1 {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        //?}
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 8, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, 128, 0x404040, false);
    }
}
