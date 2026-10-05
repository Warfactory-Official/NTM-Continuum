package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFurnaceSteel;
import com.hbm.tileentity.machine.TileEntityFurnaceSteel;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Steel furnace screen with three progress/bonus lanes and the heat gauge. */
public class GUIFurnaceSteel extends dev.golem.core.gui.CompatContainerScreen<ContainerFurnaceSteel> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_furnace_steel.png");

    public GUIFurnaceSteel(ContainerFurnaceSteel menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    /** 1.7 {@code GUIFurnaceSteel.drawGuiContainerForegroundLayer}: centred title at y 6, "Inventory" at (8, ySize - 94). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityFurnaceSteel furnace = menu.getFurnace();
        if (furnace == null) return;
        int heat = furnace.heat * 48 / TileEntityFurnaceSteel.maxHeat;
        GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 67 - heat,
                176F, 76F - heat, 7, heat, 256, 256);
        for (int i = 0; i < 3; i++) {
            int progress = furnace.progress[i] * 69 / TileEntityFurnaceSteel.processTime;
            int bonus = furnace.bonus[i] * 69 / 100;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 54, topPos + 18 + 18 * i,
                    176F, 18F, progress, 5, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 54, topPos + 27 + 18 * i,
                    176F, 23F, bonus, 5, 256, 256);
            if (furnace.wasOn) GuiDraw.blit(graphics, TEXTURE, leftPos + 16, topPos + 16 + 18 * i,
                    176F, 0F, 18, 18, 256, 256);
        }
    }

    /** 1.7 {@code GUIFurnaceSteel.drawScreen}: per-lane progress and bonus stats, then the heat stat ("TU" unspaced, as 1.7). */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFurnaceSteel furnace = menu.getFurnace();
        if (furnace == null) return;
        for (int i = 0; i < 3; i++) {
            if (in17(53, 17 + 18 * i, 70, 7, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, List.of(Component.literal(String.format(Locale.US, "%,d", furnace.progress[i])
                        + " / " + String.format(Locale.US, "%,d", TileEntityFurnaceSteel.processTime) + "TU")),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
            if (in17(53, 26 + 18 * i, 70, 7, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, List.of(Component.literal("Bonus: " + furnace.bonus[i] + "%")),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        if (in17(151, 18, 9, 50, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(String.format(Locale.US, "%,d", furnace.heat)
                    + " / " + String.format(Locale.US, "%,d", TileEntityFurnaceSteel.maxHeat) + "TU")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    /** The 1.7 GUI hit test ({@code drawCustomInfoStat}/{@code renderTankInfo}): x <= mouse < x + w, y < mouse <= y + h. */
    private boolean in17(int x, int y, int w, int h, int mouseX, int mouseY) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        return x <= mx && x + w > mx && y < my && y + h >= my;
    }
}
