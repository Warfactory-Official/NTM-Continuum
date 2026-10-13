package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMassStorage;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The old 176x221 mass-storage screen, including the stockpile gauge and two controls. */
public class GUIMassStorage extends CompatContainerScreen<ContainerMassStorage> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_mass_storage.png");

    public GUIMassStorage(ContainerMassStorage menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 221);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        int capacity = menu.getCapacity();
        int gauge = capacity <= 0 ? 0 : menu.getStockpile() * 88 / capacity;
        if (gauge > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 97, topPos + 105 - gauge,
                    176F, 88F - gauge, 16, gauge, 256, 256);
        }
        if (menu.isOutputEnabled()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 80, topPos + 72, 192F, 0F, 14, 14, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(96, 16, 18, 90, mouseX, mouseY)) {
            int capacity = menu.getCapacity();
            double percent = capacity <= 0 ? 0D : ((int) (menu.getStockpile() * 1000D / capacity)) / 10D;
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(
                    String.format(Locale.US, "%,d / %,d", menu.getStockpile(), capacity)),
                    Component.literal(percent + "%")), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(62, 72, 14, 14, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    List.of(Component.literal("Click: Provide one"), Component.literal("Shift-click: Provide stack")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(80, 72, 14, 14, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("Toggle output")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int button = -1;
        if (isHovering(62, 72, 14, 14, event.x(), event.y())) {
            button = event.hasShiftDown() ? 1 : 0;
        } else if (isHovering(80, 72, 14, 14, event.x(), event.y())) {
            button = 2;
        }

        if (button >= 0) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
                return true;
            }
        }
        return false;
    }
}
