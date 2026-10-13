package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerCompressor;
import com.hbm.tileentity.machine.TileEntityMachineCompressorBase;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the compressor: 1.7.10 {@code GUICompressor} ({@code processing/gui_compressor.png}, 176×204 on a
 * 256×256 texture). Tanks at x=17/107 (bottom y=70); power bar x=152; progress bar (42,26) 55 wide ×17;
 * the 5-way compression selector at (43 + j·11, 46) 8×14, and its highlight blits from (193,18).
 * The 1.7 title is drawn centred on <b>x=70</b> (not the panel centre) at y=6 in 0xC7C1A3.
 */
public class GUICompressor extends CompatContainerScreen<ContainerCompressor> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_compressor.png");

    public GUICompressor(ContainerCompressor menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    /** 1.7 {@code drawGuiContainerForegroundLayer}. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 0xC7C1A3, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCompressorBase te = this.menu.getTile();
        if (te == null) return;

        if (te.power >= te.powerRequirement) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 156, this.topPos + 4, 176.0F, 52.0F, 9, 12, 256, 256);
        }

        // 1.7 quirk: the selector highlight is blitted 8×124 (only the top 14 rows are the highlight sprite).
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 43 + te.tanks[0].getPressure() * 11, this.topPos + 46, 193.0F, 18.0F, 8, 124, 256, 256);

        int i = te.processTime <= 0 ? 0 : te.progress * 55 / te.processTime;
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 42, this.topPos + 26, 192.0F, 0.0F, i, 17, 256, 256);

        int j = (int) (te.power * 52 / TileEntityMachineCompressorBase.maxPower);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 70 - j, 176.0F, (float) (52 - j), 16, j, 256, 256);

        GuiUtil.tank(graphics, te.tanks[0], this.leftPos + 17, this.topPos + 70, 16, 52);
        GuiUtil.tank(graphics, te.tanks[1], this.leftPos + 107, this.topPos + 70, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCompressorBase te = this.menu.getTile();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;

        if (GuiUtil.hover(mouseX, mouseY, gl + 17, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[0]), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 107, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[1]), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 152, gt + 18, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineCompressorBase.maxPower), mouseX, mouseY);
            return;
        }
        for (int j = 0; j < 5; j++) {
            if (GuiUtil.hover(mouseX, mouseY, gl + 43 + j * 11, gt + 46, 8, 14)) {
                GuiUtil.tooltip(graphics, this.font, GuiUtil.lines(j + " PU -> " + (j + 1) + " PU"), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        double mx = event.x(), my = event.y();

        // 1.7 bounds: x [43+j*11, 43+8+j*11), y (46, 46+14].
        for (int j = 0; j < 5; j++) {
            if (mc.gameMode != null && GuiUtil.hover(mx, my, this.leftPos + 43 + j * 11, this.topPos + 46, 8, 14)) {
                GuiUtil.playClickSound();
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, j);
                return true;
            }
        }
        return false;
    }
}
