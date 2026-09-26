package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineMiningLaser;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.net.GolemNetwork;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the mining laser (1.12.2 {@code GUIMachineMiningLaser}, 176×222). Reads the synced
 * client {@link TileEntityMachineMiningLaser}: the on-button overlay, the 88-tall power bar, the
 * descending break-progress bar, the oil-tank column and the mining-square width number. The on/off
 * button sends a {@link ControlEventPayload} toggle (the old {@code AuxButtonPacket}).
 */
public class GUIMachineMiningLaser extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineMiningLaser> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_laser_miner.png");

    // On/off button hot-zone (GUI-relative), matching the 1.12.2 click region.
    private static final int BTN_X = 61, BTN_Y = 17, BTN_W = 18, BTN_H = 18;
    // Oil tank column: 7×52, bottom at y=124 (old renderTank(35, 124, …, 7, 52) draws upward).
    private static final int FLUID_X = 35, FLUID_TOP = 72, FLUID_W = 7, FLUID_H = 52;
    // "Acceptable upgrades" info glyph (old drawInfoPanel(87, 31, …, 8)).
    private static final int INFO_X = 87, INFO_Y = 31;

    /** The old GUI's hardcoded acceptable-upgrades text block (kept verbatim, incl. the deferred Scream). */
    private static final List<Component> UPGRADE_TEXT = List.of(
            Component.literal("Acceptable upgrades:"),
            Component.literal(" -Speed (stacks to level 12)"),
            Component.literal(" -Effectiveness (stacks to level 12)"),
            Component.literal(" -Overdrive (stacks to level 3)"),
            Component.literal(" -Fortune (stacks to level 3)"),
            Component.literal(" -Smelter (exclusive)"),
            Component.literal(" -Shredder (exclusive)"),
            Component.literal(" -Centrifuge (exclusive)"),
            Component.literal(" -Crystallizer (exclusive)"),
            Component.literal(" -Scream (4xSpeed, 4xCylces, 20xConsumption)"),
            Component.literal(" -Nullifier"));

    public GUIMachineMiningLaser(ContainerMachineMiningLaser menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2; // the old centered name
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        // The mining-square width readout (old drawGuiContainerForegroundLayer).
        TileEntityMachineMiningLaser te = this.menu.getLaser();
        if (te != null) {
            String width = "" + te.getWidth();
            GuiDraw.text(graphics, this.font, width, 43 - this.font.width(width) / 2, 26, 0xFFFFFFFF, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineMiningLaser te = this.menu.getLaser();
        if (te == null) return;

        // On-state button overlay: 18×18 at (61,17) from tex (200,0).
        if (te.isOn) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + BTN_X, this.topPos + BTN_Y, 200.0F, 0.0F, BTN_W, BTN_H, 256, 256);
        }

        // Power bar: 16×88 at (8, 18)..(8, 106), filling upward from tex (176, 88-i).
        int i = (int) te.getPowerScaled(88);
        if (i > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 8, this.topPos + 106 - i, 176.0F, (float) (88 - i), 16, i, 256, 256);
        }

        // Break-progress bar: 8 wide, growing downward from (66,36), tex (192,0).
        int j = te.getProgressScaled(34);
        if (j > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 66, this.topPos + 36, 192.0F, 0.0F, 8, j, 256, 256);
        }

        // "Acceptable upgrades" info glyph (small blue *).
        GuiUtil.drawUpgradeInfoPanel(graphics, this.leftPos + INFO_X, this.topPos + INFO_Y);

        // Oil tank column — a solid-colour level (the fluid's colour), bottom-anchored (port gauge convention).
        int max = te.tank.getMaxFill();
        int fh = max <= 0 ? 0 : (int) ((long) te.tank.getFill() * FLUID_H / max);
        if (fh > 0) {
            int x0 = this.leftPos + FLUID_X;
            int yBottom = this.topPos + FLUID_TOP + FLUID_H;
            graphics.fill(x0, yBottom - fh, x0 + FLUID_W, yBottom, 0xFF000000 | (te.tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineMiningLaser te = this.menu.getLaser();
        if (te == null) return;

        if (this.isHovering(8, 18, 16, 88, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineMiningLaser.maxPower), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(FLUID_X, FLUID_TOP, FLUID_W, FLUID_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    te.tank.getTankType().getDisplayName(),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(INFO_X, INFO_Y, GuiUtil.INFO_PANEL_SIZE, GuiUtil.INFO_PANEL_SIZE, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, UPGRADE_TEXT, Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineMiningLaser te = this.menu.getLaser();
        if (te != null && this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            Gauges.click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(te.getBlockPos(), data));
            return true;
        }
        return false;
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getLaser();
    }
}
