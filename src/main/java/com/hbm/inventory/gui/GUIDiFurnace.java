package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerDiFurnace;
import com.hbm.tileentity.machine.TileEntityDiFurnace;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/** The 176x166 legacy DiFurnace screen, including fuel and side-mode tooltips. */
public class GUIDiFurnace extends dev.golem.core.gui.CompatContainerScreen<ContainerDiFurnace> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/guidifurnace.png");

    public GUIDiFurnace(ContainerDiFurnace menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityDiFurnace furnace = menu.getFurnace();
        if (furnace == null) return;

        if (furnace.hasPower()) {
            int fuel = furnace.getPowerRemainingScaled(52);
            if (fuel > 0) {
                GuiDraw.blit(graphics, TEXTURE, leftPos + 44, topPos + 70 - fuel,
                        201F, 53F - fuel, 16, fuel, 256, 256);
            }
        }

        int progress = furnace.getDiFurnaceProgressScaled(24);
        if (furnace.isProcessing()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 101, topPos + 35,
                    176F, 14F, progress + 1, 17, 256, 256);
        }

        if (furnace.hasPower() && furnace.canProcess()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 63, topPos + 37,
                    176F, 0F, 14, 14, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityDiFurnace furnace = menu.getFurnace();
        if (furnace == null) return;

        if (isHovering(43, 17, 18, 54, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(furnace.fuel, TileEntityDiFurnace.MAX_FUEL),
                    Optional.empty(), mouseX, mouseY);
            return;
        }

        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < 3; i++) {
                Slot slot = menu.getSlot(i);
                if (!isHovering(slot.x - 1, slot.y - 1, 18, 18, mouseX, mouseY)) continue;
                byte side = i == 0 ? furnace.sideUpper : i == 1 ? furnace.sideLower : furnace.sideFuel;
                String name = DirectionNames.name(side);
                GuiDraw.tooltip(graphics, font, List.of(
                        Component.literal("Accepts items from: " + name).withStyle(ChatFormatting.YELLOW)),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    /** Keeps Direction's name lookup out of the screen's old-style slot code. */
    private static final class DirectionNames {
        private static String name(byte side) {
            int index = Math.max(0, Math.min(5, side));
            return net.minecraft.core.Direction.values()[index].getSerializedName();
        }
    }
}
