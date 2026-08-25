package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerWatz;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityWatz;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Watz power plant control panel. The whole upper panel tints red with the log of the reactor's heat,
 * which is the operator's main at-a-glance warning; the three thin bars on the right are the shared
 * column volumes (cold coolant, hot coolant, mud) and the padlock toggles the per-slot pellet filter.
 */
public class GUIWatz extends dev.golem.core.gui.CompatContainerScreen<ContainerWatz> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_watz.png");

    private static final int LOCK_X = 142;
    private static final int LOCK_Y = 70;
    private static final int LOCK_SIZE = 18;
    private static final int HEAT_X = 13;
    private static final int HEAT_Y = 100;
    private static final int TANK_Y = 69;
    private static final int TANK_HEIGHT = 43;

    public GUIWatz(ContainerWatz menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 229);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        TileEntityWatz watz = this.menu.getWatz();
        int heat = watz == null ? 0 : watz.heat;

        // The panel reddens with heat: 1 at cold, approaching 0 as heat climbs (log scale).
        float col = Mth.clamp(1F - (float) Math.log(heat / 100_000D + 1D) * 0.4F, 0F, 1F);
        int channel = Math.round(col * 255F);
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, 131, 122, 256, 256,
                0xFF000000 | 0xFF0000 | (channel << 8) | channel);

        GuiDraw.blit(graphics, TEXTURE, x + 131, y, 131F, 0F, 36, 122, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, x, y + 130, 0F, 130F, this.imageWidth, 99, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, x + 126, y + 31, 176F, 31F, 9, 60, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, x + 105, y + 96, 185F, 26F, 30, 26, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, x + 9, y + 96, 184F, 0F, 26, 26, 256, 256);

        if (watz == null) return;
        if (watz.isOn) GuiDraw.blit(graphics, TEXTURE, x + 147, y + 8, 176F, 0F, 8, 8, 256, 256);
        if (watz.isLocked) GuiDraw.blit(graphics, TEXTURE, x + LOCK_X, y + LOCK_Y, 210F, 0F, 18, 18, 256, 256);

        // Heat gauge: a simple vertical fill, standing in for the old GUIElements round gauge.
        int filled = Math.round((1F - col) * 16F);
        if (filled > 0) {
            GuiDraw.fill(graphics, x + HEAT_X + 1, y + HEAT_Y + 17 - filled,
                    x + HEAT_X + 17, y + HEAT_Y + 17, 0xFFFF4C1A);
        }

        FluidTankNTM[] tanks = watz.getDisplayTanks();
        for (int i = 0; i < tanks.length; i++) {
            drawTank(graphics, tanks[i], x + 143 + i * 6, y + TANK_Y);
        }
    }

    /** A 4×43 column filling upward from the bottom, tinted with the fluid's colour. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        if (tank.getMaxFill() <= 0 || tank.getFill() <= 0) return;
        int height = Math.max(1, tank.getFill() * TANK_HEIGHT / tank.getMaxFill());
        GuiDraw.fill(graphics, x, y + TANK_HEIGHT - height, x + 4, y + TANK_HEIGHT,
                0xFF000000 | tank.getTankType().getColor());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 93, 0xFF404040, false);

        TileEntityWatz watz = this.menu.getWatz();
        if (watz == null) return;
        String flux = String.format(Locale.US, "%,.1f", watz.fluxDisplay);
        GuiDraw.text(graphics, this.font, flux, 161 - this.font.width(flux), 107, 0xFF00FF00, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityWatz watz = this.menu.getWatz();
        if (watz == null) return;

        if (this.isHovering(HEAT_X, HEAT_Y, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, Component.literal(String.format(Locale.US, "%,d", watz.heat) + " TU"), mouseX, mouseY);
            return;
        }
        if (this.isHovering(143, TANK_Y - 46, 16, 16, mouseX, mouseY)) return;
        if (this.isHovering(LOCK_X + 1, LOCK_Y + 1, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, Component.translatable(watz.isLocked
                    ? "gui.watz.unlock" : "gui.watz.lock"), mouseX, mouseY);
            return;
        }
        FluidTankNTM[] tanks = watz.getDisplayTanks();
        for (int i = 0; i < tanks.length; i++) {
            if (this.isHovering(143 + i * 6, TANK_Y, 6, TANK_HEIGHT, mouseX, mouseY)) {
                tankTooltip(graphics, tanks[i], mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityWatz watz = this.menu.getWatz();
        if (watz == null || !this.isHovering(LOCK_X, LOCK_Y, LOCK_SIZE, LOCK_SIZE, event.x(), event.y())) return false;

        CompoundTag data = new CompoundTag();
        data.putBoolean("lock", true);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(watz.getBlockPos(), data));
        return true;
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, List.of(tank.getTankType().getDisplayName(),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
    }

    private void tooltip(GuiGraphicsExtractor graphics, Component text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, List.of(text), Optional.empty(), mouseX, mouseY);
    }
}
