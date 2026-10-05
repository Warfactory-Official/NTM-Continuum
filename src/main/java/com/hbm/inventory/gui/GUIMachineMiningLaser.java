package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineMiningLaser;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.net.GolemNetwork;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the mining laser (1.7 {@code GUIMiningLaser}, {@code machine/gui_laser_miner.png}, 176x222). Reads the synced
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

    /** 1.7's hard-coded acceptable-upgrades text block, verbatim (1.7 does not list the Scream upgrade it accepts). */
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
            Component.literal(" -Nullifier"));

    public GUIMachineMiningLaser(ContainerMachineMiningLaser menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 GUIMiningLaser: centred title at y=4, "Inventory", then the mining-square width readout.
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 4, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
        TileEntityMachineMiningLaser te = this.menu.getLaser();
        if (te != null) {
            String width = "" + te.getWidth();
            GuiDraw.text(graphics, this.font, width, 43 - this.font.width(width) / 2, 26, 0xffffff, false);
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
        GuiUtil.drawInfoPanel(graphics, this.leftPos + INFO_X, this.topPos + INFO_Y, 8);

        // Oil tank column: 1.7 renderTank(35, 124, z, 7, 52).
        GuiUtil.tank(graphics, te.tank, this.leftPos + FLUID_X, this.topPos + FLUID_TOP + FLUID_H, FLUID_W, FLUID_H);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineMiningLaser te = this.menu.getLaser();
        if (te == null) return;

        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 8, this.topPos + 106 - 88, 16, 88)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineMiningLaser.maxPower), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, this.leftPos + INFO_X, this.topPos + INFO_Y, 8, 8)) {
            // 1.7 draws this block at a fixed spot, (guiLeft + 141, guiTop + 39 + 16), not at the mouse.
            GuiDraw.tooltip(graphics, this.font, UPGRADE_TEXT, Optional.empty(), this.leftPos + 141, this.topPos + 39 + 16);
        } else if (GuiUtil.hover(mouseX, mouseY, this.leftPos + FLUID_X, this.topPos + FLUID_TOP, FLUID_W, FLUID_H)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        TileEntityMachineMiningLaser te = this.menu.getLaser();
        if (te != null && GuiUtil.hover(event.x(), event.y(), this.leftPos + BTN_X, this.topPos + BTN_Y, BTN_W, BTN_H)) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
