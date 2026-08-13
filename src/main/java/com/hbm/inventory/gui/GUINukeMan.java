package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerNukeMan;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Fat Man ({@code nuke_man}). Uses the 26.1 render API ({@code extractBackground}
 * + {@code blit}). The lens "installed" overlays and the ready indicator are drawn from the right-hand
 * side of the schematic texture, gated by the container's arming state (read off the synced BE).
 */
public class GUINukeMan extends dev.golem.core.gui.CompatContainerScreen<ContainerNukeMan> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/weapon/fatmanschematic.png");

    public GUINukeMan(ContainerNukeMan menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
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
        int x = this.leftPos;
        int y = this.topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.exp1()) GuiDraw.blit(graphics, TEXTURE, x + 82, y + 19, 176.0F, 0.0F, 24, 24, 256, 256);
        if (this.menu.exp2()) GuiDraw.blit(graphics, TEXTURE, x + 106, y + 19, 200.0F, 0.0F, 24, 24, 256, 256);
        if (this.menu.exp3()) GuiDraw.blit(graphics, TEXTURE, x + 82, y + 43, 176.0F, 24.0F, 24, 24, 256, 256);
        if (this.menu.exp4()) GuiDraw.blit(graphics, TEXTURE, x + 106, y + 43, 200.0F, 24.0F, 24, 24, 256, 256);
        if (this.menu.isReady()) GuiDraw.blit(graphics, TEXTURE, x + 134, y + 35, 176.0F, 48.0F, 16, 16, 256, 256);
    }
}
