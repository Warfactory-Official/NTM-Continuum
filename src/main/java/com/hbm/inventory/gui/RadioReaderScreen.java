package com.hbm.inventory.gui;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.packet.ControlEventPayload;
import com.hbm.inventory.container.RadioReaderMenu;
import com.hbm.tileentity.network.RadioTargets;
import com.hbm.tileentity.network.TileEntityRadioTorchReader;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RadioReaderScreen extends CompatContainerScreen<RadioReaderMenu> {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_rtty_reader.png");
    private final EditBox[] channels = new EditBox[TileEntityRadioTorchReader.MAPPING_SIZE];
    private final EditBox[] names = new EditBox[TileEntityRadioTorchReader.MAPPING_SIZE];
    private boolean polling;

    public RadioReaderScreen(RadioReaderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 204);
    }
    @Override protected void init() {
        super.init(); TileEntityRadioTorchReader reader = menu.reader(); polling = reader != null && reader.polling;
        for (int i = 0; i < channels.length; i++) {
            channels[i] = field(29, 57 + i * 18, 64, reader == null ? "" : reader.channels[i], 15);
            names[i] = field(123, 57 + i * 18, 118, reader == null ? "" : reader.names[i], 25);
        }
    }
    private EditBox field(int x, int y, int width, String value, int max) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, width, 14, Component.empty());
        box.setBordered(false); box.setTextColor(0xFF00FF00); box.setTextColorUneditable(0xFF00FF00);
        box.setMaxLength(max); box.setValue(value == null ? "" : value); addRenderableWidget(box); return box;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, 256, 204, 256, 256);
        if (polling) GuiDraw.blit(graphics, TEXTURE, leftPos + 173, topPos + 17, 0, 204, 18, 18, 256, 256);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int x, int y) {
        GuiDraw.text(graphics, font, title, (256 - font.width(title)) / 2, 6, 0xFF404040, false);
    }
    @Override protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (mouseX >= leftPos + 29 && mouseX < leftPos + 47 && mouseY >= topPos + 17 && mouseY < topPos + 35) {
            List<Component> lines = new ArrayList<>(); lines.add(Component.literal("Readable values:"));
            TileEntityRadioTorchReader reader = menu.reader();
            if (reader != null && reader.getLevel() != null) {
                var provider = RadioTargets.valueProvider(reader.getLevel(),
                        reader.getBlockPos().relative(reader.getTorchFacing().getOpposite()));
                if (provider != null) {
                    for (String info : provider.getFunctionInfo()) if (info.startsWith(IRORValueProvider.PREFIX_VALUE))
                        lines.add(Component.literal(info.substring(4)).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
        } else if (mouseX >= leftPos + 173 && mouseX < leftPos + 191 && mouseY >= topPos + 17 && mouseY < topPos + 35) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(polling ? "Polling" : "State Change")), Optional.empty(), mouseX, mouseY);
        } else if (mouseX >= leftPos + 209 && mouseX < leftPos + 227 && mouseY >= topPos + 17 && mouseY < topPos + 35) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("Save Settings")), Optional.empty(), mouseX, mouseY);
        }
    }
    @Override protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        if (over(event, 173, 17)) { RadioScreenUtil.click(); polling = !polling; CompoundTag tag = new CompoundTag(); tag.putBoolean("polling", polling); send(tag); return true; }
        if (over(event, 209, 17)) { RadioScreenUtil.click(); CompoundTag tag = new CompoundTag(); for (int i = 0; i < channels.length; i++) { tag.putString("channels" + i, channels[i].getValue()); tag.putString("names" + i, names[i].getValue()); } send(tag); return true; }
        return false;
    }
    private boolean over(MouseInput event, int x, int y) { return event.x() >= leftPos + x && event.x() < leftPos + x + 18 && event.y() >= topPos + y && event.y() < topPos + y + 18; }
    private void send(CompoundTag tag) { if (menu.reader() != null) GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(menu.reader().getBlockPos(), tag)); }
}
