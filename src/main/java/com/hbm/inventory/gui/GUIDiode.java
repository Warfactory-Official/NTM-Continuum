package com.hbm.inventory.gui;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.tileentity.network.TileEntityNetworkSeparator;
import com.hbm.packet.ControlEventPayload;
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
import net.minecraft.sounds.SoundEvents;

/**
 * 1.7's {@code GUIDiode}: a throughput field and a priority button over the dimmed world; closing it
 * sends both to the separator as one control event. Not a container screen.
 */
public final class GUIDiode extends CompatScreen {

    private final TileEntityNetworkSeparator diode;
    private EditBox textThroughput;
    private int priority;
    private int buttonX, buttonY;

    public GUIDiode(TileEntityNetworkSeparator diode) {
        super(Component.translatable("block.hbm.network_separator"));
        this.diode = diode;
        this.priority = diode.priority.ordinal();
    }

    @Override
    protected void init() {
        textThroughput = new EditBox(font, this.width / 2 - 150, 100, 90, 20, Component.empty());
        textThroughput.setValue("" + diode.configuredLimit);
        textThroughput.setMaxLength(11);
        addRenderableWidget(textThroughput);
        focusedEditBoxForInput = textThroughput;
        buttonX = this.width / 2 + 20;
        buttonY = 100;
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.text(g, font, "Throughput:", this.width / 2 - 150, 80, 0xFFA0A0A0, false);
        GuiDraw.text(g, font, "(max. 10,000,000,000 HE)", this.width / 2 - 150, 90, 0xFFA0A0A0, false);
        GuiDraw.text(g, font, "Priority:", this.width / 2 + 20, 80, 0xFFA0A0A0, false);

        // The priority button, drawn as a plain vanilla-style box.
        boolean hover = mouseX >= buttonX && mouseX < buttonX + 90 && mouseY >= buttonY && mouseY < buttonY + 20;
        GuiDraw.fill(g, buttonX, buttonY, buttonX + 90, buttonY + 20, 0xFF000000);
        GuiDraw.fill(g, buttonX + 1, buttonY + 1, buttonX + 89, buttonY + 19, hover ? 0xFF7A7AA0 : 0xFF6F6F6F);
        String label = ConnectionPriority.VALUES[priority].name();
        GuiDraw.text(g, font, label, buttonX + 45 - font.width(label) / 2, buttonY + 6, 0xFFFFFFFF, true);
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput event, boolean doubleClick) {
        if (event.x() >= buttonX && event.x() < buttonX + 90 && event.y() >= buttonY && event.y() < buttonY + 20) {
            priority++;
            if (priority >= ConnectionPriority.VALUES.length) priority = 0;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressedCompat(KeyInput event) {
        if (textThroughput != null && textThroughput.isFocused() && event.key() != 256) {
            event.keyPressed(textThroughput);
            return true;
        }
        if (event.key() != 256 && this.minecraft != null && event.matches(this.minecraft.options.keyInventory)) {
            onClose();
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        CompoundTag data = new CompoundTag();
        data.putByte("priority", (byte) priority);
        try {
            data.putLong("limit", Long.parseLong(textThroughput.getValue()));
        } catch (Exception ignored) {}
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(diode.getBlockPos(), data));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
