package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.List;
import java.util.Optional;

/**
 * Shared helpers for the five dark fusion core panels (1.7 {@code GUICore}, {@code GUICoreEmitter},
 * {@code GUICoreInjector}, {@code GUICoreReceiver}, {@code GUICoreStabilizer}): the 1.7 {@code FluidTank.renderTank}
 * quad, tooltips, and the title/inventory labels every one of them draws the same way.
 */
public abstract class GUIDFCBase<T extends AbstractContainerMenu> extends CompatContainerScreen<T> {

    protected GUIDFCBase(T menu, Inventory playerInventory, Component title, int width, int height) {
        super(menu, playerInventory, title, width, height);
    }

    protected abstract Identifier texture();

    /** Repeats the 16x16 fluid texture, matching the old GL_REPEAT tank quad; {@code y} is the tank's top. */
    protected void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int max = tank.getMaxFill();
        int filled = max <= 0 ? 0 : (int) ((long) tank.getFill() * height / max);
        if (filled <= 0) return;

        int x0 = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - filled;
        int tint = 0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF);
        Identifier fluidTexture = tank.getTankType().getTexture();

        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + width, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x0, tileY, 0F, 0F, width, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x0, top, x0 + width, bottom,
                    0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    protected void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, text, Optional.empty(), mouseX, mouseY);
    }

    /** 1.7 {@code drawGuiContainerForegroundLayer}: centred title at y 6, "Inventory" at (8, ySize - 94), 4210752. */
    protected void drawTitleAndInventory(GuiGraphicsExtractor graphics) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }
}
