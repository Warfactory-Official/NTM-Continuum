package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineRTG;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineRTG;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** The exact 176x176 grey RTG inventory and its heat/power gauges. */
public final class GUIMachineRTG extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineRTG> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_rtg.png");

    public GUIMachineRTG(ContainerMachineRTG menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 176);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineRTG rtg = menu.getRtg();
        if (rtg != null && rtg.hasHeat()) {
            int height = rtg.getHeatScaled(RtgGuiLayout.GAUGE_HEIGHT);
            GuiDraw.blit(graphics, TEXTURE, leftPos + RtgGuiLayout.HEAT_X,
                    topPos + RtgGuiLayout.GAUGE_Y + RtgGuiLayout.GAUGE_HEIGHT - height,
                    176F, RtgGuiLayout.GAUGE_HEIGHT - height,
                    RtgGuiLayout.GAUGE_WIDTH, height, 256, 256);
        }
        if (rtg != null && rtg.hasPower()) {
            int height = (int) rtg.getPowerScaled(RtgGuiLayout.GAUGE_HEIGHT);
            GuiDraw.blit(graphics, TEXTURE, leftPos + RtgGuiLayout.POWER_X,
                    topPos + RtgGuiLayout.GAUGE_Y + RtgGuiLayout.GAUGE_HEIGHT - height,
                    192F, RtgGuiLayout.GAUGE_HEIGHT - height,
                    RtgGuiLayout.GAUGE_WIDTH, height, 256, 256);
        }
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 36, 2);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineRTG rtg = menu.getRtg();
        if (rtg == null) return;
        if (isHovering(RtgGuiLayout.POWER_X, RtgGuiLayout.GAUGE_Y,
                RtgGuiLayout.GAUGE_WIDTH, RtgGuiLayout.GAUGE_HEIGHT, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(rtg.power, TileEntityMachineRTG.MAX_POWER),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(RtgGuiLayout.HEAT_X, RtgGuiLayout.GAUGE_Y,
                RtgGuiLayout.GAUGE_WIDTH, RtgGuiLayout.GAUGE_HEIGHT, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("RTG Heat " + rtg.heat + "/" + TileEntityMachineRTG.heatMax()),
                    Component.literal("RTG Power " + Library.getShortNumber(rtg.heat * 100L) + "HE/s")),
                    Optional.empty(), mouseX, mouseY);
        } else if (mouseX >= leftPos - 16 && mouseX < leftPos && mouseY >= topPos + 36 && mouseY < topPos + 52) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("Heat to Power Conversion 1:5")),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
