package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerReactorZirnox;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityReactorZirnox;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** ZIRNOX reactor control panel. */
public class GUIReactorZirnox extends dev.golem.core.gui.CompatContainerScreen<ContainerReactorZirnox> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_zirnox.png");
    private static final int TOGGLE_X = 144;
    private static final int TOGGLE_Y = 35;
    private static final int TOGGLE_SIZE = 14;
    private static final int VENT_X = 151;
    private static final int VENT_Y = 51;
    private static final int VENT_SIZE = 36;

    public GUIReactorZirnox(ContainerReactorZirnox menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 203, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        TileEntityReactorZirnox reactor = this.menu.getReactor();
        if (reactor == null) return;

        drawGauge(graphics, 160, 108, 238, 12 * tankLevel(reactor.steam), 18, 12);
        drawGauge(graphics, 142, 108, 238, 12 * tankLevel(reactor.carbonDioxide), 18, 12);
        drawGauge(graphics, 178, 108, 238, 12 * tankLevel(reactor.water), 18, 12);
        drawGauge(graphics, 160, 33, 220, 18 * scaled(reactor.heat, TileEntityReactorZirnox.MAX_HEAT, 12), 18, 17);
        drawGauge(graphics, 178, 33, 220, 18 * scaled(reactor.pressure, TileEntityReactorZirnox.MAX_PRESSURE, 12), 18, 17);

        if (reactor.isOn) {
            for (int column = 0; column < 4; column++) {
                for (int row = 0; row < 4; row++) {
                    drawGauge(graphics, 7 + 36 * column, 15 + 36 * row, 238, 238, 18, 18);
                }
            }
            for (int column = 0; column < 3; column++) {
                for (int row = 0; row < 3; row++) {
                    drawGauge(graphics, 25 + 36 * column, 33 + 36 * row, 238, 238, 18, 18);
                }
            }
            drawGauge(graphics, 142, 15, 220, 238, 18, 18);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, 160, 0xFF404040, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityReactorZirnox reactor = this.menu.getReactor();
        if (reactor == null) return;
        if (this.isHovering(TOGGLE_X, TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE, mouseX, mouseY)) {
            tooltip(graphics, Component.translatable("gui.zirnox.toggle"), mouseX, mouseY);
        } else if (this.isHovering(VENT_X, VENT_Y, VENT_SIZE, VENT_SIZE, mouseX, mouseY)) {
            tooltip(graphics, Component.translatable("gui.zirnox.vent"), mouseX, mouseY);
        } else if (this.isHovering(142, 108, 18, 12, mouseX, mouseY)) {
            tankTooltip(graphics, reactor.carbonDioxide, mouseX, mouseY);
        } else if (this.isHovering(160, 108, 18, 12, mouseX, mouseY)) {
            tankTooltip(graphics, reactor.steam, mouseX, mouseY);
        } else if (this.isHovering(178, 108, 18, 12, mouseX, mouseY)) {
            tankTooltip(graphics, reactor.water, mouseX, mouseY);
        } else if (this.isHovering(160, 33, 18, 17, mouseX, mouseY)) {
            tooltip(graphics, Component.translatable("gui.zirnox.temperature",
                    Math.round(reactor.heat * 0.00001F * 780 + 20)), mouseX, mouseY);
        } else if (this.isHovering(178, 33, 18, 17, mouseX, mouseY)) {
            tooltip(graphics, Component.translatable("gui.zirnox.pressure",
                    Math.round(reactor.pressure * 0.00001F * 30)), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int button = this.isHovering(TOGGLE_X, TOGGLE_Y, TOGGLE_SIZE, TOGGLE_SIZE, event.x(), event.y()) ? 0
                : this.isHovering(VENT_X, VENT_Y, VENT_SIZE, VENT_SIZE, event.x(), event.y()) ? 1 : -1;
        if (button >= 0 && Minecraft.getInstance().gameMode != null) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.menu.containerId, button);
            return true;
        }
        return false;
    }

    private void drawGauge(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + x, this.topPos + y,
                (float) u, (float) v, width, height, 256, 256);
    }

    private static int tankLevel(FluidTankNTM tank) {
        return scaled(tank.getFill(), tank.getMaxFill(), 6);
    }

    private static int scaled(int value, int maximum, int steps) {
        if (maximum <= 0) return 0;
        return Math.max(0, Math.min(steps, value * steps / maximum));
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, List.of(Component.literal(tank.getTankType().getName()),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
    }

    private void tooltip(GuiGraphicsExtractor graphics, Component text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, List.of(text), Optional.empty(), mouseX, mouseY);
    }
}
