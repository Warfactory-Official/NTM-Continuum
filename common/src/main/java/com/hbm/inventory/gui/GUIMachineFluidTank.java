package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

public class GUIMachineFluidTank extends AbstractContainerScreen<ContainerMachineFluidTank> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/storage/gui_tank.png");
    private static final String[] MODE_NAMES = {"Input only", "Buffer (in+out)", "Output only", "Disabled"};

    private static final int BAR_X = 71, BAR_TOP = 17, BAR_W = 34, BAR_H = 52, BAR_BOTTOM = 69;
    private static final int BTN_X = 151, BTN_Y = 34, BTN_W = 18, BTN_H = 18;

    public GUIMachineFluidTank(ContainerMachineFluidTank menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineFluidTank tank = this.menu.getTank();

        if (tank != null) {
            FluidTankNTM ft = tank.tank;
            int max = ft.getMaxFill();
            int h = max <= 0 ? 0 : (int) ((long) ft.getFill() * BAR_H / max);
            if (h > 0) {
                Identifier fluidTex = ft.getTankType().getTexture();
                int x0 = this.leftPos + BAR_X;
                int yBottom = this.topPos + BAR_BOTTOM;
                int yTop = yBottom - h;
                if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {

                    graphics.enableScissor(x0, yTop, x0 + BAR_W, yBottom);
                    for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                        for (int tx = 0; tx < BAR_W; tx += 16) {
                            int tw = Math.min(16, BAR_W - tx);
                            graphics.blit(RenderPipelines.GUI_TEXTURED, fluidTex, x0 + tx, ty, 0F, 0F, tw, 16, 16, 16);
                        }
                    }
                    graphics.disableScissor();
                } else {

                    graphics.fill(x0, yTop, x0 + BAR_W, yBottom, 0xFF000000 | (ft.getTankType().getColor() & 0xFFFFFF));
                }
            }

            int mode = tank.mode;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + BTN_X, this.topPos + BTN_Y, 176.0F, (float) (mode * 18), BTN_W, BTN_H, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        TileEntityMachineFluidTank tank = this.menu.getTank();
        if (tank == null) return;

        if (this.isHovering(BAR_X, BAR_TOP, BAR_W, BAR_H, mouseX, mouseY)) {
            FluidTankNTM ft = tank.tank;
            graphics.setTooltipForNextFrame(this.font, List.of(
                    Component.literal(ft.getTankType().getName()),
                    Component.literal(ft.getFill() + " / " + ft.getMaxFill() + " mB")
            ), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(
                    Component.literal("Mode: " + MODE_NAMES[tank.mode % MODE_NAMES.length])
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
