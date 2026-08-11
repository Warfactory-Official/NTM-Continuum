package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFEL;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.tileentity.machine.TileEntityFEL;
import net.minecraft.ChatFormatting;
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

/**
 * Client screen for the FEL. Coordinates are the 1.12.2 {@code GUIFEL} verbatim (203×169).
 *
 * <p>Beyond the vertical power bar it shows two pieces of status text the original leans on heavily:
 * <b>LIVE</b> in green whenever the laser is switched on, and <b>ERR.</b> in red when it is on but has
 * never found a valid SILEX — the player's only feedback that the beam is misaimed.
 *
 * <p>The horizontal streak across the middle is the in-GUI beam, drawn in the current wavelength's
 * colour (VISIBLE cycles the hue).
 */
public class GUIFEL extends AbstractContainerScreen<ContainerFEL> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/machine/gui_fel.png");

    private static final int POWER_X = 182, POWER_Y = 27, POWER_W = 16, POWER_H = 113;
    private static final int TOGGLE_X = 142, TOGGLE_Y = 41, TOGGLE_W = 29, TOGGLE_H = 17;

    public GUIFEL(ContainerFEL menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 203, 169);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        TileEntityFEL te = this.menu.getFel();
        if (te == null) return;

        // The switch lights up while the laser is on.
        if (te.isOn) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + TOGGLE_X, this.topPos + TOGGLE_Y,
                    203.0F, 0.0F, TOGGLE_W, TOGGLE_H, 256, 256);
        }

        int k = (int) te.getPowerScaled(POWER_H);
        if (k > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + POWER_X,
                    this.topPos + POWER_Y + POWER_H - k, 203.0F, (float) (17 + POWER_H - k), POWER_W, k, 256, 256);
        }

        // The in-GUI beam. NOTE the pow(2) gate: upstream's server cut uses pow(4), so the GUI can show
        // a beam for one tick after the real one has already stopped. Preserved.
        boolean beam = te.power > TileEntityFEL.powerReq * Math.pow(2, te.mode.ordinal())
                && te.isOn && te.mode != EnumWavelengths.NULL && te.distance > 0;
        if (beam) {
            int color = te.mode != EnumWavelengths.VISIBLE
                    ? te.mode.renderedBeamColor
                    : java.awt.Color.HSBtoRGB(gameTime() / 50.0F, 0.5F, 1.0F) & 0xFFFFFF;
            int argb = 0xFF000000 | (color & 0xFFFFFF);
            // Two segments at y = 31.5 (the emitter aperture and the exit), 10px thick.
            int y0 = this.topPos + 27, y1 = y0 + 10;
            graphics.fill(this.leftPos + 113, y0, this.leftPos + 135, y1, argb);
            graphics.fill(this.leftPos, y0, this.leftPos + 4, y1, argb);
        }
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Upstream nudges the title 90px right of centre; the status words sit to its left.
        int titleX = 90 + this.imageWidth / 2 - this.font.width(this.title) / 2;
        graphics.text(this.font, this.title, titleX, 7, 0xFFFFFFFF, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);

        TileEntityFEL te = this.menu.getFel();
        if (te == null) return;

        if (te.isOn) {
            Component live = Component.literal("LIVE").withStyle(ChatFormatting.GREEN);
            graphics.text(this.font, live, 54 + this.imageWidth / 2 - this.font.width(live) / 2, 9, 0xFF00FF00, false);
            if (te.missingValidSilex) {
                Component err = Component.literal("ERR.").withStyle(ChatFormatting.RED);
                graphics.text(this.font, err, 55 + this.imageWidth / 2 - this.font.width(err) / 2, 9, 0xFFFF0000, false);
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        TileEntityFEL te = this.menu.getFel();
        if (te == null) return;

        if (this.isHovering(POWER_X, POWER_Y, POWER_W, POWER_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font,
                    GuiUtil.energyTooltip(te.power, TileEntityFEL.maxPower), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font,
                    List.of(Component.literal(te.isOn ? "Turn off" : "Turn on")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isHovering(TOGGLE_X, TOGGLE_Y, TOGGLE_W, TOGGLE_H, event.x(), event.y())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
