package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFurnaceIron;
import com.hbm.tileentity.machine.TileEntityFurnaceIron;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/** Iron furnace screen, preserving the old progress and burn bars. */
public class GUIFurnaceIron extends dev.golem.core.gui.CompatContainerScreen<ContainerFurnaceIron> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_furnace_iron.png");

    public GUIFurnaceIron(ContainerFurnaceIron menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    /** 1.7 {@code GUIFurnaceIron.drawGuiContainerForegroundLayer}: centred title at y 6, "Inventory" at (8, ySize - 94). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityFurnaceIron furnace = menu.getFurnace();
        if (furnace == null) return;
        int progress = furnace.progress * 70 / Math.max(furnace.processingTime, 1);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 36, 176F, 18F, progress, 5, 256, 256);
        int burn = furnace.burnTime * 70 / Math.max(furnace.maxBurnTime, 1);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 45, 176F, 23F, burn, 5, 256, 256);
        if (furnace.getLevel() != null && furnace.canSmelt(furnace.getLevel())) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 70, topPos + 16, 176F, 0F, 18, 18, 256, 256);
        }
    }

    /**
     * 1.7 {@code GUIFurnaceIron.drawScreen}: the burn-time bonus list over an empty fuel slot (1 or 2) while nothing
     * is carried, then the progress and burn-time stats.
     */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFurnaceIron furnace = menu.getFurnace();
        if (furnace == null) return;
        if (menu.getCarried().isEmpty()) {
            for (int i = 1; i < 3; ++i) {
                Slot slot = menu.getSlot(i);
                if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && !slot.hasItem()) {
                    List<String> bonuses = furnace.burnModule.getTimeDesc();
                    if (!bonuses.isEmpty()) {
                        GuiDraw.tooltip(graphics, font, bonuses.stream().map(line -> (Component) Component.literal(line)).toList(),
                                Optional.empty(), mouseX, mouseY);
                        return;
                    }
                }
            }
        }
        if (in17(52, 35, 71, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(
                    furnace.progress * 100 / Math.max(furnace.processingTime, 1) + "%")),
                    Optional.empty(), mouseX, mouseY);
        } else if (in17(52, 44, 71, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal((furnace.burnTime / 20) + "s")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    /** The 1.7 GUI hit test ({@code drawCustomInfoStat}/{@code renderTankInfo}): x <= mouse < x + w, y < mouse <= y + h. */
    private boolean in17(int x, int y, int w, int h, int mouseX, int mouseY) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        return x <= mx && x + w > mx && y < my && y + h >= my;
    }
}
