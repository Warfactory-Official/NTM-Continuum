package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuCartDestroyer;
import com.hbm.lib.Library;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 GUICartDestroyer (ntm-next ScreenCartDestroyer): the two filter grids and the clock-driven blade strip. */
public class GUICartDestroyer extends CompatContainerScreen<MenuCartDestroyer> {

    private static final Identifier TEXTURE = Library.id("textures/gui/vehicles/gui_destroyer.png");

    public GUICartDestroyer(MenuCartDestroyer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 96 + 4;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        // 1.7: the blade strip animates off the wall clock
        int index = (int) (System.currentTimeMillis() % 1000 / 128);
        if (index == 1 || index == 7) GuiDraw.blit(graphics, TEXTURE, leftPos + 66, topPos + 35, 0, 166, 44, 16, 256, 256);
        if (index == 2 || index == 6) GuiDraw.blit(graphics, TEXTURE, leftPos + 66, topPos + 35, 0, 182, 44, 16, 256, 256);
        if (index == 3 || index == 5) GuiDraw.blit(graphics, TEXTURE, leftPos + 66, topPos + 35, 0, 198, 44, 16, 256, 256);
        if (index == 4) GuiDraw.blit(graphics, TEXTURE, leftPos + 66, topPos + 35, 0, 214, 44, 16, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 8, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}
