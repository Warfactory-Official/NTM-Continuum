package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerSILEX;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.tileentity.machine.TileEntitySILEX;
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
 * Client screen for the SILEX. Coordinates are the 1.12.2 {@code GUISILEX} verbatim.
 *
 * <p>Three gauges plus the signature animated sine trace:
 * <ul>
 *   <li>the <b>peroxide tank</b>, a 52×7 horizontal bar at (8, 42), whose <i>frame</i> at (7, 41)
 *       switches between a "valid" and "invalid" graphic depending on whether the loaded fluid is
 *       something the chamber can actually use;</li>
 *   <li>the <b>solution pool</b>, a 16×52 column at (26, 72) filling bottom-up;</li>
 *   <li>the <b>progress</b> bar, 69×43 at (45, 82);</li>
 *   <li>the <b>wave</b>: a line-strip sine whose frequency is {@code 0.0125 · 2^ordinal}, so a stronger
 *       laser visibly oscillates faster. VISIBLE has no fixed colour and cycles the hue instead.</li>
 * </ul>
 *
 * <p>The 12×12 hotspot at (10, 92) is the "void contents" button, routed through the vanilla
 * {@code clickMenuButton} path like the battery socket's buttons.
 */
public class GUISILEX extends AbstractContainerScreen<ContainerSILEX> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/processing/gui_silex.png");

    private static final int VOID_X = 10, VOID_Y = 92, VOID_W = 12;
    private static final int TANK_X = 8, TANK_Y = 42, TANK_W = 52, TANK_H = 7;
    private static final int POOL_X = 26, POOL_TOP = 72, POOL_W = 16, POOL_H = 52;

    public GUISILEX(ContainerSILEX menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        TileEntitySILEX te = this.menu.getSilex();
        if (te == null) return;

        // Tank frame: the "valid fluid" strip when the loaded fluid is peroxide (the dissolving agent)
        // or is itself a SILEX input; otherwise the "invalid" strip, telling the player nothing will run.
        if (te.tank.getFill() > 0) {
            boolean usable = te.tank.getTankType() == Fluids.PEROXIDE
                    || SILEXRecipes.getOutput(ItemFluidIcon.make(te.tank.getTankType())) != null;
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 7, this.topPos + 41,
                    176.0F, usable ? 118.0F : 109.0F, 54, 9, 256, 256);
        }

        int p = te.getProgressScaled(69);
        if (p > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 45, this.topPos + 82,
                    176.0F, 0.0F, p, 43, 256, 256);
        }

        int f = te.getFillScaled(POOL_H);
        if (f > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + POOL_X, this.topPos + 124 - f,
                    176.0F, (float) (109 - f), POOL_W, f, 256, 256);
        }

        int i = te.getFluidScaled(52);
        if (i > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + TANK_X, this.topPos + TANK_Y,
                    176.0F, te.tank.getTankType() == Fluids.PEROXIDE ? 43.0F : 50.0F, i, TANK_H, 256, 256);
        }

        if (te.mode != EnumWavelengths.NULL) {
            float freq = 0.0125F * (float) Math.pow(2, te.mode.ordinal());
            int color = te.mode != EnumWavelengths.VISIBLE ? te.mode.guiColor : rainbow(gameTime(), 50.0F, 1.0F);
            drawWave(graphics, 81, 46, 16, 84, 0.5F, freq, color);
        }
    }

    /**
     * The 1.12.2 {@code drawWave}, re-expressed for 26.1.
     *
     * <p>The original was a {@code GL_LINE_STRIP} of {@code width/resolution} vertices with
     * {@code glLineWidth(3)} and {@code GL_LINE_SMOOTH}. 26.1's GUI layer has no immediate-mode line
     * primitive, so the curve is drawn as a run of 1×2-pixel quads along the same sample points — the
     * same shape at the same amplitude and phase, without the smoothed line caps.
     *
     * <pre>
     *   samples = width / resolution
     *   scale   = height / 2
     *   offset  = gameTime % (4π / freq)          // scrolls the wave leftward over time
     *   y(x)    = y + scale · sin((x + offset) · freq)
     * </pre>
     */
    private void drawWave(GuiGraphicsExtractor graphics, int x, int y, int height, int width,
                          float resolution, float freq, int color) {
        int argb = 0xFF000000 | (color & 0xFFFFFF);
        float samples = width / resolution;
        float scale = height / 2F;
        float offset = (float) (gameTime() % (4 * Math.PI / freq));

        for (int i = 0; i <= samples; i++) {
            double sx = x + i * resolution;
            double sy = y + scale * Math.sin((sx + offset) * freq);
            int px = this.leftPos + (int) Math.round(sx);
            int py = this.topPos + (int) Math.round(sy);
            graphics.fill(px, py, px + 1, py + 2, argb);
        }
    }

    /** Hue cycling on the world clock — the old {@code Color.HSBtoRGB(time / divisor, 0.5F, b)}. */
    private static int rainbow(long time, float divisor, float brightness) {
        return java.awt.Color.HSBtoRGB(time / divisor, 0.5F, brightness) & 0xFFFFFF;
    }

    private long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Upstream nudges the title 54px LEFT of centre to clear the wave display, and draws the
        // wavelength name in the panel to its right. Both are GUI-local coordinates.
        Component title = this.title;
        int titleX = this.imageWidth / 2 - this.font.width(title) / 2 - 54;
        graphics.text(this.font, title, titleX, 8, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);

        TileEntitySILEX te = this.menu.getSilex();
        if (te != null && te.mode != EnumWavelengths.NULL) {
            Component mode = Component.translatable(te.mode.name).withStyle(te.mode.textColor);
            graphics.text(this.font, mode, 100 + (32 - this.font.width(mode) / 2), 16, 0xFF000000, false);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        TileEntitySILEX te = this.menu.getSilex();
        if (te == null) return;

        if (this.isHovering(TANK_X, TANK_Y, TANK_W, TANK_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(
                    Component.literal(te.tank.getTankType().getName()),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (te.current != null && this.isHovering(POOL_X + 1, POOL_TOP, POOL_W, POOL_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(
                    Component.literal(te.currentFill + "/" + TileEntitySILEX.maxFill + "mB"),
                    te.current.getHoverName()), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(VOID_X, VOID_Y, VOID_W, VOID_W, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(Component.literal("Void contents")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.isHovering(VOID_X, VOID_Y, VOID_W, VOID_W, event.x(), event.y())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
