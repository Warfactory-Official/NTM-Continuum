package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerOilburner;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Screen for the oil-fired heater — 1.12.2 {@code GUIOilburner} ({@code gui_oilburner.png}, 176×203):
 * heat bar 16×52 at (116,17) (fill u=194), tank bar 16×52 at (44,17), burn button 35×14 at (70,54)
 * (lit overlay u=210 while on), flame icon 18×18 at (79,34) (u=176) while burning. Setting readout on
 * the flame icon hover; left/right-click the flame area to raise/lower the mB/t setting (old
 * screwdriver IToolable — documented GUI stand-in).
 */
public class GUIOilburner extends dev.golem.core.gui.CompatContainerScreen<ContainerOilburner> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_oilburner.png");

    public GUIOilburner(ContainerOilburner menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2; // old GUI centred the name
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityHeaterOilburner te = this.menu.getHeater();
        if (te == null) return;

        int i = Math.min(te.heatEnergy, TileEntityHeaterOilburner.maxHeatEnergy) * 52 / TileEntityHeaterOilburner.maxHeatEnergy;
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 116, this.topPos + 69 - i, 194.0F, (float) (52 - i), 16, i, 256, 256);

        if (te.isOn) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 70, this.topPos + 54, 210.0F, 0.0F, 35, 14, 256, 256);

            if (te.tank.getFill() > 0 && FluidCombustion.isFlammable(te.tank.getTankType())) {
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 34, 176.0F, 0.0F, 18, 18, 256, 256);
            }
        }

        drawTank(graphics, te.tank, 44, 17, 16, 52);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop, int w, int h) {
        int maxF = tank.getMaxFill();
        int fh = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * h / maxF);
        if (fh <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + h;
        int yTop = yBottom - fh;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + w, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, w, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + w, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityHeaterOilburner te = this.menu.getHeater();
        if (te == null) return;

        if (this.isHovering(116, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal(
                    String.format("%,d", Math.min(te.heatEnergy, TileEntityHeaterOilburner.maxHeatEnergy))
                            + " / " + String.format("%,d", TileEntityHeaterOilburner.maxHeatEnergy) + " TU")), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(79, 34, 18, 18, mouseX, mouseY)) {
            long perBucket = FluidCombustion.getFlammableEnergy(te.tank.getTankType());
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal(te.setting + " mB/t"),
                    Component.literal(String.format(Locale.US, "%,d", (int) (perBucket / 1000) * te.setting) + " TU/t"),
                    Component.literal("LMB +1, RMB -1")), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(44, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal(te.tank.getTankType().getName()),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            // burn toggle (old 16×14 hit area at (80,54))
            if (this.isHovering(80, 54, 16, 14, event.x(), event.y())) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
            // setting up/down on the flame area (old screwdriver toggle)
            if (this.isHovering(79, 34, 18, 18, event.x(), event.y())) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, event.button() == 1 ? 2 : 1);
                return true;
            }
        }
        return false;
    }
}
