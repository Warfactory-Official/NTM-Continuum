package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineCoker;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Screen for the coker — 1.12.2 {@code GUIMachineCoker} ({@code gui_coker.png}, 176×204 on a 256×256
 * texture). Progress bar (61,46) src(176,0), heat bar (61,55) src(176,5); tank bars at x=35/125 (bottom y=70). */
public class GUIMachineCoker extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineCoker> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_coker.png");

    public GUIMachineCoker(ContainerMachineCoker menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCoker te = this.menu.getTile();
        if (te == null) return;

        int p = te.getProgressScaled(53);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 61, this.topPos + 46, 176.0F, 0.0F, p, 5, 256, 256);
        int h = te.getHeatScaled(52);
        if (h > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 61, this.topPos + 55, 176.0F, 5.0F, h, 5, 256, 256);

        drawTank(graphics, te.tanks[0], 35, 18, 16, 52);
        drawTank(graphics, te.tanks[1], 125, 18, 16, 52);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop, int w, int h) {
        int maxF = tank.getMaxFill();
        int fh = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * h / maxF);
        if (fh <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + h;
        int yTop = yBottom - fh;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + w, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, w, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + w, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCoker te = this.menu.getTile();
        if (te == null) return;

        int[] xs = {35, 125};
        for (int i = 0; i < 2; i++) {
            if (this.isHovering(xs[i], 18, 16, 52, mouseX, mouseY)) {
                FluidTankNTM tank = te.tanks[i];
                GuiDraw.tooltip(graphics, this.font, List.of(
                        tank.getTankType().getDisplayName(),
                        Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        if (this.isHovering(60, 45, 54, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal(String.format("%,d / %,d TU",
                    te.progress, te.processTime))), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(60, 54, 54, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.literal(
                    String.format("%,d / %,d TU", te.heat, TileEntityMachineCoker.maxHeat))), Optional.empty(), mouseX, mouseY);
        }
    }
}
