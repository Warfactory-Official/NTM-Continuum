package com.hbm.radio.client;

import dev.golem.core.gui.Gauges;
import com.hbm.packet.ControlEventPayload;
import com.hbm.radio.RadioTorchMenu;
import com.hbm.tileentity.network.TileEntityRadioTorchBase;
import com.hbm.tileentity.network.TileEntityRadioTorchSender;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Faithful 256x204 sender/receiver configuration panel. */
public final class RadioTorchScreen extends CompatContainerScreen<RadioTorchMenu> {
    private static final Identifier SENDER = Ids.of("hbm", "textures/gui/machine/gui_rtty_sender.png");
    private static final Identifier RECEIVER = Ids.of("hbm", "textures/gui/machine/gui_rtty_receiver.png");

    private EditBox frequency;
    private final EditBox[] mapping = new EditBox[TileEntityRadioTorchBase.MAPPING_SIZE];
    private boolean customMap;
    private boolean polling;
    private boolean sender;

    public RadioTorchScreen(RadioTorchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 204);
    }

    @Override
    protected void init() {
        super.init();
        TileEntityRadioTorchBase radio = menu.radio();
        sender = radio instanceof TileEntityRadioTorchSender;
        customMap = radio != null && radio.customMap;
        polling = radio != null && radio.polling;
        frequency = field(29, 22, 82, radio == null ? "" : radio.channel, 10);
        int senderOffset = sender ? 18 : 0;
        for (int i = 0; i < mapping.length; i++) {
            mapping[i] = field(11 + 130 * (i / 8) + senderOffset, 57 + 18 * (i % 8), 82,
                    radio == null || radio.mapping[i] == null ? "" : radio.mapping[i], 32);
            mapping[i].visible = customMap;
        }
    }

    private EditBox field(int x, int y, int width, String value, int maxLength) {
        EditBox field = new EditBox(font, leftPos + x, topPos + y, width, 14, Component.empty());
        field.setBordered(false);
        field.setTextColor(0xFF00FF00);
        field.setTextColorUneditable(0xFF00FF00);
        field.setMaxLength(maxLength);
        field.setValue(value == null ? "" : value);
        addRenderableWidget(field);
        return field;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Identifier texture = sender ? SENDER : RECEIVER;
        if (customMap) {
            GuiDraw.blit(graphics, texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
            GuiDraw.blit(graphics, texture, leftPos + 137, topPos + 17, 0, 204, 18, 18, 256, 256);
        } else {
            GuiDraw.blit(graphics, texture, leftPos, topPos, 0, 0, imageWidth, 35, 256, 256);
            GuiDraw.blit(graphics, texture, leftPos, topPos + 35, 0, 197, imageWidth, 7, 256, 256);
        }
        if (polling) GuiDraw.blit(graphics, texture, leftPos + 173, topPos + 17, 0, 222, 18, 18, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 6, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hover(137, 17, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, customMap ? "Custom Mapping" : "Redstone Passthrough", mouseX, mouseY);
        } else if (hover(173, 17, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, polling ? "Polling" : "State Change", mouseX, mouseY);
        } else if (hover(209, 17, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, "Save Settings", mouseX, mouseY);
        }
    }

    private void tooltip(GuiGraphicsExtractor graphics, String value, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, List.of(Component.literal(value)), Optional.empty(), mouseX, mouseY);
    }

    private boolean hover(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        if (hover(137, 17, 18, 18, event.x(), event.y())) {
            Gauges.click();
            customMap = !customMap;
            for (EditBox field : mapping) field.visible = customMap;
            if (!customMap) {
                for (EditBox field : mapping) {
                    if (!field.isFocused()) continue;
                    field.setFocused(false);
                    if (getFocused() == field) setFocused(null);
                }
            }
            sendToggle("hasMapping", customMap);
            return true;
        }
        if (hover(173, 17, 18, 18, event.x(), event.y())) {
            Gauges.click();
            polling = !polling;
            sendToggle("isPolling", polling);
            return true;
        }
        if (hover(209, 17, 18, 18, event.x(), event.y())) {
            Gauges.click();
            CompoundTag data = new CompoundTag();
            data.putString("channel", frequency.getValue());
            for (int i = 0; i < mapping.length; i++) data.putString("mapping" + i, mapping[i].getValue());
            send(data);
            return true;
        }
        return false;
    }

    private void sendToggle(String key, boolean value) {
        CompoundTag data = new CompoundTag();
        data.putBoolean(key, value);
        send(data);
    }

    private void send(CompoundTag data) {
        TileEntityRadioTorchBase radio = menu.radio();
        if (radio == null) return;
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(radio.getBlockPos(), data));
    }
}
