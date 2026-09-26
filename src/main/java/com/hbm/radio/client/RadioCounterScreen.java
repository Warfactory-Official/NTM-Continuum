package com.hbm.radio.client;

import dev.golem.core.gui.Gauges;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.packet.ControlEventPayload;
import com.hbm.radio.RadioCounterMenu;
import com.hbm.tileentity.network.TileEntityRadioTorchCounter;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Faithful 218x238 ROR counter GUI. */
public final class RadioCounterScreen extends CompatContainerScreen<RadioCounterMenu> {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_rtty_counter.png");
    private final EditBox[] frequency = new EditBox[TileEntityRadioTorchCounter.MAPPING_SIZE];
    private boolean polling;

    public RadioCounterScreen(RadioCounterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 218, 238);
    }

    @Override
    protected void init() {
        super.init();
        TileEntityRadioTorchCounter counter = menu.counter();
        polling = counter != null && counter.polling;
        for (int i = 0; i < frequency.length; i++) {
            frequency[i] = field(29, 21 + 44 * i, 86,
                    counter == null ? "" : counter.channel[i], 10);
        }
    }

    private EditBox field(int x, int y, int width, String value, int maxLength) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, width, 14, Component.empty());
        box.setBordered(false);
        box.setTextColor(0xFF00FF00);
        box.setTextColorUneditable(0xFF00FF00);
        box.setMaxLength(maxLength);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        if (polling) GuiDraw.blit(graphics, TEXTURE, leftPos + 193, topPos + 8,
                218, 0, 18, 18, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (184 - font.width(title)) / 2, 6,
                0xFF404040, false);
        GuiDraw.text(graphics, font, Component.translatable("container.inventory"),
                16, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (hover(193, 8, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, polling ? "Polling" : "State Change", mouseX, mouseY);
            return;
        }
        if (hover(193, 30, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, "Save Settings", mouseX, mouseY);
            return;
        }

        TileEntityRadioTorchCounter counter = menu.counter();
        if (counter == null || !menu.getCarried().isEmpty()) return;
        for (int i = 0; i < TileEntityRadioTorchCounter.MAPPING_SIZE; i++) {
            if (!hover(138, 18 + 44 * i, 18, 18, mouseX, mouseY)) continue;
            String mode = counter.matcher.modes[i];
            if (mode != null) {
                GuiDraw.tooltip(graphics, font,
                        List.of(Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                                Component.literal(ModulePatternMatcher.getLabel(mode))),
                        Optional.empty(), mouseX, mouseY - 30);
            }
            return;
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
        if (hover(193, 8, 18, 18, event.x(), event.y())) {
            Gauges.click();
            polling = !polling;
            CompoundTag data = new CompoundTag();
            data.putBoolean("polling", true);
            send(data);
            return true;
        }
        if (hover(193, 30, 18, 18, event.x(), event.y())) {
            Gauges.click();
            CompoundTag data = new CompoundTag();
            for (int i = 0; i < frequency.length; i++) data.putString("channel" + i, frequency[i].getValue());
            send(data);
            return true;
        }
        return false;
    }

    private void send(CompoundTag data) {
        TileEntityRadioTorchCounter counter = menu.counter();
        if (counter != null) {
            GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                    new ControlEventPayload(counter.getBlockPos(), data));
        }
    }
}
