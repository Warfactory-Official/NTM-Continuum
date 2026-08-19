package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerPyroOven;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Screen for the pyro oven — 1.12.2 {@code GUIPyroOven} ({@code gui_pyrooven.png}, 176×204 on 256×256).
 * Power bar x=152 (src u=176 v=64−i), progress arrow (57,47) src(176,0) 27 wide; tanks x=8/116 (bottom y=70).
 * The upgrade info panel (old drawCustomInfoStat) is deferred — §20. */
public class GUIPyroOven extends dev.golem.core.gui.CompatContainerScreen<ContainerPyroOven> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_pyrooven.png");

    public GUIPyroOven(ContainerPyroOven menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2 - 18;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachinePyroOven te = this.menu.getTile();
        if (te == null) return;

        int i = (int) te.getPowerScaled(52);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 70 - i, 176.0F, (float) (64 - i), 16, i, 256, 256);

        int p = te.getProgressScaled(27);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 57, this.topPos + 47, 176.0F, 0.0F, p, 12, 256, 256);

        drawTank(graphics, te.tanks[0], 8, 18, 16, 52);
        drawTank(graphics, te.tanks[1], 116, 18, 16, 52);
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
        TileEntityMachinePyroOven te = this.menu.getTile();
        if (te == null) return;

        if (this.isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachinePyroOven.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        int[] xs = {8, 116};
        for (int k = 0; k < 2; k++) {
            if (this.isHovering(xs[k], 18, 16, 52, mouseX, mouseY)) {
                FluidTankNTM tank = te.tanks[k];
                GuiDraw.tooltip(graphics, this.font, List.of(
                        tank.getTankType().getDisplayName(),
                        Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }
}
