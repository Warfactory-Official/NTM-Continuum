package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineRefinery;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the refinery — 1.12.2 {@code GUIMachineRefinery} ({@code gui_refinery.png}, <b>350×256</b>
 * source): power bar 16×52 at (186,18) (fill sampled from u=210), the input-oil column 16×101 at
 * (33,29) (fill from u=226), the four coloured product pipes (tinted by the recipe's fraction colours),
 * and the four output tank bars 16×52 at x=86/106/126/146 (y=95 bottom). Because the texture is not
 * 256×256, every blit passes explicit source dimensions 350×256.
 */
public class GUIMachineRefinery extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineRefinery> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_refinery.png");
    private static final int TEX_W = 350, TEX_H = 256;

    public GUIMachineRefinery(ContainerMachineRefinery menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 210, 231);
    }

    @Override
    protected void init() {
        super.init();
        // Old GUI: name centred with a −17 offset (xSize/2 − 34/2 − w/2); inventory label at ySize−96+4.
        this.titleLabelX = this.imageWidth / 2 - 17 - this.font.width(this.title) / 2;
        this.inventoryLabelY = this.imageHeight - 92;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, TEX_W, TEX_H);

        TileEntityMachineRefinery te = this.menu.getRefinery();
        if (te == null) return;

        // Power bar (16×52 at (186,18)).
        int j = (int) te.getPowerScaled(50);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 186, this.topPos + 69 - j, 210.0F, (float) (52 - j), 16, j, TEX_W, TEX_H);

        // Input oil column (16×101 at (33,29), fill from u=226) tinted by the input fluid colour.
        FluidTankNTM inputOil = te.tanks[0];
        if (inputOil.getFill() > 0) {
            int h = inputOil.getFill() * 101 / inputOil.getMaxFill();
            int color = 0xFF000000 | (inputOil.getTankType().getColor() & 0xFFFFFF);
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 33, this.topPos + 130 - h, 226.0F, (float) (101 - h), 16, h, TEX_W, TEX_H, color);
        }

        // The 4 product pipes, tinted by the recipe's fraction colours (grey when no recipe).
        RefineryRecipes.RefineryRecipe recipe = RefineryRecipes.getRefinery(inputOil.getTankType());
        int cHeavy = recipe != null ? tint(recipe.frac1().type.getColor()) : 0xFFFFFFFF;
        int cNaph = recipe != null ? tint(recipe.frac2().type.getColor()) : 0xFFFFFFFF;
        int cLight = recipe != null ? tint(recipe.frac3().type.getColor()) : 0xFFFFFFFF;
        int cGas = recipe != null ? tint(recipe.frac4().type.getColor()) : 0xFFFFFFFF;
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 52, this.topPos + 63, 247.0F, 1.0F, 33, 48, TEX_W, TEX_H, cHeavy);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 52, this.topPos + 32, 247.0F, 50.0F, 66, 52, TEX_W, TEX_H, cNaph);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 52, this.topPos + 24, 247.0F, 145.0F, 86, 35, TEX_W, TEX_H, cLight);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 36, this.topPos + 16, 211.0F, 119.0F, 122, 25, TEX_W, TEX_H, cGas);

        // Output tank bars.
        drawTank(graphics, te.tanks[1], 86);
        drawTank(graphics, te.tanks[2], 106);
        drawTank(graphics, te.tanks[3], 126);
        drawTank(graphics, te.tanks[4], 146);
    }

    private static int tint(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * 52 / maxF);
        if (h <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + 43 + 52; // top 43 -> bottom 95
        int yTop = yBottom - h;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + 16, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, 16, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + 16, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineRefinery te = this.menu.getRefinery();
        if (te == null) return;

        if (this.isHovering(186, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineRefinery.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(33, 29, 16, 101, mouseX, mouseY)) { tankTooltip(graphics, te.tanks[0], mouseX, mouseY); return; }
        int[] xs = {86, 106, 126, 146};
        for (int i = 0; i < 4; i++) {
            if (this.isHovering(xs[i], 43, 16, 52, mouseX, mouseY)) { tankTooltip(graphics, te.tanks[i + 1], mouseX, mouseY); return; }
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, List.of(
                tank.getTankType().getDisplayName(),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")
        ), Optional.empty(), mouseX, mouseY);
    }
}
