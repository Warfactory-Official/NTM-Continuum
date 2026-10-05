package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineGiantTank;
import com.hbm.tileentity.machine.TileEntityMachineGiantTank;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/** The 1.7 {@code GUIBarrel} (176x166), shared by both giant storage machines (1.7 BAT9000/Orbus extend TileEntityBarrel). */
public class GUIMachineGiantTank extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineGiantTank> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_barrel.png");
    private static final int TANK_X = 71, TANK_TOP = 17, TANK_WIDTH = 34, TANK_HEIGHT = 52;
    private static final int MODE_X = 151, MODE_Y = 34, MODE_SIZE = 18;

    public GUIMachineGiantTank(ContainerMachineGiantTank menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineGiantTank tank = menu.getTank();
        if (tank == null) return;

        // 1.7 GUIBarrel order: mode icon, then the tinted tank column with its bottom at (71, 69).
        int mode = Math.max(0, Math.min(3, tank.mode));
        GuiDraw.blit(graphics, TEXTURE, leftPos + MODE_X, topPos + MODE_Y,
                176F, mode * 18F, MODE_SIZE, MODE_SIZE, 256, 256);
        Production2Gui.drawTank(graphics, tank.tankNew, leftPos + TANK_X, topPos + TANK_TOP + TANK_HEIGHT, TANK_WIDTH, TANK_HEIGHT);
    }

    /** 1.7 {@code GUIBarrel.drawGuiContainerForegroundLayer}: centred title at y 6, "Inventory" at (8, ySize - 94). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    /** 1.7 GUIBarrel draws only the tank tooltip; the mode button has none. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineGiantTank tank = menu.getTank();
        if (tank == null) return;
        if (in17(TANK_X, TANK_TOP, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(tank.tankNew), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        // 1.7 GUIBarrel hit box: guiLeft + 151 <= x < guiLeft + 169, guiTop + 35 < y <= guiTop + 53.
        if (in17(MODE_X, MODE_Y + 1, MODE_SIZE, MODE_SIZE, event.x(), event.y())) {
            GuiUtil.playClickSound();
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
        }
        return false;
    }

    /** The 1.7 GUI hit test ({@code renderTankInfo}): x <= mouse < x + w, y < mouse <= y + h. */
    private boolean in17(int x, int y, int w, int h, double mouseX, double mouseY) {
        double mx = mouseX - leftPos, my = mouseY - topPos;
        return x <= mx && x + w > mx && y < my && y + h >= my;
    }
}
