package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineArcWelder;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Arc Welder screen with the exact legacy 176x204 layout and gauges. */
public class GUIMachineArcWelder extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineArcWelder>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm",
            "textures/gui/processing/gui_arc_welder.png");
    private static final int FLUID_X = 35, FLUID_TOP = 63, FLUID_W = 34, FLUID_H = 16;
    private static final int INFO_X = 78, INFO_Y = 67;

    public GUIMachineArcWelder(ContainerMachineArcWelder menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineArcWelder welder = menu.getWelder();
        if (welder == null) return;

        int power = (int) (welder.power * 52L / Math.max(welder.maxPower, 1L));
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 70 - power,
                    176F, 52F - power, 16, power, 256, 256);
        }

        int progress = welder.progress * 33 / Math.max(welder.processTime, 1);
        if (progress > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 72, topPos + 37,
                    192F, 0F, progress, 14, 256, 256);
        }

        if (welder.power >= welder.consumption) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 156, topPos + 4,
                    176F, 52F, 9, 12, 256, 256);
        }

        GuiUtil.drawUpgradeInfoPanel(graphics, leftPos + INFO_X, topPos + INFO_Y);

        FluidTankNTM tank = welder.tank;
        // FluidTankNTM.renderTank(..., orientation=1) fills this gauge from left to right. Its y argument
        // is the bottom edge, so the legacy call at (35, 79) maps to the rectangle (35, 63)-(69, 79).
        int width = tank.getMaxFill() <= 0 ? 0
                : (int) ((long) tank.getFill() * FLUID_W / tank.getMaxFill());
        if (width > 0) {
            int color = 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF);
            graphics.fill(leftPos + FLUID_X, topPos + FLUID_TOP,
                    leftPos + FLUID_X + width, topPos + FLUID_TOP + FLUID_H, color);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineArcWelder welder = menu.getWelder();
        if (welder == null) return;
        if (isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(welder.power, welder.maxPower),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(FLUID_X, FLUID_TOP, FLUID_W, FLUID_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    welder.tank.getTankType().getDisplayName(),
                    Component.literal(welder.tank.getFill() + " / " + welder.tank.getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(INFO_X, INFO_Y, GuiUtil.INFO_PANEL_SIZE, GuiUtil.INFO_PANEL_SIZE, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.upgradeInfo(welder), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineArcWelder welder = menu.getWelder();
        Component label = welder != null && welder.hasCustomName()
                ? Component.literal(welder.getName()) : title;
        GuiDraw.text(graphics, font, label, imageWidth / 2 - font.width(label) / 2 - 18,
                6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getWelder();
    }
}
