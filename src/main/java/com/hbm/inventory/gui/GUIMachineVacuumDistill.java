package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineVacuumDistill;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Screen for the vacuum distiller — 1.12.2 {@code GUIMachineVacuumDistill} ({@code gui_vacuum_distill.png},
 * 176×238 panel on a 256×256 texture). Power bar at x=26, 5 tank bars at x=44/80/98/116/134 (bottom y=70). */
public class GUIMachineVacuumDistill extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineVacuumDistill> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_vacuum_distill.png");
    private static final int[] TANK_X = {44, 80, 98, 116, 134};

    public GUIMachineVacuumDistill(ContainerMachineVacuumDistill menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 238);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        this.titleLabelY = 5;
        this.inventoryLabelY = 500; // old GUI drew only the centred title
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineVacuumDistill te = this.menu.getTile();
        if (te == null) return;

        int j = (int) te.getPowerScaled(52);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 26, this.topPos + 70 - j, 176.0F, (float) (52 - j), 16, j, 256, 256);

        for (int i = 0; i < 5; i++) drawTank(graphics, te.tanks[i], TANK_X[i], 18, 16, 52);
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
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineVacuumDistill te = this.menu.getTile();
        if (te == null) return;

        if (this.isHovering(26, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineVacuumDistill.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < 5; i++) {
            if (this.isHovering(TANK_X[i], 18, 16, 52, mouseX, mouseY)) {
                FluidTankNTM tank = te.tanks[i];
                GuiDraw.tooltip(graphics, this.font, List.of(
                        Component.literal(tank.getTankType().getName()),
                        Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }
}
