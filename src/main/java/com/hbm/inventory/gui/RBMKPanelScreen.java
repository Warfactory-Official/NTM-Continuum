package com.hbm.inventory.gui;

import com.hbm.packet.ControlEventPayload;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

/**
 * The shared frame of the 1.7 Redstone-over-Radio panel screens ({@code GUIScreenRBMKKeyPad} and its
 * siblings): a centred sheet, green borderless text fields ({@code setupTextFieldStandard}), the title in
 * dark grey at y 6, and a save button that sends one control packet. Not a container menu.
 */
abstract class RBMKPanelScreen extends CompatScreen {
    /** 1.7 {@code GUIScreenRadioTorch.MAX_CHAN_LENGTH}. */
    protected static final int MAX_CHAN_LENGTH = 15;
    protected final Identifier texture;
    protected final int xSize, ySize;
    protected int guiLeft, guiTop;

    protected RBMKPanelScreen(String titleKey, String texture, int xSize, int ySize) {
        super(Component.translatable(titleKey));
        this.texture = Ids.of("hbm", "textures/gui/machine/" + texture + ".png");
        this.xSize = xSize;
        this.ySize = ySize;
    }

    @Override protected void init() {
        guiLeft = (width - xSize) / 2;
        guiTop = (height - ySize) / 2;
        initFields();
    }

    protected abstract void initFields();

    /** {@code new GuiTextField(font, guiLeft + x + 4, guiTop + y + 4, w - 8, 14)} plus the standard setup. */
    protected final EditBox field(int x, int y, int w, int length, String value) {
        EditBox box = new EditBox(font, guiLeft + x + 4, guiTop + y + 4, w - 8, 14, Component.empty());
        box.setBordered(false);
        box.setTextColor(0xFF00FF00);
        box.setTextColorUneditable(0xFF00FF00);
        box.setMaxLength(length);
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    protected final void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        GuiDraw.blit(g, texture, guiLeft + x, guiTop + y, u, v, w, h, 256, 256);
    }

    @Override protected void extractForegroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        GuiDraw.text(g, font, title, guiLeft + xSize / 2 - font.width(title) / 2, guiTop + 6, 0xFF404040, false);
    }

    /** The 1.7 click test: inclusive of the bottom edge and exclusive of the top, exactly as written. */
    protected final boolean over(MouseInput e, int x, int y, int w, int h) {
        return guiLeft + x <= e.x() && guiLeft + x + w > e.x() && guiTop + y < e.y() && guiTop + y + h >= e.y();
    }

    protected static void press(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, pitch));
    }

    /** The toggle sound: 0.5 pitch when switching off, 0.75 when switching on. */
    protected static boolean toggle(boolean value) {
        press(0.5F + (!value ? 0.25F : 0F));
        return !value;
    }

    protected static void send(BlockPos pos, CompoundTag data) {
        press(1F);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(pos, data));
    }

    protected static String hex(int color) {
        String col = Integer.toHexString(color);
        while (col.length() < 6) col = "0" + col;
        return col;
    }

    @Override public boolean isPauseScreen() { return false; }
}
