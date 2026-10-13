package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerPyroOven;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Screen for the pyro oven, the 1.7.10 {@code GUIPyroOven} ({@code processing/gui_pyrooven.png}, 176×204 on 256×256).
 * Power bar x=152 (src u=176 v=64−i), progress arrow (57,47) src(176,0) 27 wide; tanks x=8/116 (bottom y=70);
 * upgrade info panel at (108,76). */
public class GUIPyroOven extends CompatContainerScreen<ContainerPyroOven> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_pyrooven.png");

    public GUIPyroOven(ContainerPyroOven menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    /** 1.7: title centred 18 px left of the panel centre, inventory at (8, ySize-96+2), both 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2 - 18, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachinePyroOven te = this.menu.getTile();
        if (te == null) return;

        int i = (int) (te.power * 52 / TileEntityMachinePyroOven.maxPower);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 70 - i, 176.0F, (float) (64 - i), 16, i, 256, 256);

        int p = (int) (te.progress * 27);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 57, this.topPos + 47, 176.0F, 0.0F, p, 12, 256, 256);

        GuiUtil.tank(graphics, te.tanks[0], this.leftPos + 8, this.topPos + 70, 16, 52);
        GuiUtil.tank(graphics, te.tanks[1], this.leftPos + 116, this.topPos + 70, 16, 52);

        GuiUtil.drawInfoPanel(graphics, this.leftPos + 108, this.topPos + 76, 8);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachinePyroOven te = this.menu.getTile();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;

        if (GuiUtil.hover(mouseX, mouseY, gl + 8, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[0]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 116, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[1]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 152, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachinePyroOven.maxPower), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 108, gt + 76, 8, 8)) {
            // 1.7 draws this list anchored at the panel (guiLeft + 108, guiTop + 76), not at the mouse.
            GuiUtil.tooltip(graphics, this.font, GuiUtil.upgradeInfo(te), gl + 108, gt + 76);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getTile();
    }
}
