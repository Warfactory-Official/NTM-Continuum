package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKControl;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.RBMKColor;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class ScreenRBMKControl extends RBMKContainerScreen<MenuRBMKControl> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/rbmk/gui_rbmk_control.png");

    public ScreenRBMKControl(MenuRBMKControl menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        texture(graphics, TEX);
        BlockEntityRBMKControl control = menu.blockEntity();
        double level = control == null ? 0 : control.level;
        int height = (int) (56 * (1D - level));
        if (height > 0) GuiDraw.blit(graphics, TEX, leftPos + 75, topPos + 29, 176, 56 - height, 8, height, 256, 256);
        if (control != null && control.isPowered()) {
            GuiDraw.blit(graphics, TEX, leftPos + 87, topPos + 21, 196, control.hasPower ? 16 : 0, 16, 16, 256, 256);
        }
        if (control instanceof BlockEntityRBMKControlManual manual) {
            if (manual.color != null) GuiDraw.blit(graphics, TEX, leftPos + 28, topPos + 26 + manual.color.ordinal() * 11,
                    184, manual.color.ordinal() * 10, 12, 10, 256, 256);
        }
    }

    @Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityRBMKControl control = menu.blockEntity();
        if (control == null) return;
        if (region(71, 29, 16, 56, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal((int) (control.level * 100) + "% withdrawn")), mouseX, mouseY);
        } else if (control.isPowered() && region(87, 21, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal(control.power + " / " + BlockEntityRBMKControl.maxPower + " HE")), mouseX, mouseY);
        } else if (control instanceof BlockEntityRBMKControlManual) {
            for (int i = 0; i < 5; i++) if (region(28, 26 + i * 11, 12, 10, mouseX, mouseY)) {
                tooltip(graphics, List.of(Component.literal("Assign colour group " + (i + 1))), mouseX, mouseY);
                return;
            }
        }
    }

    @Override protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        if (event.button() != 0) return false;
        BlockEntityRBMKControlManual control = menu.blockEntity();
        if (control == null) return false;
        for (int i = 0; i < 5; i++) {
            if (region(118, 26 + i * 11, 30, 10, (int) event.x(), (int) event.y())) {
                send("level", 1D - i * .25D);
                return true;
            }
        }
        for (int i = 0; i < RBMKColor.VALUES.length; i++) if (region(28, 26 + i * 11, 12, 10, (int) event.x(), (int) event.y())) { sendColor(i); return true; }
        return false;
    }

    private void send(String key, double value) {
        CompoundTag data = new CompoundTag();
        data.putDouble(key, value);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(menu.blockEntity().getBlockPos(), data));
    }

    private void sendColor(int color) {
        CompoundTag data = new CompoundTag();
        data.putInt("color", color);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(menu.blockEntity().getBlockPos(), data));
    }
}
