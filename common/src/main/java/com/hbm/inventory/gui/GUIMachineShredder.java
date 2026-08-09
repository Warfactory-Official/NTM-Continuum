package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineShredder;
import com.hbm.tileentity.machine.TileEntityMachineShredder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the shredder. Reads {@code power}/{@code progress} and the two synced blade-wear
 * values off the client {@link TileEntityMachineShredder} (BufPacket).
 *
 * <p>Draws the energy bar (left), the shredding progress arrow, and a per-blade wear icon (green / worn /
 * broken, from the three 18×18 sprite rows in the texture). When a blade is missing or spent the machine
 * is stalled, so the blade icons carry a hover tooltip explaining it — the 26.1 stand-in for the old
 * {@code drawInfoPanel} error marker that hung off the GUI's left edge.
 */
public class GUIMachineShredder extends AbstractContainerScreen<ContainerMachineShredder> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/gui_shredder.png");

    public GUIMachineShredder(ContainerMachineShredder menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 233);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineShredder shredder = this.menu.getShredder();
        if (shredder == null) return;

        if (shredder.power > 0) {
            int i = (int) shredder.getPowerScaled(88);
            if (i > 0) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 8, this.topPos + 106 - i,
                        176.0F, (float) (160 - i), 16, i, 256, 256);
            }
        }

        int progress = shredder.getProgressScaled(34);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 63, this.topPos + 89,
                176.0F, 54.0F, progress + 1, 18, 256, 256);

        drawGear(graphics, shredder.getGearLeft(), this.leftPos + 43, 176.0F);
        drawGear(graphics, shredder.getGearRight(), this.leftPos + 79, 194.0F);
    }

    /** Wear icon: state 1/2/3 → the fine / worn / broken sprite row. State 0 (no blade) draws nothing. */
    private void drawGear(GuiGraphicsExtractor graphics, int state, int x, float u) {
        if (state < 1 || state > 3) return;
        float v = (state - 1) * 18.0F;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, this.topPos + 71, u, v, 18, 18, 256, 256);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        TileEntityMachineShredder shredder = this.menu.getShredder();
        if (shredder == null) return;

        if (this.isHovering(8, 18, 16, 88, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font,
                    GuiUtil.energyTooltip(shredder.power, TileEntityMachineShredder.maxPower),
                    Optional.empty(), mouseX, mouseY);
        } else if (shredder.bladesBroken() && this.isHovering(43, 71, 54, 18, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font,
                    List.of(Component.translatable("gui.shredder.bladesBroken").withStyle(ChatFormatting.RED)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Only the player-inventory label; the taller (233px) background already carries the machine name.
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.imageHeight - 96 + 2,
                0xFF404040, false);
    }
}
