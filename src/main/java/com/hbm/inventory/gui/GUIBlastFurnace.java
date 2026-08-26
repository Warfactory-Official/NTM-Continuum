package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerBlastFurnace;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Blast furnace screen preserving the legacy 176x222 layout and two fluid gauges. */
public class GUIBlastFurnace extends dev.golem.core.gui.CompatContainerScreen<ContainerBlastFurnace> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_blast_furnace.png");

    public GUIBlastFurnace(ContainerBlastFurnace menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
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
        TileEntityMachineBlastFurnace furnace = menu.getFurnace();
        if (furnace == null) return;

        int fuel = Math.round(furnace.fuel * 26F / TileEntityMachineBlastFurnace.MAX_FUEL);
        int progress = Math.round(furnace.progress * (88F - fuel));
        if (progress > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 106 - progress - fuel,
                    176F, 102F - progress - fuel, 56, progress, 256, 256);
        }
        if (fuel > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 106 - fuel,
                    176F, 128F - fuel, 56, fuel, 256, 256);
        }
        if (furnace.isProgressing) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 81, topPos + 64, 176F, 0F, 14, 14, 256, 256);
        }

        drawGauge(graphics, leftPos + 34, topPos + 80,
                (double) furnace.tanks[0].getFill() / furnace.tanks[0].getMaxFill(), 0x800000);
        drawGauge(graphics, leftPos + 34, topPos + 26,
                (double) furnace.tanks[1].getFill() / furnace.tanks[1].getMaxFill(), 0x800000);
    }

    private static void drawGauge(GuiGraphicsExtractor graphics, int x, int y, double progress, int color) {
        progress = Math.max(0D, Math.min(1D, progress));
        float angle = (float) Math.toRadians(progress * 270D + 45D);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, x, y);
        GuiDraw.rotate(graphics, angle);
        GuiDraw.scale(graphics, 1F / 8F, 1F / 8F);
        fillNeedle(graphics, 1.5D, 0xFF000000);
        fillNeedle(graphics, 1D, 0xFF000000 | (color & 0xFFFFFF));
        GuiDraw.popMatrix(graphics);
    }

    private static void fillNeedle(GuiGraphicsExtractor graphics, double multiplier, int color) {
        double tipLength = 5D * multiplier * 8D;
        double backLength = 2D * multiplier * 8D;
        double backSide = multiplier * 8D;
        double total = tipLength + backLength;
        int top = (int) Math.round(tipLength);
        int bottom = (int) Math.round(-backLength);
        for (int y = bottom; y < top; y++) {
            int halfWidth = (int) Math.round(backSide * (tipLength - y) / total);
            if (halfWidth > 0) graphics.fill(-halfWidth, y, halfWidth, y + 1, color);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineBlastFurnace furnace = menu.getFurnace();
        if (furnace == null) return;
        if (isHovering(79, 62, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    List.of(Component.literal("Speed: " + (int) (furnace.speed * 100F) + "%")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(25, 71, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.translatable("hbmfluid.airblast"),
                    Component.literal(furnace.tanks[0].getFill() + " / " + furnace.tanks[0].getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(25, 17, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.translatable("hbmfluid.flue"),
                    Component.literal(furnace.tanks[1].getFill() + " / " + furnace.tanks[1].getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(62, 18, 56, 88, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(String.format(Locale.US,
                    "%.0f%%", furnace.progress * 100F))), Optional.empty(), mouseX, mouseY);
        }
    }
}
