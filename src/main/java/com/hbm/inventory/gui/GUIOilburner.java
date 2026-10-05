package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerOilburner;
import com.hbm.inventory.fluid.FluidCombustion;
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
 * Screen for the oil-fired heater — 1.7.10 {@code GUIOilburner} ({@code machine/gui_oilburner.png}, 176×203):
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

    /** 1.7: centred title at y=6, inventory at (8, ySize-96+2), both 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
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

        GuiUtil.tank(graphics, te.tank, this.leftPos + 44, this.topPos + 69, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityHeaterOilburner te = this.menu.getHeater();
        if (te == null) return;

        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 116, this.topPos + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines(
                    String.format(Locale.US, "%,d", Math.min(te.heatEnergy, TileEntityHeaterOilburner.maxHeatEnergy))
                            + " / " + String.format(Locale.US, "%,d", TileEntityHeaterOilburner.maxHeatEnergy) + " TU"), mouseX, mouseY);
            return;
        }
        // 1.7 shows the setting readout only for an FT_Flammable fluid. The third line is the port's stand-in for
        // 1.7's screwdriver (IToolable is not ported), so the setting stays reachable.
        if (FluidCombustion.isFlammable(te.tank.getTankType()) && GuiUtil.hover(mouseX, mouseY, this.leftPos + 79, this.topPos + 34, 18, 18)) {
            long perBucket = FluidCombustion.getFlammableEnergy(te.tank.getTankType());
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal(te.setting + " mB/t"),
                    Component.literal(String.format(Locale.US, "%,d", (int) (perBucket / 1000) * te.setting) + " TU/t"),
                    Component.literal("LMB +1, RMB -1")), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 44, this.topPos + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            // burn toggle — 1.7 hit area x [80,96), y (54,68]
            if (GuiUtil.hover(event.x(), event.y(), this.leftPos + 80, this.topPos + 54, 16, 14)) {
                GuiUtil.playClickSound();
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
