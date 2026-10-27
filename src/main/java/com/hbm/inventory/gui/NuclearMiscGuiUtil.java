package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** The old {@code FluidTank.renderTank} quad, repeated 16x16 like the GL_REPEAT texture it replaced. */
public final class NuclearMiscGuiUtil {
    private NuclearMiscGuiUtil() {}

    /** {@code x, y} is the top-left of the gauge, unlike the old bottom-left {@code renderTank(x, y, z, w, h)}. */
    public static void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int max = tank.getMaxFill();
        int filled = max <= 0 ? 0 : (int) ((long) tank.getFill() * height / max);
        if (filled <= 0) return;

        int bottom = y + height;
        int top = bottom - filled;
        int tint = 0xFF000000 | (tank.getTankType().clientMetadata().getTint() & 0xFFFFFF);
        Identifier fluidTexture = tank.getTankType().clientMetadata().getTexture();

        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x, top, x + width, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                for (int tileX = x; tileX < x + width; tileX += 16) {
                    GuiDraw.blit(graphics, fluidTexture, tileX, tileY, 0F, 0F, 16, 16, 16, 16, tint);
                }
            }
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x, top, x + width, bottom, 0xFF000000 | (tank.getTankType().clientMetadata().getColor() & 0xFFFFFF));
        }
    }
}
