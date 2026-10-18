package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerLaunchpadSoyuz;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz;
import com.hbm.tileentity.machine.TileEntityLaunchpadSoyuz.SoyuzStatus;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.7 {@code GUILaunchpadSoyuz}: {@code gui_launchpad_soyuz.png} at 194x244. Power and both tanks with their ready
 * lamps, the CARGO / SATELLITE switch at (79,52)/(97,52), the launch button at (88,97) usable once READY, the slot
 * lamps, and the state below it: the red {@code SS:CC} clock while counting down, otherwise IDLE, LOADING,
 * FUELING or READY.
 */
public class GUILaunchpadSoyuz extends CompatContainerScreen<ContainerLaunchpadSoyuz> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_launchpad_soyuz.png");

    public GUILaunchpadSoyuz(ContainerLaunchpadSoyuz menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 194, 244);
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + y, (float) u, (float) v, w, h, 256, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityLaunchpadSoyuz pad = menu.getLauncher();
        if (pad == null) return;

        int power = (int) (pad.power * 52 / TileEntityLaunchpadSoyuz.maxPower);
        if (power > 0) blit(graphics, 134, 96 - power, 194, 52 - power, 16, power);

        blit(graphics, 97 - (pad.cargoMode ? 18 : 0), 52, 228 - (pad.cargoMode ? 18 : 0), 26, 18, 18);

        blit(graphics, 157, 31, pad.hasJetFuel() ? 210 : 216, 0, 6, 8);
        blit(graphics, 175, 31, pad.hasOxidizer() ? 210 : 216, 0, 6, 8);
        blit(graphics, 139, 31, pad.power >= TileEntityLaunchpadSoyuz.CONSUMPTION ? 210 : 216, 0, 6, 8);

        int l = pad.orbital();
        if (l > 0) blit(graphics, 79, 25, 210 + (l - 1) * 18, 8, 18, 18);

        blit(graphics, 97, 79, pad.loadedType == -1 ? 210 : 228, 8, 18, 18);

        if (!pad.cargoMode) {
            blit(graphics, 97, 25, pad.getItem(2).isEmpty() ? 210 : 228, 8, 18, 18);
        } else {
            blit(graphics, 79, 79, pad.getItem(1).isEmpty() ? 210 : 228, 8, 18, 18);
        }

        if (pad.soyuzStatus == SoyuzStatus.LAUNCHING) blit(graphics, 88, 97, 210, 44, 18, 18);

        GuiUtil.tank(graphics, pad.tanks[0], leftPos + 152, topPos + 96, 16, 52);
        GuiUtil.tank(graphics, pad.tanks[1], leftPos + 170, topPos + 96, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 4, 0xFFFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 17, imageHeight - 96 + 2, 0xFF404040, false);

        TileEntityLaunchpadSoyuz pad = menu.getLauncher();
        if (pad == null) return;

        if (pad.soyuzStatus == SoyuzStatus.LAUNCHING) {
            // 1.7: seconds padded in front, hundredths padded behind
            String secs = "" + pad.countdown / 20;
            String cents = "" + (pad.countdown % 20) * 5;
            if (secs.length() == 1) secs = "0" + secs;
            if (cents.length() == 1) cents += "0";
            GuiDraw.text(graphics, font, Component.literal(secs + ":" + cents), 85, 121, 0xFFFF0000, false);
        } else {
            String key = switch (pad.soyuzStatus) {
                case LOADING -> "desc.gui.soyuz.loading";
                case FUELING -> "desc.gui.soyuz.fueling";
                case READY -> "desc.gui.soyuz.ready";
                default -> "desc.gui.soyuz.idle";
            };
            int color = switch (pad.soyuzStatus) {
                case LOADING -> 0xFFFF8000;
                case FUELING -> 0xFFFFFF00;
                case READY -> 0xFF00FF00;
                default -> 0xFFFF0000;
            };
            constrainedLabel(graphics, font, I18n.get(key), 97, 125, color);
        }
    }

    /** 1.7 {@code GuiInfoContainer.drawConstrainedLabel}: centred on (x, y), shrunk to fit 22 px. */
    static void constrainedLabel(GuiGraphicsExtractor graphics, Font font, String label, int x, int y, int color) {
        float scale = Math.min(1F, 22F / font.width(label));
        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, scale, scale);
        GuiDraw.text(graphics, font, Component.literal(label), (int) (x / scale - font.width(label) / 2F),
                (int) (y / scale - font.lineHeight / 2F), color, false);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityLaunchpadSoyuz pad = menu.getLauncher();
        if (pad == null) return;

        if (isHovering(152, 44, 16, 52, mouseX, mouseY)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(pad.tanks[0]), mouseX, mouseY);
        } else if (isHovering(170, 44, 16, 52, mouseX, mouseY)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(pad.tanks[1]), mouseX, mouseY);
        } else if (isHovering(134, 44, 16, 52, mouseX, mouseY)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(pad.power, TileEntityLaunchpadSoyuz.maxPower), mouseX, mouseY);
        } else if (isHovering(79, 52, 18, 18, mouseX, mouseY)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.keyArray("desc.gui.soyuz.cargo"), mouseX, mouseY);
        } else if (isHovering(97, 52, 18, 18, mouseX, mouseY)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.keyArray("desc.gui.soyuz.satellite"), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityLaunchpadSoyuz pad = menu.getLauncher();
        if (pad == null) return super.mouseClicked(event, doubleClick);
        double x = event.x();
        double y = event.y();
        CompoundTag data = null;

        if (leftPos + 79 <= x && leftPos + 79 + 18 > x && topPos + 52 < y && topPos + 52 + 18 >= y) {
            data = new CompoundTag();
            data.putBoolean("cargo", true);
        }
        if (leftPos + 97 <= x && leftPos + 97 + 18 > x && topPos + 52 < y && topPos + 52 + 18 >= y) {
            data = new CompoundTag();
            data.putBoolean("cargo", false);
        }
        if (leftPos + 88 <= x && leftPos + 88 + 18 > x && topPos + 97 < y && topPos + 97 + 18 >= y
                && pad.soyuzStatus == SoyuzStatus.READY) {
            data = new CompoundTag();
            data.putBoolean("launch", true);
        }

        if (data == null) return super.mouseClicked(event, doubleClick);
        GuiUtil.playClickSound();
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(pad.getBlockPos(), data));
        return true;
    }
}
