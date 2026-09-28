package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerToolBox;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUIToolBox}: 176x211, the box name centred at y 37, the inventory label at y 117. */
public class GUIToolBox extends CompatContainerScreen<ContainerToolBox> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_toolbox.png");
    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 211;

    public GUIToolBox(ContainerToolBox menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, X_SIZE, Y_SIZE);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, X_SIZE, Y_SIZE, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, X_SIZE / 2 - font.width(title) / 2, 37, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, Y_SIZE - 96 + 2, 0xFF404040, false);
    }
}
