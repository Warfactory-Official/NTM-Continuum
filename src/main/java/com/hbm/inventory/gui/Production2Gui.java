package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** The 1.7 {@code FluidTank.renderTank(x, yBottom, z, width, height)} column, shared by the group-B screens. */
public final class Production2Gui {

    private Production2Gui() {}

    /** Draws the tank level bottom-anchored at {@code (x, yBottom)}, tiling the fluid texture. */
    public static void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int yBottom, int width, int height) {
        int level = tank.getMaxFill() <= 0 ? 0 : (int) ((long) tank.getFill() * height / tank.getMaxFill());
        if (level <= 0) return;
        int top = yBottom - level;
        Identifier texture = tank.getTankType().clientMetadata().getTexture();
        int tint = 0xFF000000 | (tank.getTankType().clientMetadata().getTint() & 0xFFFFFF);
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x, top, x + width, yBottom);
            for (int tileY = yBottom - 16; tileY > top - 16; tileY -= 16) {
                for (int tileX = x; tileX < x + width; tileX += 16) {
                    GuiDraw.blit(graphics, texture, tileX, tileY, 0F, 0F, 16, 16, 16, 16, tint);
                }
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x, top, x + width, yBottom, tint);
        }
    }

    /** The 1.7 500 ms blink phase used by every "something is missing" indicator. */
    public static boolean blink() {
        return System.currentTimeMillis() % 1000 < 500;
    }
}
