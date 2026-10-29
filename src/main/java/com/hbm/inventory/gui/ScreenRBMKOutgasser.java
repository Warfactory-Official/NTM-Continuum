package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKOutgasser;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The 1.7 {@code GUIRBMKOutgasser}: {@code reactors/gui_rbmk_outgasser.png} (176x186), a 13-wide progress arrow at
 * (82, 50), the 10x42 gas gauge with its bottom at (115, 66), and the tank tooltip over (112, 21, 16, 48): the
 * 1.7 hover box is larger than the gauge it describes; kept as 1.7 had it. Labels come from
 * {@link RBMKContainerScreen#extractLabels}, which draws exactly 1.7's title and "Inventory" strings.
 */
public final class ScreenRBMKOutgasser extends RBMKContainerScreen<MenuRBMKOutgasser> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/reactors/gui_rbmk_outgasser.png");

    public ScreenRBMKOutgasser(MenuRBMKOutgasser menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        texture(graphics, TEX);
        BlockEntityRBMKOutgasser outgasser = menu.blockEntity();
        if (outgasser == null) return;
        int progress = outgasser.duration > 0 ? (int) (outgasser.progress * 13 / outgasser.duration) : 0;
        if (progress > 0) GuiDraw.blit(graphics, TEX, leftPos + 82, topPos + 50, 176, 0, progress, 6, 256, 256);
        int gas = outgasser.gas.getMaxFill() > 0 ? outgasser.gas.getFill() * 42 / outgasser.gas.getMaxFill() : 0;
        if (gas > 0) GuiDraw.blit(graphics, TEX, leftPos + 115, topPos + 66 - gas, 188, 42 - gas, 10, gas, 256, 256);
    }

    @Override protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityRBMKOutgasser outgasser = menu.blockEntity();
        if (outgasser == null) return;
        // 1.7 FluidTank.renderTankInfo hit test: x <= mouse < x + w, y < mouse <= y + h.
        int x = leftPos + 112, y = topPos + 21;
        if (x <= mouseX && x + 16 > mouseX && y < mouseY && y + 48 >= mouseY) {
            tooltip(graphics, TankDisplay.tooltip(outgasser.gas, true), mouseX, mouseY);
        }
    }
}
