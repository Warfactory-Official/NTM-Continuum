package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerPneumoStorageClutter;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageClutter;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUIPneumoStorageClutter}: the 6x9 bank with the compressor gauge and pressure selector at the right. */
public class GUIPneumoStorageClutter extends PneumaticScreenBase<ContainerPneumoStorageClutter> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_pneumatic_clutter.png");

    public GUIPneumoStorageClutter(ContainerPneumoStorageClutter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 235);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        TileEntityPneumoStorageClutter storage = menu.tile();
        if (storage == null) return;
        GuiDraw.blit(graphics, TEXTURE, leftPos + 174 + 4 * (storage.compair.getPressure() - 1), topPos + 36, 200, 0, 4, 8, 256, 256);
        drawSmoothGauge(graphics, leftPos + 184, topPos + 25,
                (double) storage.compair.getFill() / (double) storage.compair.getMaxFill(), 5, 2, 1, 0xCA6C43, 0xAB4223);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 176 / 2 - font.width(title) / 2, 5, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityPneumoStorageClutter storage = menu.tile();
        if (storage == null) return;
        drawCustomInfoStat(graphics, mouseX, mouseY, 174, 36, 20, 8, "Compressor: " + storage.compair.getPressure() + " PU",
                "Max range: " + TileEntityPneumoTube.getRangeFromPressure(storage.compair.getPressure()) + "m");
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityPneumoStorageClutter storage = menu.tile();
        if (storage == null) return false;
        return clickSendFlag(storage.getBlockPos(), event.x(), event.y(), 174, 36, 20, 8, "pressure");
    }
}
