package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The 1.7 {@code GuiInfoContainer} helpers this family's screens lean on: a hover tooltip over a painted region
 * ({@code drawCustomInfoStat}), a click on a region that sends one control flag ({@code clickSendFlag}), and the
 * smooth needle gauge ({@code GUIElements.drawSmoothGauge}).
 */
public abstract class PneumaticScreenBase<T extends AbstractContainerMenu> extends CompatContainerScreen<T> {

    protected PneumaticScreenBase(T menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    protected boolean hover(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    /** 1.7 {@code drawCustomInfoStat}: the given lines while the mouse is over the region. */
    protected void drawCustomInfoStat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y,
                                      int width, int height, String... lines) {
        if (!hover(x, y, width, height, mouseX, mouseY)) return;
        List<Component> components = new ArrayList<>(lines.length);
        for (String line : lines) components.add(Component.literal(line));
        GuiDraw.tooltip(graphics, font, components, Optional.empty(), mouseX, mouseY);
    }

    protected void drawCustomInfoStat(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int y,
                                      int width, int height, List<Component> lines) {
        if (!hover(x, y, width, height, mouseX, mouseY)) return;
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }

    /** 1.7 {@code clickSendFlag}: a click on the region sends {@code {key: true}} to the tile at {@code pos}. */
    protected boolean clickSendFlag(BlockPos pos, double mouseX, double mouseY, int x, int y, int width, int height,
                                    String key) {
        if (!hover(x, y, width, height, mouseX, mouseY)) return false;
        GuiUtil.playClickSound();
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, true);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(pos, data));
        return true;
    }

    /**
     * 1.7 {@code FluidTank.renderTank(x, y, z, width, height)}: the fluid's own sprite tiled up from the bar's bottom
     * edge ({@code y} is the bottom, as 1.7 took it) to the filled height, the way the fluid tank GUI ports it.
     */
    protected void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int bottom, int width, int height) {
        int max = tank.getMaxFill();
        int h = max <= 0 ? 0 : (int) ((long) tank.getFill() * height / max);
        if (h <= 0) return;
        int x0 = leftPos + x;
        int yBottom = topPos + bottom;
        int yTop = yBottom - h;
        var fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + width, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                for (int tx = 0; tx < width; tx += 16) {
                    int tw = Math.min(16, width - tx);
                    GuiDraw.blit(graphics, fluidTex, x0 + tx, ty, 0F, 0F, tw, 16, 16, 16);
                }
            }
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x0, yTop, x0 + width, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    /** Supersample factor: the needle is rasterised at SS times and the pose scaled by 1/SS. */
    private static final int SS = 8;

    /**
     * 1.7 {@code GUIElements.drawSmoothGauge}: a needle turned by {@code -progress * 270 - 45} degrees, an outer
     * triangle 1.5 times the inner one behind it. The ported rasteriser from the press dial.
     */
    protected static void drawSmoothGauge(GuiGraphicsExtractor graphics, int cx, int cy, double progress,
                                          double tipLength, double backLength, double backSide,
                                          int color, int colorOuter) {
        progress = Math.max(0.0, Math.min(1.0, progress));
        float angle = (float) Math.toRadians(progress * 270.0 + 45.0);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, cx, cy);
        GuiDraw.rotate(graphics, angle);
        GuiDraw.scale(graphics, 1.0F / SS, 1.0F / SS);
        fillNeedle(graphics, 1.5, tipLength, backLength, backSide, 0xFF000000 | colorOuter);
        fillNeedle(graphics, 1.0, tipLength, backLength, backSide, 0xFF000000 | color);
        GuiDraw.popMatrix(graphics);
    }

    private static void fillNeedle(GuiGraphicsExtractor g, double mult, double tipLength, double backLength,
                                   double backSide, int color) {
        double tipLen = tipLength * mult * SS, backLen = backLength * mult * SS, side = backSide * mult * SS;
        double total = tipLen + backLen;
        int top = (int) Math.round(tipLen), bot = (int) Math.round(-backLen);
        for (int y = bot; y < top; y++) {
            int hw = (int) Math.round(side * (tipLen - y) / total);
            if (hw > 0) GuiDraw.fill(g, -hw, y, hw, y + 1, color);
        }
    }
}
