package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerCrystallizer;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Client screen for the ore acidizer: 1.7.10 {@code GUICrystallizer} ({@code processing/gui_crystallizer_alt.png},
 * 176×204). Power bar (152,18) 16×52 from (176, 64−i); progress arrow (80,47) 28×12 from (176,0); acid tank
 * (35, bottom 70) 16×52; upgrade info panel at (117,22) with the fixed 1.7 {@code desc.gui.upgrade*} text.
 */
public class GUICrystallizer extends CompatContainerScreen<ContainerCrystallizer> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_crystallizer_alt.png");

    public GUICrystallizer(ContainerCrystallizer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    /** 1.7: title centred on x=70 (not the panel centre), inventory at (8, ySize-96+2), both 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCrystallizer te = this.menu.getCrystallizer();
        if (te == null) return;

        int i = (int) (te.power * 52 / TileEntityMachineCrystallizer.MAX_POWER);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 70 - i, 176.0F, (float) (64 - i), 16, i, 256, 256);

        int j = te.progress * 28 / Math.max(te.duration, 1);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 80, this.topPos + 47, 176.0F, 0.0F, j, 12, 256, 256);

        GuiUtil.drawInfoPanel(graphics, this.leftPos + 117, this.topPos + 22, 8);

        GuiUtil.tank(graphics, te.tank, this.leftPos + 35, this.topPos + 70, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCrystallizer te = this.menu.getCrystallizer();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;
        if (GuiUtil.hover(mouseX, mouseY, gl + 152, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineCrystallizer.MAX_POWER), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 35, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 117, gt + 22, 8, 8)) {
            // 1.7: a fixed four-line text, drawn at (guiLeft + 200, guiTop + 45) rather than at the mouse.
            GuiUtil.tooltip(graphics, this.font, List.of(
                    Component.translatable("desc.gui.upgrade"),
                    Component.translatable("desc.gui.upgrade.speed"),
                    Component.translatable("desc.gui.upgrade.effectiveness"),
                    Component.translatable("desc.gui.upgrade.overdrive")), gl + 200, gt + 45);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getCrystallizer();
    }
}
