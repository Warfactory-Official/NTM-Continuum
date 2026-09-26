package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
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

        Gauges.needle(graphics, this.leftPos + 34, this.topPos + 25,
                press.speed / (double) TileEntityMachinePress.maxSpeed, 0x7F0000);
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
