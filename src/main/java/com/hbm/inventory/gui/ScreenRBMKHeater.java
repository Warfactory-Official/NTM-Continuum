package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKHeater;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class ScreenRBMKHeater extends RBMKContainerScreen<MenuRBMKHeater> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/rbmk/gui_rbmk_heater.png");

    public ScreenRBMKHeater(MenuRBMKHeater menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        texture(graphics, TEX);
        BlockEntityRBMKHeater heater = menu.blockEntity();
        if (heater == null) return;
        drawFluidBar(graphics, heater.feed, 68, 24);
        drawFluidBar(graphics, heater.steam, 126, 24);
    }

    @Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityRBMKHeater heater = menu.blockEntity();
        if (heater == null) return;
        fluidTooltip(graphics, heater.feed, 68, 24, 16, 58, mouseX, mouseY);
        fluidTooltip(graphics, heater.steam, 126, 24, 16, 58, mouseX, mouseY);
    }

    private void drawFluidBar(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        if (tank.getMaxFill() <= 0 || tank.getFill() <= 0) return;
        int height = tank.getFill() * 58 / tank.getMaxFill();
        GuiDraw.fill(graphics, leftPos + x, topPos + y + 58 - height, leftPos + x + 14, topPos + y + 58,
                0xFF000000 | tank.getTankType().getColor());
    }
}
