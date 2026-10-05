package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerReactorZirnox;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import com.hbm.sound.ModSounds;

/**
 * ZIRNOX reactor control panel, ported from 1.7 {@code GUIReactorZirnox} (203x256): three sliding linear
 * gauges for CO2, steam and water, two needle gauges for heat and pressure, the lit rod grid while running,
 * and the info/warning panels hanging off the left edge.
 */
public class GUIReactorZirnox extends dev.golem.core.gui.CompatContainerScreen<ContainerReactorZirnox> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_zirnox.png");
    /** Supersample factor the polygon gauges are rasterised at before the pose scales them back down. */
    private static final int SUPERSAMPLE = 8;

    public GUIReactorZirnox(ContainerReactorZirnox menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 203, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityReactorZirnox reactor = this.menu.getReactor();
        if (reactor == null) return;

        drawLinearGauge(graphics, x + 162, y + 114, fraction(reactor.steam), 2, 5, 0.75, 14, 0x7F0000);
        drawLinearGauge(graphics, x + 144, y + 114, fraction(reactor.carbonDioxide), 2, 5, 0.75, 14, 0x7F0000);
        drawLinearGauge(graphics, x + 180, y + 114, fraction(reactor.water), 2, 5, 0.75, 14, 0x7F0000);

        GuiUtil.drawSmoothGauge(graphics, x + 169, y + 42, (double) reactor.heat / 100000, 5, 2, 1, 0x7F0000);
        GuiUtil.drawSmoothGauge(graphics, x + 187, y + 42, (double) reactor.pressure / 100000, 5, 2, 1, 0x7F0000);

        if (reactor.isOn) {
            for (int column = 0; column < 4; column++) {
                for (int row = 0; row < 4; row++) {
                    blit(graphics, 7 + 36 * column, 15 + 36 * row, 238, 238, 18, 18);
                }
            }
            for (int column = 0; column < 3; column++) {
                for (int row = 0; row < 3; row++) {
                    blit(graphics, 25 + 36 * column, 33 + 36 * row, 238, 238, 18, 18);
                }
            }
            blit(graphics, 142, 15, 220, 238, 18, 18);
        }

        GuiUtil.drawInfoPanel(graphics, x - 16, y + 36, 2);
        GuiUtil.drawInfoPanel(graphics, x - 16, y + 36 + 16, 3);
        if (reactor.water.getFill() <= 0) GuiUtil.drawInfoPanel(graphics, x - 16, y + 36 + 32, 6);
        // 1.7 draws this panel at <= 4000 but only shows its tooltip at < 4000; both kept.
        if (reactor.carbonDioxide.getFill() <= 4000) GuiUtil.drawInfoPanel(graphics, x - 16, y + 36 + 32 + 16, 6);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        // 1.7 uses ySize - 96 here, not the usual - 94.
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityReactorZirnox reactor = this.menu.getReactor();
        if (reactor == null) return;

        if (this.isHovering(160, 108, 18, 12, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(reactor.steam), mouseX, mouseY);
        } else if (this.isHovering(142, 108, 18, 12, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(reactor.carbonDioxide), mouseX, mouseY);
        } else if (this.isHovering(178, 108, 18, 12, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(reactor.water), mouseX, mouseY);
        } else if (this.isHovering(160, 33, 18, 17, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Temperature:"),
                    Component.literal("   " + Math.round((reactor.heat) * 0.00001 * 780 + 20) + "°C")), mouseX, mouseY);
        } else if (this.isHovering(178, 33, 18, 17, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Pressure:"),
                    Component.literal("   " + Math.round((reactor.pressure) * 0.00001 * 30) + " bar")), mouseX, mouseY);
        } else if (this.isHovering(-16, 36, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, lines("desc.gui.zirnox.coolant"), mouseX, mouseY);
        } else if (this.isHovering(-16, 36 + 16, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, lines("desc.gui.zirnox.pressure"), mouseX, mouseY);
        } else if (reactor.water.getFill() <= 0 && this.isHovering(-16, 36 + 32, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, lines("desc.gui.zirnox.warning1"), mouseX, mouseY);
        } else if (reactor.carbonDioxide.getFill() < 4000 && this.isHovering(-16, 36 + 48, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, lines("desc.gui.zirnox.warning2"), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int x = (int) event.x() - this.leftPos;
        int y = (int) event.y() - this.topPos;
        // 1.7 hit boxes: control x in [144, 158), y in (35, 49]; vent x in [151, 187), y in (51, 87].
        int button = x >= 144 && x < 158 && y > 35 && y <= 49 ? 0
                : x >= 151 && x < 187 && y > 51 && y <= 87 ? 1 : -1;
        if (button >= 0 && Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, button);
            Minecraft.getInstance().getSoundManager().play(
                    SimpleSoundInstance.forUI(ModSounds.RBMK_AZ5_COVER.get(), 0.5F, 1.0F));
            return true;
        }
        return false;
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + x, this.topPos + y, (float) u, (float) v, width, height, 256, 256);
    }

    private static double fraction(FluidTankNTM tank) {
        return tank.getMaxFill() <= 0 ? 0 : (double) tank.getFill() / tank.getMaxFill();
    }

    /**
     * 1.7 {@code GUIElements.drawSmoothLinearGauge} at rotation 0: an upward pointer (a tip of {@code tipLength}
     * above, a {@code 2 * backSide} wide stem of {@code backLength} below) slid right by {@code progress * scale}.
     * The black outline is the same shape at 1.5x, except that 1.7 forgets to scale the stem's length.
     */
    private static void drawLinearGauge(GuiGraphicsExtractor graphics, int x, int y, double progress,
                                        double tipLength, double backLength, double backSide, double scale, int color) {
        progress = Math.max(0, Math.min(1, progress)) * Math.max(scale, 1);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, (float) (x + progress), (float) y);
        GuiDraw.scale(graphics, 1F / SUPERSAMPLE, 1F / SUPERSAMPLE);
        fillPointer(graphics, tipLength * 1.5, backLength, backSide * 1.5, 0xFF000000);
        fillPointer(graphics, tipLength, backLength, backSide, 0xFF000000 | (color & 0xFFFFFF));
        GuiDraw.popMatrix(graphics);
    }

    private static void fillPointer(GuiGraphicsExtractor graphics, double tipLength, double backLength, double backSide,
                                    int color) {
        double tip = tipLength * SUPERSAMPLE;
        double side = backSide * SUPERSAMPLE;
        int top = (int) Math.round(-tip);
        for (int row = top; row < 0; row++) {
            int halfWidth = (int) Math.round(side * (row + 0.5 + tip) / tip);
            if (halfWidth > 0) graphics.fill(-halfWidth, row, halfWidth, row + 1, color);
        }
        int half = (int) Math.round(side);
        graphics.fill(-half, 0, half, (int) Math.round(backLength * SUPERSAMPLE), color);
    }

    /** 1.7 {@code I18nUtil.resolveKeyArray}: one translation split on {@code $}. */
    private static List<Component> lines(String key) {
        List<Component> lines = new ArrayList<>();
        for (String line : I18n.get(key).split("\\$")) lines.add(Component.literal(line));
        return lines;
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, text, Optional.empty(), mouseX, mouseY);
    }
}
