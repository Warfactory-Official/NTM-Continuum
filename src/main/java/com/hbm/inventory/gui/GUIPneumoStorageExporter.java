package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerPneumoStorageExporter;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageExporter;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code GUIPneumoStorageExporter}: nine filters, nine outputs and the three mode buttons at the right. */
public class GUIPneumoStorageExporter extends PneumaticScreenBase<ContainerPneumoStorageExporter> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_pneumatic_exporter.png");

    public GUIPneumoStorageExporter(ContainerPneumoStorageExporter menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 185);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        TileEntityPneumoStorageExporter exporter = menu.tile();
        if (exporter == null) return;

        if (exporter.rorConfiguredMode) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 52, imageWidth, 18, 18, 18, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 14, topPos + 14, 77, 14, 58, 58, 256, 256);
        }
        if (!exporter.continuousRequest) GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 16, imageWidth, 0, 18, 18, 256, 256);
        if (exporter.requestMode == TileEntityPneumoStorageExporter.MODE_FULL_STACK) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 34, imageWidth + 18, 0, 18, 18, 256, 256);
        }
        if (exporter.requestMode == TileEntityPneumoStorageExporter.MODE_FULL_REQUEST) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 34, imageWidth + 18, 18, 18, 18, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 5, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityPneumoStorageExporter exporter = menu.tile();
        if (exporter == null) return;

        drawCustomInfoStat(graphics, mouseX, mouseY, 142, 16, 18, 18,
                "Request mode: " + ChatFormatting.YELLOW + (exporter.continuousRequest ? "Continuous" : "By request"));

        drawCustomInfoStat(graphics, mouseX, mouseY, 142, 34, 18, 18, "Request type: " + ChatFormatting.YELLOW + (
                exporter.requestMode == TileEntityPneumoStorageExporter.MODE_AS_MUCH_AS_POSSIBLE ? "As much as possible"
                        : exporter.requestMode == TileEntityPneumoStorageExporter.MODE_FULL_STACK ? "Only full stacks"
                        : "Only full requests"));

        if (exporter.rorConfiguredMode) {
            String[] label = new String[10];
            label[0] = "Filter type: " + ChatFormatting.YELLOW + "RoR configured";
            for (int i = 0; i < 9; i++) {
                ItemStack filter = exporter.rorFilters[i];
                boolean hasFilter = !filter.isEmpty() && filter.getCount() > 0;
                label[i + 1] = "Slot " + (i + 1) + ": " + (!hasFilter ? "None"
                        : (filter.getHoverName().getString() + " x" + filter.getCount()));
            }
            drawCustomInfoStat(graphics, mouseX, mouseY, 142, 52, 18, 18, label);
        } else {
            drawCustomInfoStat(graphics, mouseX, mouseY, 142, 52, 18, 18,
                    "Filter type: " + ChatFormatting.YELLOW + "Manually configured");
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityPneumoStorageExporter exporter = menu.tile();
        if (exporter == null) return false;
        double x = event.x(), y = event.y();
        boolean handled = false;
        handled |= clickSendFlag(exporter.getBlockPos(), x, y, 142, 16, 18, 18, "continuous");
        handled |= clickSendFlag(exporter.getBlockPos(), x, y, 142, 34, 18, 18, "request");
        handled |= clickSendFlag(exporter.getBlockPos(), x, y, 142, 52, 18, 18, "ror");
        return handled;
    }
}
