package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * The 1.7 {@code GUIMachineFluidTank}: {@code gui_tank.png} (176x166), the tinted fluid column 34x52 with its
 * bottom at (71, 69), and the I/O-mode button at (151, 34). Reads the live client
 * {@link TileEntityMachineFluidTank} for fill/type/mode; the mode button dispatches through the vanilla
 * menu-button path.
 */
public class GUIMachineFluidTank extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineFluidTank> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_tank.png");

    // Fill bar geometry (GUI-relative), matching the 1.12.2 layout: 34×52 at (71, 17)..(71, 69).
    private static final int BAR_X = 71, BAR_TOP = 17, BAR_W = 34, BAR_H = 52, BAR_BOTTOM = 69;
    private static final int BTN_X = 151, BTN_Y = 34, BTN_W = 18, BTN_H = 18;

    public GUIMachineFluidTank(ContainerMachineFluidTank menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineFluidTank tank = this.menu.getTank();
        if (tank == null) return;
        // 1.7 order: mode icon (strip at u=176, v=mode*18), then tank.renderTank(guiLeft + 71, guiTop + 69, z, 34, 52)
        // — the fluid texture tinted with the fluid's tint, bottom-anchored.
        int mode = tank.mode;
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + BTN_X, this.topPos + BTN_Y, 176.0F, (float) (mode * 18), BTN_W, BTN_H, 256, 256);
        Production2Gui.drawTank(graphics, tank.tank, this.leftPos + BAR_X, this.topPos + BAR_BOTTOM, BAR_W, BAR_H);
    }

    /** 1.7 {@code GUIMachineFluidTank.drawGuiContainerForegroundLayer}: centred title at y 6, "Inventory" at (8, ySize - 94). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    /** 1.7 draws only the tank tooltip ({@code renderTankInfo}); the mode button has none. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineFluidTank tank = this.menu.getTank();
        if (tank == null) return;
        if (in17(BAR_X, BAR_TOP, BAR_W, BAR_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.tankTooltip(tank.tank), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        // 1.7 hit box: guiLeft + 151 <= x < guiLeft + 169, guiTop + 35 < y <= guiTop + 53; 1.7 plays the click sound.
        if (in17(BTN_X, BTN_Y + 1, BTN_W, BTN_H, event.x(), event.y())) {
            GuiUtil.playClickSound();
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        }
        return false;
    }

    /** The 1.7 GUI hit test ({@code drawCustomInfoStat}/{@code renderTankInfo}): x <= mouse < x + w, y < mouse <= y + h. */
    private boolean in17(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - this.leftPos, my = mouseY - this.topPos;
        return x <= mx && x + w > mx && y < my && y + h >= my;
    }
}
