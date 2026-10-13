package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.tileentity.machine.oil.TileEntityOilDrillBase;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Screen shared by the oil derrick, pumpjack and fracking tower, a straight port of 1.7 {@code GUIMachineOilWell}
 * ({@code machine/gui_well.png}, 184x190): power bar 16x34 at (8,22), indicator lamp 14x14 at (50,19), the cover
 * plate over the third-tank window when there is no third tank, tanks 16x52 at (76,22)/(112,22), the fracking
 * solution tank 6x32 at (54,45), and the upgrade info glyph at (160,21).
 */
public class GUIMachineOilWell extends CompatContainerScreen<ContainerMachineOilWell> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_well.png");

    public GUIMachineOilWell(ContainerMachineOilWell menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 184, 190);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int guiLeft = this.leftPos, guiTop = this.topPos;
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityOilDrillBase derrick = this.menu.getDrill();
        if (derrick == null) return;

        int i = derrick.getMaxPower() <= 0 ? 0 : (int) (derrick.power * 34 / derrick.getMaxPower());
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, guiLeft + 8, guiTop + 56 - i, 184.0F, (float) (34 - i), 16, i, 256, 256);

        int k = derrick.indicator;
        if (k != 0) GuiDraw.blit(graphics, TEXTURE, guiLeft + 50, guiTop + 19, (float) (184 + (k - 1) * 14), 34.0F, 14, 14, 256, 256);

        if (derrick.tanks.length < 3) {
            GuiDraw.blit(graphics, TEXTURE, guiLeft + 48, guiTop + 44, 200.0F, 0.0F, 18, 34, 256, 256);
        }

        GuiUtil.tank(graphics, derrick.tanks[0], guiLeft + 76, guiTop + 74, 16, 52);
        GuiUtil.tank(graphics, derrick.tanks[1], guiLeft + 112, guiTop + 74, 16, 52);
        if (derrick.tanks.length > 2) {
            GuiUtil.tank(graphics, derrick.tanks[2], guiLeft + 54, guiTop + 77, 6, 32);
        }

        GuiUtil.drawInfoPanel(graphics, guiLeft + 160, guiTop + 21, 8);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7: title centred on x=126 at y=10; "Inventory" at x=12.
        GuiDraw.text(graphics, this.font, this.title, 126 - this.font.width(this.title) / 2, 10, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 12, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityOilDrillBase derrick = this.menu.getDrill();
        if (derrick == null) return;
        int guiLeft = this.leftPos, guiTop = this.topPos;

        if (GuiUtil.hover(mouseX, mouseY, guiLeft + 76, guiTop + 74 - 52, 16, 52)) { GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(derrick.tanks[0]), mouseX, mouseY); return; }
        if (GuiUtil.hover(mouseX, mouseY, guiLeft + 112, guiTop + 74 - 52, 16, 52)) { GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(derrick.tanks[1]), mouseX, mouseY); return; }
        if (derrick.tanks.length >= 3 && GuiUtil.hover(mouseX, mouseY, guiLeft + 54, guiTop + 45, 6, 32)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(derrick.tanks[2]), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, guiLeft + 160, guiTop + 21, 8, 8)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.upgradeInfo(derrick), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, guiLeft + 8, guiTop + 22, 16, 34)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(derrick.power, derrick.getMaxPower()), mouseX, mouseY);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getDrill();
    }
}
