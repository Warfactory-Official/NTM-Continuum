package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerSafe;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUISafe}: 176x168, dark labels. */
public class GUISafe extends CompatContainerScreen<ContainerSafe> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_safe.png");

    public GUISafe(ContainerSafe menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }
}
