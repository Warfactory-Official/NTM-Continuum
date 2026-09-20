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
 * Shared chrome for the five dark fusion core panels: the vertical power column, the coolant and fuel
 * tanks, and the little watts gauge the emitter and stabiliser both carry. Consolidated here rather than
 * in each screen, the way {@link GUIPABase} serves the accelerator's panels.
 */
public abstract class GUIDFCBase<T extends AbstractContainerMenu> extends CompatContainerScreen<T> {

    protected GUIDFCBase(T menu, Inventory playerInventory, Component title, int width, int height) {
        super(menu, playerInventory, title, width, height);
    }

    protected abstract Identifier texture();

    /** The 16x52 power column, filling upward from {@code y + 52}. */
    protected void drawPowerBar(GuiGraphicsExtractor graphics, int x, int y, long power, long maxPower) {
        int height = (int) (power * 52 / Math.max(1L, maxPower));
        if (height <= 0) return;
        GuiDraw.blit(graphics, texture(), leftPos + x, topPos + y + 52 - height,
                176F, (float) (52 - height), 16, height, 256, 256);
    }

    /** The 4x35 watts gauge, filling upward from {@code y + 35}. */
    protected void drawWattsBar(GuiGraphicsExtractor graphics, int x, int y, int wattsScaled) {
        if (wattsScaled <= 0) return;
        GuiDraw.blit(graphics, texture(), leftPos + x, topPos + y + 35 - wattsScaled,
                176F, (float) (87 - wattsScaled), 4, wattsScaled, 256, 256);
    }

    /** Repeats the 16x16 fluid texture, matching the old GL_REPEAT tank quad. */
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

    protected void drawTitleAndInventory(GuiGraphicsExtractor graphics) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle,
                imageWidth - 8 - font.width(playerInventoryTitle), imageHeight - 96 + 2, 0xFF404040, false);
    }
}
