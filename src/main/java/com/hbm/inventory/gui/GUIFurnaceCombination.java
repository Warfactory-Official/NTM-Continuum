package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFurnaceCombination;
import com.hbm.tileentity.machine.TileEntityFurnaceCombination;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Screen for the combination furnace, preserving the 176x186 legacy layout. */
public class GUIFurnaceCombination extends dev.golem.core.gui.CompatContainerScreen<ContainerFurnaceCombination> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_furnace_combination.png");

    public GUIFurnaceCombination(ContainerFurnaceCombination menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
        titleLabelY = 6;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityFurnaceCombination furnace = menu.getFurnace();
        if (furnace == null) return;

        int progress = furnace.getProgressScaled(38);
        if (progress > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 45, topPos + 37, 176F, 0F, progress, 5, 256, 256);
        int heat = furnace.getHeatScaled(37);
        if (heat > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 45, topPos + 46, 176F, 5F, heat, 5, 256, 256);

        drawTank(graphics, furnace, 118, 18, 16, 52);
    }

    private void drawTank(GuiGraphicsExtractor graphics, TileEntityFurnaceCombination furnace,
                          int x, int y, int width, int height) {
        int fill = furnace.tank.getFill();
        int max = furnace.tank.getMaxFill();
        int filledHeight = max <= 0 ? 0 : fill * height / max;
        if (filledHeight <= 0 || furnace.tank.getTankType() == com.hbm.inventory.fluid.Fluids.NONE) return;
        int x0 = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - filledHeight;
        Identifier fluidTexture = furnace.tank.getTankType().getTexture();
        if (net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + width, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x0, tileY, 0F, 0F, width, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, top, x0 + width, bottom, 0xFF000000 | (furnace.tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFurnaceCombination furnace = menu.getFurnace();
        if (furnace == null) return;
        if (isHovering(118, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    furnace.tank.getTankType().getDisplayName(),
                    Component.literal(furnace.tank.getFill() + " / " + furnace.tank.getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(44, 36, 39, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(String.format(Locale.US, "%,d / %,d TU",
                    furnace.progress, TileEntityFurnaceCombination.processTime))), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(44, 45, 39, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(String.format(Locale.US, "%,d / %,d TU",
                    furnace.heat, TileEntityFurnaceCombination.maxHeat))), Optional.empty(), mouseX, mouseY);
        }
    }
}
