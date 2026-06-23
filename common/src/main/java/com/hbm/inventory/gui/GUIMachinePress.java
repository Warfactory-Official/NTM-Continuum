package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachinePress;
import com.hbm.tileentity.machine.TileEntityMachinePress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class GUIMachinePress extends AbstractContainerScreen<ContainerMachinePress> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/gui_press.png");

    public GUIMachinePress(ContainerMachinePress menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 202);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachinePress press = this.menu.getPress();
        if (press == null) return;

        if (press.burnTime > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 27, this.topPos + 36, 0.0F, 202.0F, 14, 14, 256, 256);
        }

        int progress = press.getProgressScaled(16);
        if (progress > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 79, this.topPos + 35, 14.0F, 202.0F, 18, progress, 256, 256);
        }

        drawSpeedDial(graphics, this.leftPos + 34, this.topPos + 25, press.speed / (double) TileEntityMachinePress.maxSpeed);
    }

    private static final int SS = 8;

    private static void drawSpeedDial(GuiGraphicsExtractor graphics, int cx, int cy, double speed) {
        speed = Math.max(0.0, Math.min(1.0, speed));

        float angle = (float) Math.toRadians(speed * 270.0 + 45.0);
        graphics.pose().pushMatrix();
        graphics.pose().translate(cx, cy);
        graphics.pose().rotate(angle);
        graphics.pose().scale(1.0F / SS, 1.0F / SS);
        fillNeedle(graphics, 1.5, 0xFF000000);
        fillNeedle(graphics, 1.0, 0xFF7F0000);
        graphics.pose().popMatrix();
    }

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
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        TileEntityMachinePress press = this.menu.getPress();
        if (press == null) return;
        if (this.isHovering(25, 16, 18, 18, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(
                    Component.literal((press.speed * 100 / TileEntityMachinePress.maxSpeed) + "%")), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(25, 34, 18, 18, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(
                    Component.translatable("gui.press.operations", press.burnTime / 200)), Optional.empty(), mouseX, mouseY);
        }
    }
}
