package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeBoy;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Little Boy schematic screen with the five installed-part overlays from 1.12.2. */
public class GUINukeBoy extends dev.golem.core.gui.CompatContainerScreen<ContainerNukeBoy> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/lilboyschematic.png");

    public GUINukeBoy(ContainerNukeBoy menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (menu.isReady()) GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 90, 176F, 0F, 16, 16, 256, 256);
        if (menu.hasPart(0)) GuiDraw.blit(graphics, TEXTURE, leftPos + 27, topPos + 87, 176F, 16F, 21, 22, 256, 256);
        if (menu.hasPart(1)) GuiDraw.blit(graphics, TEXTURE, leftPos + 27, topPos + 89, 176F, 38F, 21, 18, 256, 256);
        if (menu.hasPart(2)) GuiDraw.blit(graphics, TEXTURE, leftPos + 74, topPos + 94, 176F, 57F, 19, 8, 256, 256);
        if (menu.hasPart(3)) GuiDraw.blit(graphics, TEXTURE, leftPos + 92, topPos + 95, 176F, 66F, 12, 6, 256, 256);
        if (menu.hasPart(4)) GuiDraw.blit(graphics, TEXTURE, leftPos + 107, topPos + 91, 176F, 75F, 16, 14, 256, 256);
    }
}
