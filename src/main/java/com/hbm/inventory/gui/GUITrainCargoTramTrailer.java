package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuTrainCargoTramTrailer;
import com.hbm.lib.Library;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 GUITrainCargoTramTrailer (ntm-next ScreenTrainCargoTramTrailer): the trailer's cargo grid. */
public class GUITrainCargoTramTrailer extends CompatContainerScreen<MenuTrainCargoTramTrailer> {

    private static final Identifier TEXTURE = Library.id("textures/gui/vehicles/gui_cargo_tram_trailer.png");

    public GUITrainCargoTramTrailer(MenuTrainCargoTramTrailer menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 222);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 8, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }
}
