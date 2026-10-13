package com.hbm.inventory.gui;

import com.hbm.packet.ControlEventPayload;
import com.hbm.inventory.container.RadioLogicMenu;
import com.hbm.tileentity.network.TileEntityRadioTorchLogic;
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

public final class RadioLogicScreen extends CompatContainerScreen<RadioLogicMenu> {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_rtty_logic_receiver.png");
    private final EditBox[] mapping = new EditBox[TileEntityRadioTorchLogic.MAPPING_SIZE];
    private final int[] conditions = new int[TileEntityRadioTorchLogic.MAPPING_SIZE];
    private EditBox frequency;
    private boolean polling;
    private boolean descending;

    public RadioLogicScreen(RadioLogicMenu menu, Inventory inventory, Component title) { super(menu, inventory, title, 256, 204); }
    @Override protected void init() {
        super.init(); TileEntityRadioTorchLogic logic = menu.logic();
        polling = logic != null && logic.polling; descending = logic != null && logic.descending;
        frequency = field(29, 21, 82, logic == null ? "" : logic.channel, 10);
        for (int i = 0; i < mapping.length; i++) {
            mapping[i] = field(29 + 130 * (i / 8), 57 + 18 * (i % 8), 46,
                    logic == null ? "" : logic.mapping[i], 15);
            conditions[i] = logic == null ? 0 : logic.conditions[i];
        }
    }
    private EditBox field(int x, int y, int width, String value, int max) {
        EditBox box = new EditBox(font, leftPos + x, topPos + y, width, 14, Component.empty());
        box.setBordered(false); box.setTextColor(0xFF00FF00); box.setTextColorUneditable(0xFF00FF00);
        box.setMaxLength(max); box.setValue(value == null ? "" : value); addRenderableWidget(box); return box;
    }
    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float tick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, 256, 204, 256, 256);
        if (descending) GuiDraw.blit(graphics, TEXTURE, leftPos + 137, topPos + 17, 0, 204, 18, 18, 256, 256);
        if (polling) GuiDraw.blit(graphics, TEXTURE, leftPos + 173, topPos + 17, 0, 222, 18, 18, 256, 256);
        for (int i = 0; i < mapping.length; i++) {
            int u = 18 + conditions[i] * 18;
            int v = mapping[i].getValue().isEmpty() ? 222 : 204;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 7 + 130 * (i / 8), topPos + 53 + 18 * (i % 8), u, v, 18, 18, 256, 256);
        }
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int x, int y) {
        GuiDraw.text(graphics, font, title, (256 - font.width(title)) / 2, 6, 0xFF404040, false);
    }
    @Override protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (over(mouseX, mouseY, 137, 17, 18, 18)) tooltip(graphics, descending ? "Descending Order" : "Ascending Order", mouseX, mouseY);
        else if (over(mouseX, mouseY, 173, 17, 18, 18)) tooltip(graphics, polling ? "Polling" : "State Change", mouseX, mouseY);
        else if (over(mouseX, mouseY, 209, 17, 18, 18)) tooltip(graphics, "Save Settings", mouseX, mouseY);
        else for (int i = 0; i < conditions.length; i++) if (conditionOver(mouseX, mouseY, i)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.translatable("desc.gui.rttyLogic.cond" + conditions[i])), Optional.empty(), mouseX, mouseY); break;
        }
    }
    private void tooltip(GuiGraphicsExtractor graphics, String text, int x, int y) {
        GuiDraw.tooltip(graphics, font, List.of(Component.literal(text)), Optional.empty(), x, y);
    }
    @Override protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        if (over(event, 137, 17, 18, 18)) { RadioScreenUtil.click(); descending = !descending; toggle("descending", descending); return true; }
        if (over(event, 173, 17, 18, 18)) { RadioScreenUtil.click(); polling = !polling; toggle("polling", polling); return true; }
        if (over(event, 209, 17, 18, 18)) { RadioScreenUtil.click(); save(); return true; }
        for (int i = 0; i < conditions.length; i++) if (conditionOver(event.x(), event.y(), i)) { RadioScreenUtil.click(); conditions[i] = (conditions[i] + 1) % 10; return true; }
        return false;
    }
    @Override protected boolean mouseScrolledCompat(double mouseX, double mouseY, double scrollX, double scrollY) {
        for (int i = 0; i < conditions.length; i++) if (conditionOver(mouseX, mouseY, i)) {
            if (scrollY > 0) conditions[i] = (conditions[i] + 1) % 10;
            if (scrollY < 0) conditions[i] = (conditions[i] + 9) % 10;
            return scrollY != 0;
        }
        return false;
    }
    private boolean conditionOver(double x, double y, int index) { return over(x, y, 7 + 130 * (index / 8), 53 + 18 * (index % 8), 18, 18); }
    private boolean over(MouseInput event, int x, int y, int w, int h) { return over(event.x(), event.y(), x, y, w, h); }
    private boolean over(double mx, double my, int x, int y, int w, int h) { return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h; }
    private void toggle(String key, boolean value) { CompoundTag tag = new CompoundTag(); tag.putBoolean(key, value); send(tag); }
    private void save() { CompoundTag tag = new CompoundTag(); tag.putString("channel", frequency.getValue()); for (int i = 0; i < mapping.length; i++) { tag.putString("mapping" + i, mapping[i].getValue()); tag.putInt("conditions" + i, conditions[i]); } send(tag); }
    private void send(CompoundTag tag) { if (menu.logic() != null) GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(menu.logic().getBlockPos(), tag)); }
}
