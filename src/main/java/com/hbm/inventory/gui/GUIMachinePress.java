package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachinePress;
import com.hbm.tileentity.machine.TileEntityMachinePress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the burner press. Reads {@code speed}/{@code burnTime}/{@code progress} off the
 * synced client {@link TileEntityMachinePress} (broadcast via the BufPacket, not a 16-bit ContainerData
 * slot). Background + the burn-flame icon + the descending stamp progress bar, with hover tooltips for
 * the speed percentage and the remaining fuel operations — matching the 1.12.2 GUI.
 */
public class GUIMachinePress extends dev.golem.core.gui.CompatContainerScreen<ContainerMachinePress> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_press.png");

    public GUIMachinePress(ContainerMachinePress menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 202);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachinePress press = this.menu.getPress();
        if (press == null) return;

        // Burn-flame indicator (lit while there is fuel buffered).
        if (press.burnTime > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 27, this.topPos + 36, 0.0F, 202.0F, 14, 14, 256, 256);
        }
        // Descending stamp progress bar (grows downward from the top).
        int progress = press.getProgressScaled(16);
        if (progress > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 35, 14.0F, 202.0F, 18, progress, 256, 256);
        }

        // Dynamic speed dial — the rotating needle from the 1.12.2 GUIElements.drawSmoothGauge, a faithful
        // port: a black outer triangle (×1.5) behind a dark-red inner triangle (tip 5 / base 2 / back -2),
        // rotated by `-speed*270-45`° about the gauge centre via the original rotateRollSelf math. (fill
        // only draws rects, so the triangle is scanline-rasterised.)
        drawSpeedDial(graphics, this.leftPos + 34, this.topPos + 25, press.speed / (double) TileEntityMachinePress.maxSpeed);
    }

    /** Supersample factor: the triangle is rasterised at SS× and the pose scaled by 1/SS, so the rotated
     * edges and the tapered sides render smoothly instead of stepped. */
    private static final int SS = 8;

    private static void drawSpeedDial(GuiGraphicsExtractor graphics, int cx, int cy, double speed) {
        speed = Math.max(0.0, Math.min(1.0, speed));
        // The pose rotates the needle smoothly (clean rotated quads). The rotation matrix matches the
        // original's rotateRollSelf — placing the unrotated local tip (0,+tipLen) at the same screen point
        // requires pose angle = speed*270 + 45° (= -(−speed*270−45)).
        float angle = (float) Math.toRadians(speed * 270.0 + 45.0);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, cx, cy);
        GuiDraw.rotate(graphics, angle);
        GuiDraw.scale(graphics, 1.0F / SS, 1.0F / SS);
        fillNeedle(graphics, 1.5, 0xFF000000); // outer (black) ×1.5
        fillNeedle(graphics, 1.0, 0xFF7F0000); // inner (dark red) ×1.0
        GuiDraw.popMatrix(graphics);
    }

    /** The 1.12.2 gauge needle: a triangle, tip at local +5, base width ±1 at local −2. Symmetric about
     * the local X axis, so each row's half-width tapers linearly from the base to the tip. Drawn in local
     * SS-scaled space (the pose handles rotation + the 1/SS down-scale). */
    private static void fillNeedle(GuiGraphicsExtractor g, double mult, int color) {
        double tipLen = 5 * mult * SS, backLen = 2 * mult * SS, backSide = 1 * mult * SS;
        double total = tipLen + backLen;
        int top = (int) Math.round(tipLen), bot = (int) Math.round(-backLen);
        for (int y = bot; y < top; y++) {
            int hw = (int) Math.round(backSide * (tipLen - y) / total);
            if (hw > 0) g.fill(-hw, y, hw, y + 1, color);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachinePress press = this.menu.getPress();
        if (press == null) return;
        if (this.isHovering(25, 16, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal((press.speed * 100 / TileEntityMachinePress.maxSpeed) + "%")), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(25, 34, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.translatable("gui.press.operations", press.burnTime / 200)), Optional.empty(), mouseX, mouseY);
        }
    }
}
