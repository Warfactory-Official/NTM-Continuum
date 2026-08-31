package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineCyclotron;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/** Cyclotron screen preserving the legacy 190×215 layout and three fluid gauges. */
public class GUIMachineCyclotron extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineCyclotron>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_cyclotron.png");

    public GUIMachineCyclotron(ContainerMachineCyclotron menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 190, 215);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityMachineCyclotron cyclotron = menu.getCyclotron();
        if (cyclotron == null) return;

        int power = (int) cyclotron.getPowerScaled(63);
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 168, topPos + 80 - power,
                    190F, 62F - power, 16, power, 256, 256);
        }

        int progress = cyclotron.getProgressScaled(34);
        if (progress > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 48, topPos + 27,
                    206F, 0F, progress, 34, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 172, topPos + 4,
                    190F, 63F, 9, 12, 256, 256);
        }

        GuiUtil.drawUpgradeInfoPanel(graphics, leftPos + 49, topPos + 85);
        drawTank(graphics, cyclotron.tanks[0], 11, 88, 34, 7);
        drawTank(graphics, cyclotron.tanks[1], 11, 97, 34, 7);
        drawTank(graphics, cyclotron.tanks[2], 107, 97, 34, 16);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank,
                          int x, int y, int width, int height) {
        int fill = tank.getMaxFill() <= 0 ? 0 : tank.getFill() * height / tank.getMaxFill();
        if (fill <= 0) return;
        int drawX = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - fill;
        Identifier fluidTexture = tank.getTankType().getTexture();
        int tint = 0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF);
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(drawX, top, drawX + width, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, drawX, tileY, 0F, 0F, width, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(drawX, top, drawX + width, bottom, tint);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 15, imageHeight - 94, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCyclotron cyclotron = menu.getCyclotron();
        if (cyclotron == null) return;

        if (isHovering(168, 18, 16, 63, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    GuiUtil.energyTooltip(cyclotron.power, TileEntityMachineCyclotron.MAX_POWER),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(11, 81, 34, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(cyclotron.tanks[0]),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(11, 90, 34, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(cyclotron.tanks[1]),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(107, 81, 34, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(cyclotron.tanks[2]),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(49, 85, GuiUtil.INFO_PANEL_SIZE, GuiUtil.INFO_PANEL_SIZE, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.upgradeInfo(cyclotron),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getCyclotron();
    }
}
