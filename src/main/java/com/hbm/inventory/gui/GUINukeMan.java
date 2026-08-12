package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeMan;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the Fat Man ({@code nuke_man}). Uses the 26.1 render API ({@code extractBackground}
 * + {@code blit}). The lens "installed" overlays and the ready indicator are drawn from the right-hand
 * side of the schematic texture, gated by the container's arming state (read off the synced BE).
 */
public class GUINukeMan extends AbstractContainerScreen<ContainerNukeMan> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/weapon/fatmanschematic.png");

    public GUINukeMan(ContainerNukeMan menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (this.menu.exp1()) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 82, y + 19, 176.0F, 0.0F, 24, 24, 256, 256);
        if (this.menu.exp2()) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 106, y + 19, 200.0F, 0.0F, 24, 24, 256, 256);
        if (this.menu.exp3()) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 82, y + 43, 176.0F, 24.0F, 24, 24, 256, 256);
        if (this.menu.exp4()) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 106, y + 43, 200.0F, 24.0F, 24, 24, 256, 256);
        if (this.menu.isReady()) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + 134, y + 35, 176.0F, 48.0F, 16, 16, 256, 256);
    }
}
