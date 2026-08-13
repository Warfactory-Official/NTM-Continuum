package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineDiesel;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineDiesel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the diesel generator — 1.12.2 {@code GUIMachineDiesel} (texture {@code gui_diesel.png},
 * 176×166; the old ref was mis-cased {@code GUIDiesel.png} while the file on disk is {@code guidiesel.png}
 * — copied to a lowercase path for 26.1). Power bar 16×52 at (152,18) (fill u=176), the "burning" flame
 * icon 18×18 at (115,34) (u=208) while fuelled, tank bar 16×52 at (80,17).
 */
public class GUIMachineDiesel extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineDiesel> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/gui_diesel.png");

    public GUIMachineDiesel(ContainerMachineDiesel menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2; // old GUI centred the name
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

        TileEntityMachineDiesel te = this.menu.getDiesel();
        if (te == null) return;

        if (te.power > 0) {
            int i = (int) te.getPowerScaled(52);
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 69 - i, 176.0F, (float) (52 - i), 16, i, 256, 256);
        }

        if (te.tank.getFill() > 0 && te.hasAcceptableFuel()) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 43 + 18 * 4, this.topPos + 34, 208.0F, 0.0F, 18, 18, 256, 256);
        }

        drawTank(graphics, te.tank, 80, 17);
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
        TileEntityMachineDiesel te = this.menu.getDiesel();
        if (te == null) return;

        if (this.isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.getMaxPower()), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(80, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal(te.tank.getTankType().getName()),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        }
    }
}
