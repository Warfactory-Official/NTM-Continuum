package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineCyclotron;
import com.hbm.tileentity.machine.TileEntityMachineCyclotron;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/** Cyclotron screen, 1.7 {@code GUIMachineCyclotron} ({@code machine/gui_cyclotron.png}, 190x215). */
public class GUIMachineCyclotron extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineCyclotron>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_cyclotron.png");

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

        GuiUtil.drawInfoPanel(graphics, leftPos + 49, topPos + 85, 8);
        // 1.7 renderTank(x, yBottom, z, w, h, orientation 1): horizontal gauges filling from the left.
        GuiUtil.tankHorizontal(graphics, cyclotron.tanks[0], leftPos + 11, topPos + 88, 34, 7);
        GuiUtil.tankHorizontal(graphics, cyclotron.tanks[1], leftPos + 11, topPos + 97, 34, 7);
        GuiUtil.tankHorizontal(graphics, cyclotron.tanks[2], leftPos + 107, topPos + 97, 34, 16);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7: title centred on x=79, "Inventory" at x=15.
        GuiDraw.text(graphics, font, title, 79 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, Component.translatable("container.inventory"), 15, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCyclotron cyclotron = menu.getCyclotron();
        if (cyclotron == null) return;

        if (GuiUtil.hover(mouseX, mouseY, leftPos + 168, topPos + 18, 16, 63)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(cyclotron.power, TileEntityMachineCyclotron.MAX_POWER), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 11, topPos + 81, 34, 7)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(cyclotron.tanks[0]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 11, topPos + 90, 34, 7)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(cyclotron.tanks[1]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 107, topPos + 81, 34, 16)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(cyclotron.tanks[2]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 49, topPos + 85, 8, 8)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.upgradeInfo(cyclotron), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getCyclotron();
    }
}
