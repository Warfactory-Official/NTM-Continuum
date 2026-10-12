package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerFEL;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.tileentity.machine.TileEntityFEL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Client screen for the FEL, ported from 1.7 {@code GUIFEL} (203×169).
 *
 * <p>Beyond the vertical power bar it shows two pieces of status text the original leans on heavily:
 * <b>LIVE</b> in green whenever the laser is switched on, and <b>ERR.</b> in red when it is on but has
 * never found a valid SILEX — the player's only feedback that the beam is misaimed.
 *
 * <p>The horizontal streak across the middle is the in-GUI beam, drawn in the current wavelength's
 * colour (VISIBLE cycles the hue).
 */
public class GUIFEL extends dev.golem.core.gui.CompatContainerScreen<ContainerFEL> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_fel.png");

    private static final int POWER_X = 182, POWER_Y = 27, POWER_W = 16, POWER_H = 113;
    private static final int TOGGLE_X = 142, TOGGLE_Y = 41, TOGGLE_W = 29, TOGGLE_H = 17;

    public GUIFEL(ContainerFEL menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 203, 169);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        TileEntityFEL te = this.menu.getFel();
        if (te == null) return;

        // The switch lights up while the laser is on.
        if (te.isOn) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + TOGGLE_X, this.topPos + TOGGLE_Y,
                    203.0F, 0.0F, TOGGLE_W, TOGGLE_H, 256, 256);
        }

        // 1.7 scales to 114 but anchors at 113, so a full bar overdraws one pixel above the column. Preserved.
        int k = (int) te.getPowerScaled(114);
        if (k > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + POWER_X,
                    this.topPos + POWER_Y + POWER_H - k, 203.0F, (float) (17 + POWER_H - k), POWER_W, k, 256, 256);
        }

        // The in-GUI beam. NOTE the pow(2) gate: 1.7's server cost is 3^ordinal, so the GUI can show
        // a beam for one tick after the real one has already stopped. Preserved.
        boolean beam = te.power > TileEntityFEL.powerReq * Math.pow(2, te.mode.strength())
                && te.isOn && te.mode != EnumWavelengths.NULL && te.distance > 0;
        if (beam) {
            int color = te.mode != EnumWavelengths.VISIBLE
                    ? te.mode.guiColor
                    : java.awt.Color.HSBtoRGB(gameTime() / 50.0F, 0.5F, 1.0F) & 0xFFFFFF;
            int argb = 0xFF000000 | (color & 0xFFFFFF);
            // 1.7 draws two GL lines at y = 31.5 with glLineWidth(5): five framebuffer pixels thick, whatever the
            // GUI scale. The second segment starts at the screen's left edge (x = 0), not at the GUI's.
            float half = (float) (2.5 / Minecraft.getInstance().getWindow().getGuiScale());
            GuiDraw.pushMatrix(graphics);
            GuiDraw.translate(graphics, 0F, this.topPos + 31.5F);
            GuiDraw.scale(graphics, 1F / 8F, 1F / 8F);
            int h = Math.max(1, Math.round(half * 8F));
            graphics.fill((this.leftPos + 113) * 8, -h, (this.leftPos + 135) * 8, h, argb);
            graphics.fill(0, -h, (this.leftPos + 4) * 8, h, argb);
            GuiDraw.popMatrix(graphics);
        }
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int nameWidth = this.font.width(this.title);
        GuiDraw.text(graphics, this.font, this.title, 90 + this.imageWidth / 2 - nameWidth / 2, 7, 0xffffff, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 98, 4210752, false);

        TileEntityFEL te = this.menu.getFel();
        if (te == null) return;

        // 1.7 centres both status words on the width of the machine's name, not their own.
        if (te.missingValidSilex && te.isOn) {
            GuiDraw.text(graphics, this.font, Component.literal("ERR."), 55 + this.imageWidth / 2 - nameWidth / 2, 9, 0xFF0000, false);
        } else if (te.isOn) {
            GuiDraw.text(graphics, this.font, Component.literal("LIVE"), 54 + this.imageWidth / 2 - nameWidth / 2, 9, 0x00FF00, false);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFEL te = this.menu.getFel();
        if (te == null) return;

        if (this.isHovering(POWER_X, POWER_Y, POWER_W, POWER_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(te.power, TileEntityFEL.maxPower), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int x = (int) event.x() - this.leftPos;
        int y = (int) event.y() - this.topPos;
        // 1.7 hit box: x in [142, 171), y in (41, 58].
        if (x >= TOGGLE_X && x < TOGGLE_X + TOGGLE_W && y > TOGGLE_Y && y <= TOGGLE_Y + TOGGLE_H) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                GuiUtil.playClickSound();
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                return true;
            }
        }
        return false;
    }
}
