package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFunnel;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUIFunnel}: 176x168 processing sheet with the mode button at (159,73). */
public final class GUIFunnel extends CompatContainerScreen<ContainerFunnel> {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_funnel.png");
    public GUIFunnel(ContainerFunnel menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 168); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (menu.getFunnel() != null) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 159, topPos + 73, 176F,
                    menu.getFunnel().mode * 10F, 10, 10, 256, 256);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        if (isHovering(159, 73, 10, 10, event.x(), event.y())) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
