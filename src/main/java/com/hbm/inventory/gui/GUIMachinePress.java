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
 * Client screen for the burner press, a straight port of 1.7 {@code GUIMachinePress}
 * ({@code processing/gui_press.png}, 176x214). Reads {@code speed}/{@code burnTime}/{@code progress} off the
 * synced client {@link TileEntityMachinePress} (broadcast via the BufPacket, not a 16-bit ContainerData slot).
 */
public class GUIMachinePress extends dev.golem.core.gui.CompatContainerScreen<ContainerMachinePress> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_press.png");

    public GUIMachinePress(ContainerMachinePress menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 214);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachinePress press = this.menu.getPress();
        if (press == null) return;

        // 1.7: flame lit while at least 20 ticks of fuel are buffered.
        if (press.burnTime >= 20) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 26, this.topPos + 36, 0.0F, 214.0F, 14, 14, 256, 256);
        }
        // 1.7: k = renderPress * 16 / maxPress, drawn from the top down.
        int k = press.getProgressScaled(16);
        if (k > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 35, 15.0F, 214.0F, 18, k, 256, 256);
        }

        GuiUtil.drawSmoothGauge(graphics, this.leftPos + 34, this.topPos + 25,
                press.speed / (double) TileEntityMachinePress.maxSpeed, 5, 2, 1, 0x7F0000);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7: white title at y=5, grey "Inventory" at ySize - 96 + 2.
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 5, 0xffffff, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
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
