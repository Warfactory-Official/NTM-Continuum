package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerWatz;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityWatz;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
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
 * 1.7 {@code GUIWatz}. The pellet panel tints red with the log of the reactor's heat; the three thin bars are the
 * column's cold coolant, hot coolant and mud, and the padlock toggles the per-slot pellet filter.
 */
public class GUIWatz extends CompatContainerScreen<ContainerWatz> {

    private static final Identifier TEXTURE =
            Ids.of("hbm", "textures/gui/reactors/gui_watz.png");

    public GUIWatz(ContainerWatz menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 229);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.leftPos;
        int y = this.topPos;
        TileEntityWatz watz = this.menu.getWatz();
        int heat = watz == null ? 0 : watz.heat;

        float col = Mth.clamp(1F - (float) Math.log(heat / 100_000D + 1D) * 0.4F, 0F, 1F);
        int channel = Math.round(col * 255F);
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, 131, 122, 256, 256,
                0xFF000000 | 0xFF0000 | (channel << 8) | channel);

        GuiDraw.blit(graphics, TEXTURE, x + 131, y, 131F, 0F, 36, 122, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, x, y + 130, 0F, 130F, this.imageWidth, 99, 256, 256);

        // Before 26.1 a tinted blit is lifted above later draws; the parts lying on the panel must go above it.
        GuiDraw.pushMatrix(graphics);
        GuiDraw.nextStratum(graphics);
        GuiDraw.blit(graphics, TEXTURE, x + 126, y + 31, 176F, 31F, 9, 60, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, x + 105, y + 96, 185F, 26F, 30, 26, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, x + 9, y + 96, 184F, 0F, 26, 26, 256, 256);
        if (watz != null) GuiUtil.drawSmoothGauge(graphics, x + 22, y + 109, 1D - col, 5, 2, 1, 0x7F0000);
        GuiDraw.popMatrix(graphics);

        if (watz == null) return;
        if (watz.isOn) GuiDraw.blit(graphics, TEXTURE, x + 147, y + 8, 176F, 0F, 8, 8, 256, 256);
        if (watz.isLocked) GuiDraw.blit(graphics, TEXTURE, x + 142, y + 70, 210F, 0F, 18, 18, 256, 256);

        FluidTankNTM[] tanks = watz.getDisplayTanks();
        for (int i = 0; i < tanks.length; i++) GuiUtil.tank(graphics, tanks[i], x + 143 + i * 6, y + 69, 4, 43);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 93, 0x404040, false);

        TileEntityWatz watz = this.menu.getWatz();
        if (watz == null) return;
        float scale = 1.25F;
        String flux = String.format(Locale.US, "%,.1f", watz.fluxDisplay);
        GuiDraw.pushMatrix(graphics);
        GuiDraw.nextStratum(graphics);
        GuiDraw.scale(graphics, 1F / scale, 1F / scale);
        GuiDraw.text(graphics, this.font, flux, (int) (161 * scale - this.font.width(flux)), (int) (107 * scale), 0x00FF00, false);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityWatz watz = this.menu.getWatz();
        if (watz == null) return;
        int x = this.leftPos;
        int y = this.topPos;

        if (GuiUtil.hover(mouseX, mouseY, x + 13, y + 100, 18, 18)) {
            tooltip(graphics, List.of(Component.literal(String.format(Locale.US, "%,d", watz.heat) + " TU")), mouseX, mouseY);
        }
        if (GuiUtil.hover(mouseX, mouseY, x + 143, y + 71, 16, 16)) {
            tooltip(graphics, List.of(Component.translatable(watz.isLocked ? "gui.watz.unlock" : "gui.watz.lock")), mouseX, mouseY);
        }
        FluidTankNTM[] tanks = watz.getDisplayTanks();
        for (int i = 0; i < tanks.length; i++) {
            if (GuiUtil.hover(mouseX, mouseY, x + 142 + i * 6, y + 23, 6, 45)) {
                tooltip(graphics, GuiUtil.tankTooltip(tanks[i]), mouseX, mouseY);
            }
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityWatz watz = this.menu.getWatz();
        int x = this.leftPos;
        int y = this.topPos;
        if (watz == null || !(x + 142 <= event.x() && x + 142 + 18 > event.x() && y + 70 < event.y() && y + 70 + 18 >= event.y())) {
            return false;
        }

        CompoundTag data = new CompoundTag();
        data.putBoolean("lock", true);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(watz.getBlockPos(), data));
        GuiUtil.playClickSound();
        return true;
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, lines, Optional.empty(), mouseX, mouseY);
    }
}
