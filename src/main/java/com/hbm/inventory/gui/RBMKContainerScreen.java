package com.hbm.inventory.gui;

import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;
import java.util.Optional;

abstract class RBMKContainerScreen<T extends AbstractContainerMenu> extends CompatContainerScreen<T> {
    protected RBMKContainerScreen(T menu, Inventory inventory, Component title, int w, int h) { super(menu, inventory, title, w, h); }
    protected final void texture(GuiGraphicsExtractor graphics, Identifier texture) { GuiDraw.blit(graphics, texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256); }
    protected final boolean region(int x, int y, int w, int h, int mouseX, int mouseY) { return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h; }
    protected final void fluidTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int w, int h,
            int mouseX, int mouseY) {
        if (region(x, y, w, h, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(tank.getTankType().getDisplayName(),
                    Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        }
    }

    protected final void tooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0xFF404040, false);
    }
}
