package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerLiquefactor;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.oil.TileEntityMachineLiquefactor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the liquefactor — 1.12.2 {@code GUILiquefactor} ({@code gui_liquefactor.png}, 176×204):
 * power bar 16×52 at (134,18) (fill u=176), progress arrow up to 42×35 at (42,17) (u=192), the
 * "powered" LED 9×12 at (138,4) (u=176,v=52), tank bar 16×52 at (71,36).
 */
public class GUILiquefactor extends dev.golem.core.gui.CompatContainerScreen<ContainerLiquefactor> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_liquefactor.png");

    public GUILiquefactor(ContainerLiquefactor menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    /** The 1.12.2 GUI drew no machine title (only the inventory label) — the texture carries the art. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineLiquefactor te = this.menu.getLiquefactor();
        if (te == null) return;

        int i = (int) (te.getPower() * 52 / te.getMaxPower());
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 134, this.topPos + 70 - i, 176.0F, (float) (52 - i), 16, i, 256, 256);

        int j = te.processTime > 0 ? te.progress * 42 / te.processTime : 0;
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 42, this.topPos + 17, 192.0F, 0.0F, j, 35, 256, 256);

        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 138, this.topPos + 4, 176.0F, 52.0F, 9, 12, 256, 256);

        drawTank(graphics, te.tank, 71, 36);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * 52 / maxF);
        if (h <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + 52;
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
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineLiquefactor te = this.menu.getLiquefactor();
        if (te == null) return;

        if (this.isHovering(134, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.getMaxPower()), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(71, 36, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    te.tank.getTankType().getDisplayName(),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getLiquefactor();
    }
}
