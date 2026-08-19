package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineSolderingStation;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineSolderingStation;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the soldering station. Reads {@code power}/{@code progress} off the synced client
 * {@link TileEntityMachineSolderingStation} (BufPacket, not ContainerData): an energy bar (right), a
 * progress bar, and a "powered" indicator, with an energy-bar hover tooltip. (The fluid-level bar is
 * deferred — the tank still works; recipes needing a fluid just show no gauge yet.)
 */
public class GUIMachineSolderingStation extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineSolderingStation> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_soldering_station.png");
    // Fluid gauge (GUI-relative), matching the 1.12.2 layout: a 34×16 area at (35, 63).
    private static final int FLUID_X = 35, FLUID_TOP = 63, FLUID_W = 34, FLUID_H = 16;
    // "Acceptable Upgrades" info-panel glyph (1.12.2 drawInfoPanel at 78, 67).
    private static final int INFO_X = 78, INFO_Y = 67;

    public GUIMachineSolderingStation(ContainerMachineSolderingStation menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineSolderingStation te = this.menu.getStation();
        if (te == null) return;

        int p = (int) (te.power * 52 / Math.max(te.maxPower, 1));
        if (p > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 70 - p, 176.0F, (float) (52 - p), 16, p, 256, 256);
        }
        int i = te.progress * 33 / Math.max(te.processTime, 1);
        if (i > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 72, this.topPos + 28, 192.0F, 0.0F, i, 14, 256, 256);
        }
        if (te.power >= te.consumption) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 156, this.topPos + 4, 176.0F, 52.0F, 9, 12, 256, 256);
        }

        // Fluid fill bar — a solid-colour level (the fluid's colour), bottom-anchored in the 34×16 gauge at
        // (35, 63)..(35, 79), matching the 1.12.2 layout. (Solid colour, like the fluid tank's fallback.)
        FluidTankNTM ft = te.tank;
        int max = ft.getMaxFill();
        int fh = max <= 0 ? 0 : (int) ((long) ft.getFill() * FLUID_H / max);
        if (fh > 0) {
            int x0 = this.leftPos + FLUID_X;
            int yBottom = this.topPos + FLUID_TOP + FLUID_H;
            graphics.fill(x0, yBottom - fh, x0 + FLUID_W, yBottom, 0xFF000000 | (ft.getTankType().getColor() & 0xFFFFFF));
        }

        // The "Acceptable Upgrades" info-panel glyph (1.12.2 drawInfoPanel(78, 67, …, 8)).
        GuiUtil.drawUpgradeInfoPanel(graphics, this.leftPos + INFO_X, this.topPos + INFO_Y);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineSolderingStation te = this.menu.getStation();
        if (te == null) return;
        if (this.isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.maxPower), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(FLUID_X, FLUID_TOP, FLUID_W, FLUID_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    te.tank.getTankType().getDisplayName(),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(INFO_X, INFO_Y, GuiUtil.INFO_PANEL_SIZE, GuiUtil.INFO_PANEL_SIZE, mouseX, mouseY)) {
            // Info-panel glyph hover: the "Acceptable Upgrades" panel (1.12.2 getUpgradeInfo).
            GuiDraw.tooltip(graphics, this.font, GuiUtil.upgradeInfo(te), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getStation();
    }
}
