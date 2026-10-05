package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineCoker;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Screen for the coker — 1.7 {@code GUIMachineCoker} ({@code processing/gui_coker.png}, 176×204 on a 256×256
 * texture). Progress bar (61,46) src(176,0), heat bar (61,55) src(176,5); tank bars at x=35/125 (bottom y=70). */
public class GUIMachineCoker extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineCoker> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_coker.png");

    public GUIMachineCoker(ContainerMachineCoker menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    /** 1.7 {@code GUIMachineCoker.drawGuiContainerForegroundLayer}: centred title at y 6, "Inventory" at (8, ySize - 94). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xC7C1A3, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCoker te = this.menu.getTile();
        if (te == null) return;

        int p = te.getProgressScaled(53);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 61, this.topPos + 46, 176.0F, 0.0F, p, 5, 256, 256);
        int h = te.getHeatScaled(52);
        if (h > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 61, this.topPos + 55, 176.0F, 5.0F, h, 5, 256, 256);

        // 1.7: tanks[i].renderTank(guiLeft + 35 / 125, guiTop + 70, z, 16, 52) — tinted, bottom-anchored.
        Production2Gui.drawTank(graphics, te.tanks[0], this.leftPos + 35, this.topPos + 70, 16, 52);
        Production2Gui.drawTank(graphics, te.tanks[1], this.leftPos + 125, this.topPos + 70, 16, 52);
    }

    /** 1.7 {@code GUIMachineCoker.drawScreen}: both tank readouts, then progress and heat stats ("TU" unspaced, as 1.7). */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCoker te = this.menu.getTile();
        if (te == null) return;
        if (in17(35, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[0]), Optional.empty(), mouseX, mouseY);
        } else if (in17(125, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[1]), Optional.empty(), mouseX, mouseY);
        } else if (in17(60, 45, 54, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal(String.format(Locale.US, "%,d", te.progress)
                    + " / " + String.format(Locale.US, "%,d", te.processTime) + "TU")), Optional.empty(), mouseX, mouseY);
        } else if (in17(60, 54, 54, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal(String.format(Locale.US, "%,d", te.heat)
                    + " / " + String.format(Locale.US, "%,d", TileEntityMachineCoker.maxHeat) + "TU")), Optional.empty(), mouseX, mouseY);
        }
    }

    /** The 1.7 GUI hit test ({@code drawCustomInfoStat}/{@code renderTankInfo}): x <= mouse < x + w, y < mouse <= y + h. */
    private boolean in17(int x, int y, int w, int h, int mouseX, int mouseY) {
        int mx = mouseX - this.leftPos, my = mouseY - this.topPos;
        return x <= mx && x + w > mx && y < my && y + h >= my;
    }
}
