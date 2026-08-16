package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineElectricFurnace;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Client screen for the electric furnace. Reads {@code power}/{@code progress}/{@code canProgress} off
 * the synced client {@link TileEntityMachineElectricFurnace} (BufPacket). Energy bar (left) + a fire
 * indicator (lit while it can smelt) + the smelt-progress arrow, with an energy-bar hover tooltip.
 */
public class GUIMachineElectricFurnace extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineElectricFurnace> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/guielectricfurnace.png");

    public GUIMachineElectricFurnace(ContainerMachineElectricFurnace menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineElectricFurnace furnace = this.menu.getFurnace();
        if (furnace == null) return;

        if (furnace.hasPower()) {
            int i = (int) furnace.getPowerScaled(52);
            if (i > 0) {
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + 20, this.topPos + 69 - i, 200.0F, (float) (52 - i), 16, i, 256, 256);
            }
        }
        if (furnace.canProgress) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 56, this.topPos + 35, 176.0F, 0.0F, 16, 16, 256, 256);
        }
        int j = furnace.getProgressScaled(24);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 34, 176.0F, 17.0F, j + 1, 17, 256, 256);

        // The "Acceptable Upgrades" info-panel glyph (1.12.2 drawInfoPanel(151, 19, …, 8)).
        GuiUtil.drawUpgradeInfoPanel(graphics, this.leftPos + INFO_X, this.topPos + INFO_Y);
    }

    // "Acceptable Upgrades" info-panel glyph (1.12.2 drawInfoPanel at 151, 19).
    private static final int INFO_X = 151, INFO_Y = 19;

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineElectricFurnace furnace = this.menu.getFurnace();
        if (furnace == null) return;
        if (this.isHovering(20, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(furnace.power, TileEntityMachineElectricFurnace.maxPower),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(INFO_X, INFO_Y, GuiUtil.INFO_PANEL_SIZE, GuiUtil.INFO_PANEL_SIZE, mouseX, mouseY)) {
            // Info-panel glyph hover: the "Acceptable Upgrades" panel (1.12.2 getUpgradeInfo).
            GuiDraw.tooltip(graphics, this.font, GuiUtil.upgradeInfo(furnace), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getFurnace();
    }
}
