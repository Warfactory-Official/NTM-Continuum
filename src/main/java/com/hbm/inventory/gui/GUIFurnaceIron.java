package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFurnaceIron;
import com.hbm.tileentity.machine.TileEntityFurnaceIron;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Iron furnace screen, preserving the old progress and burn bars. */
public class GUIFurnaceIron extends dev.golem.core.gui.CompatContainerScreen<ContainerFurnaceIron> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_furnace_iron.png");

    public GUIFurnaceIron(ContainerFurnaceIron menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        inventoryLabelY = imageHeight - 96 + 2;
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

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFurnaceIron furnace = menu.getFurnace();
        if (furnace == null) return;
        if (isHovering(52, 35, 71, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(
                    furnace.progress * 100 / Math.max(furnace.processingTime, 1) + "%")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(52, 44, 71, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal((furnace.burnTime / 20) + "s")),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
