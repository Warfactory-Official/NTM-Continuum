package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKControlAuto;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class ScreenRBMKControlAuto extends RBMKContainerScreen<MenuRBMKControlAuto> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/rbmk/gui_rbmk_control_auto.png");
    private final EditBox[] fields = new EditBox[4];

    public ScreenRBMKControlAuto(MenuRBMKControlAuto menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); }

    @Override protected void init() {
        super.init();
        BlockEntityRBMKControlAuto control = menu.blockEntity();
        for (int i = 0; i < fields.length; i++) {
            EditBox field = new EditBox(font, leftPos + 30, topPos + 27 + i * 11, 26, 10, Component.empty());
            field.setBordered(false);
            field.setMaxLength(i < 2 ? 3 : 4);
            field.setTextColor(0xFFFFFFFF);
            addRenderableWidget(field);
            fields[i] = field;
        }
        if (control != null) {
            fields[0].setValue(String.valueOf((int) control.levelUpper));
            fields[1].setValue(String.valueOf((int) control.levelLower));
            fields[2].setValue(String.valueOf((int) control.heatUpper));
            fields[3].setValue(String.valueOf((int) control.heatLower));
        }
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        texture(graphics, TEX);
        BlockEntityRBMKControlAuto control = menu.blockEntity();
        if (control == null) return;
        int height = (int) (56 * (1D - control.level));
        if (height > 0) GuiDraw.blit(graphics, TEX, leftPos + 124, topPos + 29, 176, 56 - height, 8, height, 256, 256);
        GuiDraw.blit(graphics, TEX, leftPos + 59, topPos + 27, 184, control.function.ordinal() * 19, 26, 19, 256, 256);
        if (control.isPowered()) {
            GuiDraw.blit(graphics, TEX, leftPos + 136, topPos + 21, 210, control.hasPower ? 16 : 0, 16, 16, 256, 256);
        }
    }

    @Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityRBMKControlAuto control = menu.blockEntity();
        if (control == null) return;
        if (region(58, 26, 28, 19, mouseX, mouseY)) {
            String function = switch (control.function) { case LINEAR -> "Linear"; case QUAD_UP -> "Quadratic"; case QUAD_DOWN -> "Inverse Quadratic"; };
            tooltip(graphics, List.of(Component.literal("Function: " + function)), mouseX, mouseY);
        } else if (region(124, 29, 8, 56, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal((int) (control.level * 100) + "% withdrawn")), mouseX, mouseY);
        } else if (control.isPowered() && region(136, 21, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal(control.power + " / " + BlockEntityRBMKControl.maxPower + " HE")), mouseX, mouseY);
        } else if (region(28, 26, 30, 10, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Level at max heat"), Component.literal("Should be smaller than level at min heat")), mouseX, mouseY);
        } else if (region(28, 37, 30, 10, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Level at min heat"), Component.literal("Should be larger than level at min heat")), mouseX, mouseY);
        } else if (region(28, 48, 30, 10, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Max heat"), Component.literal("Must be larger than min heat")), mouseX, mouseY);
        } else if (region(28, 59, 30, 10, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Min heat"), Component.literal("Must be smaller than max heat")), mouseX, mouseY);
        } else if (region(28, 70, 30, 10, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Save parameters")), mouseX, mouseY);
        } else for (int i = 0; i < 3; i++) if (region(61, 48 + i * 11, 22, 10, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal(i == 0 ? "Select linear interpolation" : i == 1 ? "Select quadratic interpolation" : "Select inverse quadratic interpolation")), mouseX, mouseY);
            return;
        }
    }

    @Override protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        if (event.button() != 0) return false;
        if (region(28, 70, 30, 10, (int) event.x(), (int) event.y())) {
            saveParameters();
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (region(61, 48 + i * 11, 22, 10, (int) event.x(), (int) event.y())) {
                CompoundTag data = new CompoundTag();
                data.putInt("function", i);
                send(data);
                return true;
            }
        }
        return false;
    }

    private void saveParameters() {
        double[] values = new double[4];
        for (int i = 0; i < values.length; i++) {
            double maximum = i < 2 ? 100 : 9999;
            try {
                values[i] = Mth.clamp(Double.parseDouble(fields[i].getValue()), 0, maximum);
            } catch (NumberFormatException ignored) {
                values[i] = 0;
            }
            fields[i].setValue(String.valueOf((int) values[i]));
        }
        CompoundTag data = new CompoundTag();
        data.putDouble("levelUpper", values[0]);
        data.putDouble("levelLower", values[1]);
        data.putDouble("heatUpper", values[2]);
        data.putDouble("heatLower", values[3]);
        send(data);
    }

    private void send(CompoundTag data) {
        BlockEntityRBMKControlAuto control = menu.blockEntity();
        if (control != null) GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(control.getBlockPos(), data));
    }
}
