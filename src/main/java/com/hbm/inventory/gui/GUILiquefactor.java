package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerLiquefactor;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the liquefactor, 1.7.10 {@code GUILiquefactor} ({@code processing/gui_liquefactor.png}, 176×204):
 * power bar 16×52 at (134,18) (fill u=176), progress arrow up to 42×35 at (42,17) (u=192), the
 * "powered" LED 9×12 at (138,4) (u=176,v=52), tank 16×52 at x=71 (bottom y=88).
 */
public class GUILiquefactor extends CompatContainerScreen<ContainerLiquefactor> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_liquefactor.png");

    public GUILiquefactor(ContainerLiquefactor menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    /** 1.7: title centred on x=70 at y=6 in 0xC7C1A3; inventory at (8, ySize-96+2) in 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        GuiDraw.text(graphics, this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 0xC7C1A3, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineLiquefactor te = this.menu.getLiquefactor();
        if (te == null) return;

        int i = (int) (te.getPower() * 52 / te.getMaxPower());
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 134, this.topPos + 70 - i, 176.0F, (float) (52 - i), 16, i, 256, 256);

        int j = te.processTime > 0 ? te.progress * 42 / te.processTime : 0;
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 42, this.topPos + 17, 192.0F, 0.0F, j, 35, 256, 256);

        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 138, this.topPos + 4, 176.0F, 52.0F, 9, 12, 256, 256);

        GuiUtil.tank(graphics, te.tank, this.leftPos + 71, this.topPos + 88, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineLiquefactor te = this.menu.getLiquefactor();
        if (te == null) return;

        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 71, this.topPos + 36, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 134, this.topPos + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.getPower(), te.getMaxPower()), mouseX, mouseY);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getLiquefactor();
    }
}
