package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.inventory.container.ContainerPneumoStorageMono;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageMono;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoTube;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** 1.7 {@code GUIPneumoStorageMono}: three typed cells with fill bars, counts and percentages. */
public class GUIPneumoStorageMono extends PneumaticScreenBase<ContainerPneumoStorageMono> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_pneumatic_mono.png");

    public GUIPneumoStorageMono(ContainerPneumoStorageMono menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 200, 181);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        TileEntityPneumoStorageMono storage = menu.tile();
        if (storage == null) return;

        for (int i = 0; i < 3; i++) {
            if (!storage.getItem(i).isEmpty()) {
                int bar = (int) ((long) storage.amounts[i] * 124 / TileEntityPneumoStorageMono.CAPACITY);
                if (bar > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 44, topPos + 17 + i * 18, 0, 181, bar, 16, 256, 256);
            }
        }

        GuiDraw.blit(graphics, TEXTURE, leftPos + 174 + 4 * (storage.compair.getPressure() - 1), topPos + 36, 200, 0, 4, 8, 256, 256);
        Gauges.needle(graphics, leftPos + 184, topPos + 25,
                (double) storage.compair.getFill() / (double) storage.compair.getMaxFill(), 5, 2, 1, 0xCA6C43, 0xAB4223);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 176 / 2 - font.width(title) / 2, 5, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);

        TileEntityPneumoStorageMono storage = menu.tile();
        if (storage == null) return;

        for (int k = 0; k < 3; k++) {
            if (!storage.getItem(k).isEmpty()) {
                int amount = storage.amounts[k];
                String percent = " (" + (((int) (amount * 1000D / (double) TileEntityPneumoStorageMono.CAPACITY)) / 10D) + "%)";
                GuiDraw.text(graphics, font, String.format(Locale.US, "%,d", amount) + percent, 50, 22 + k * 18, 0xFF000000, false);
            }
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityPneumoStorageMono storage = menu.tile();
        if (storage == null) return;
        drawCustomInfoStat(graphics, mouseX, mouseY, 174, 36, 20, 8, "Compressor: " + storage.compair.getPressure() + " PU",
                "Max range: " + TileEntityPneumoTube.getRangeFromPressure(storage.compair.getPressure()) + "m");
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityPneumoStorageMono storage = menu.tile();
        if (storage == null) return false;
        return clickSendFlag(storage.getBlockPos(), event.x(), event.y(), 174, 36, 20, 8, "pressure");
    }
}
