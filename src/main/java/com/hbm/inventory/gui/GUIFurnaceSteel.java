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

    @Override protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        inventoryLabelY = imageHeight - 96 + 2;
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

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFurnaceSteel furnace = menu.getFurnace();
        if (furnace == null) return;
        for (int i = 0; i < 3; i++) {
            if (isHovering(53, 17 + 18 * i, 70, 7, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, List.of(Component.literal(String.format(Locale.US,
                        "%,d / %,d TU", furnace.progress[i], TileEntityFurnaceSteel.processTime))),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
            if (isHovering(53, 26 + 18 * i, 70, 7, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, font, List.of(Component.literal("Bonus: " + furnace.bonus[i] + "%")),
                        Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        if (isHovering(151, 18, 9, 50, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(String.format(Locale.US,
                    "%,d / %,d TU", furnace.heat, TileEntityFurnaceSteel.maxHeat))),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
