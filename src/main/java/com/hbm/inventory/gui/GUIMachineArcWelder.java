package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineArcWelder;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.machine.TileEntityMachineArcWelder;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;


/** Arc Welder screen, the 1.7.10 {@code GUIMachineArcWelder} ({@code processing/gui_arc_welder.png}, 176x204). */
public class GUIMachineArcWelder extends CompatContainerScreen<ContainerMachineArcWelder>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm",
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

        // 1.7: welder.tank.renderTank(guiLeft + 35, guiTop + 79, z, 34, 16, 1). Horizontal, y is the bottom edge.
        GuiUtil.tankHorizontal(graphics, welder.tank, leftPos + FLUID_X, topPos + FLUID_TOP + FLUID_H, FLUID_W, FLUID_H);
    }

    /** 1.7 {@code drawScreen}: tank info, energy info, then the upgrade list anchored at the panel. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineArcWelder welder = menu.getWelder();
        if (welder == null) return;
        if (GuiUtil.hover(mouseX, mouseY, leftPos + FLUID_X, topPos + FLUID_TOP, FLUID_W, FLUID_H)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(welder.tank), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 152, topPos + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(welder.power, welder.maxPower), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + INFO_X, topPos + INFO_Y, 8, 8)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.upgradeInfo(welder), leftPos + INFO_X, topPos + INFO_Y);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2 - 18,
                6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getWelder();
    }
}
