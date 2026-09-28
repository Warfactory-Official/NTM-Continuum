package com.hbm.inventory.gui;

import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityRadioRec;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code GUIRadioRec}: a 220x42 strip of {@code gui_radio.png} - a green ten-character frequency field at
 * (29, 21), a save button at (137, 17) and the on/off switch at (173, 17), lit from (0, 42) while on. Both
 * buttons send a control packet; neither closes the screen.
 */
public class GUIRadioRec extends CompatScreen {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_radio.png");
    private final TileEntityRadioRec radio;
    private final int xSize = 220;
    private final int ySize = 42;
    private int guiLeft;
    private int guiTop;
    private EditBox frequency;

    public GUIRadioRec(TileEntityRadioRec radio) {
        super(Component.translatable("container.radiorec"));
        this.radio = radio;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        int oX = 4;
        int oY = 4;
        frequency = new EditBox(font, guiLeft + 25 + oX, guiTop + 17 + oY, 90 - oX * 2, 14, Component.empty());
        frequency.setTextColor(0xFF00FF00);
        frequency.setTextColorUneditable(0xFF00FF00);
        frequency.setBordered(false);
        frequency.setMaxLength(10);
        frequency.setValue(radio.channel == null ? "" : radio.channel);
        addRenderableWidget(frequency);
        focusedEditBoxForInput = frequency;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(g, TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize, 256, 256);
        if (radio.isOn) GuiDraw.blit(g, TEXTURE, guiLeft + 173, guiTop + 17, 0, 42, 18, 18, 256, 256);
    }

    @Override
    protected void extractForegroundCompat(GuiGraphicsExtractor g, int x, int y, float partialTick) {
        GuiDraw.text(g, font, title, guiLeft + xSize / 2 - font.width(title) / 2, guiTop + 6, 0xFF404040, false);
        if (guiLeft + 137 <= x && guiLeft + 137 + 18 > x && guiTop + 17 < y && guiTop + 17 + 18 >= y)
            GuiDraw.tooltip(g, font, List.of(Component.literal("Save Settings")), Optional.empty(), x, y);
        if (guiLeft + 173 <= x && guiLeft + 173 + 18 > x && guiTop + 17 < y && guiTop + 17 + 18 >= y)
            GuiDraw.tooltip(g, font, List.of(Component.literal("Toggle")), Optional.empty(), x, y);
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput e, boolean doubleClick) {
        double x = e.x(), y = e.y();
        if (guiLeft + 137 <= x && guiLeft + 137 + 18 > x && guiTop + 17 < y && guiTop + 17 + 18 >= y) {
            click();
            CompoundTag data = new CompoundTag();
            data.putString("channel", frequency.getValue());
            send(data);
            return true;
        }
        if (guiLeft + 173 <= x && guiLeft + 173 + 18 > x && guiTop + 17 < y && guiTop + 17 + 18 >= y) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("isOn", !radio.isOn);
            send(data);
            return true;
        }
        return false;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private void send(CompoundTag data) {
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(radio.getBlockPos(), data));
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.key() == 256 || (!frequency.isFocused() && event.matches(mc.options.keyInventory))) {
            mc.setScreen(null);
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
