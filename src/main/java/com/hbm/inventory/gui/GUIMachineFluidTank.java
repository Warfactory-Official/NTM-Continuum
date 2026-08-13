package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineFluidTank;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineFluidTank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.1 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the fluid tank multiblock. Draws the {@code gui_tank.png} background, a coloured fill
 * bar (the carried fluid's colour, height scaled by fill), and the I/O-mode button. Reads the live
 * client {@link TileEntityMachineFluidTank} for fill/type/mode; the mode button dispatches through the
 * vanilla menu-button path. 26.1 render API (extractBackground + blit/fill).
 *
 * <p>The 1.12.2 {@code FluidTankNTM.renderTank} (a GL-textured fluid sprite) was stripped in the slim
 * port, so the fill is shown as a solid colour bar — enough to verify the network is filling the tank.
 */
public class GUIMachineFluidTank extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineFluidTank> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/storage/gui_tank.png");
    private static final String[] MODE_NAMES = {"Input only", "Buffer (in+out)", "Output only", "Disabled"};

    // Fill bar geometry (GUI-relative), matching the 1.12.2 layout: 34×52 at (71, 17)..(71, 69).
    private static final int BAR_X = 71, BAR_TOP = 17, BAR_W = 34, BAR_H = 52, BAR_BOTTOM = 69;
    private static final int BTN_X = 151, BTN_Y = 34, BTN_W = 18, BTN_H = 18;

    public GUIMachineFluidTank(ContainerMachineFluidTank menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    //? if >=26.1 {
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
    *///?}
        //? if >=26.1 {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        //?}
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineFluidTank tank = this.menu.getTank();

        // Fluid fill bar — the fluid's own 16×16 GUI texture TILED at native scale into the filled
        // height, bottom-anchored (the 1.12.2 renderTank tiled type.getTexture() via a REPEAT quad;
        // stretching it looked wrong). Static, like the original; a scissor clips the partial top tile.
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
                    // Tile the fluid's own 16×16 sprite, clipped to the filled height.
                    graphics.enableScissor(x0, yTop, x0 + BAR_W, yBottom);
                    for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                        for (int tx = 0; tx < BAR_W; tx += 16) {
                            int tw = Math.min(16, BAR_W - tx);
                            GuiDraw.blit(graphics, fluidTex, x0 + tx, ty, 0F, 0F, tw, 16, 16, 16);
                        }
                    }
                    graphics.disableScissor();
                } else {
                    // A few of the ~157 fluids ship no GUI sprite — fall back to a solid colour bar.
                    graphics.fill(x0, yTop, x0 + BAR_W, yBottom, 0xFF000000 | (ft.getTankType().getColor() & 0xFFFFFF));
                }
            }
            // I/O mode button overlay (strip at u=176, v=mode*18).
            int mode = tank.mode;
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + BTN_X, this.topPos + BTN_Y, 176.0F, (float) (mode * 18), BTN_W, BTN_H, 256, 256);
        }
    }

    @Override
    //? if >=26.1 {
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    //?} else {
    /*protected void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    *///?}
        //? if >=26.1 {
        super.extractTooltip(graphics, mouseX, mouseY);
        //?} else {
        /*super.renderTooltip(graphics, mouseX, mouseY);
        *///?}
        TileEntityMachineFluidTank tank = this.menu.getTank();
        if (tank == null) return;

        if (this.isHovering(BAR_X, BAR_TOP, BAR_W, BAR_H, mouseX, mouseY)) {
            FluidTankNTM ft = tank.tank;
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal(ft.getTankType().getName()),
                    Component.literal(ft.getFill() + " / " + ft.getMaxFill() + " mB")
            ), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal("Mode: " + MODE_NAMES[tank.mode % MODE_NAMES.length])
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    //? if >=26.1 {
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    //?} else {
    /*public boolean mouseClicked(double hbm$mx, double hbm$my, int hbm$btn) {
        dev.golem.core.gui.MouseInput event = new dev.golem.core.gui.MouseInput(hbm$mx, hbm$my, hbm$btn);
    *///?}
        if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        }
        //? if >=26.1 {
        return super.mouseClicked(event, doubleClick);
        //?} else {
        /*return super.mouseClicked(hbm$mx, hbm$my, hbm$btn);
        *///?}
    }
}
