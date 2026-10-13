package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMicrowave;
import com.hbm.tileentity.machine.TileEntityMicrowave;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/** 1.7 {@code GUIMicrowave}: {@code gui_microwave.png}, 176x168; the two speed arrows at (43,25) and (43,43). */
public final class GUIMicrowave extends CompatContainerScreen<ContainerMicrowave> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_microwave.png");

    public GUIMicrowave(ContainerMicrowave menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 168);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMicrowave microwave = menu.getMicrowave();
        if (microwave == null) return;

        int i = (int) microwave.getPowerScaled(34);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 51 - i, 176F, 34F - i, 16, i, 256, 256);

        int j = Math.min(microwave.getProgressScaled(23), 22);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 104, topPos + 34, 192F, 0F, j, 16, 256, 256);

        int k = microwave.getSpeedScaled(34);
        if (k > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 60 - k, 214F, 34F - k, 4, k, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMicrowave microwave = menu.getMicrowave();
        if (microwave == null) return;
        if (isHovering(8, 51 - 34, 16, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(microwave.power, TileEntityMicrowave.maxPower),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null) return false;
        if (isHovering(43, 25, 18, 18, event.x(), event.y())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            return true;
        }
        if (isHovering(43, 43, 18, 18, event.x(), event.y())) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
            return true;
        }
        return false;
    }
}
