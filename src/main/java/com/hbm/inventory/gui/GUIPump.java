package com.hbm.inventory.gui;

import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.tileentity.network.TileEntityFlowControlPump;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.gui.CompatScreen;
import dev.golem.core.gui.GuiDraw;
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
 * The 1.7 {@code GUIPump}: a plain screen (not a container) with a throughput text box and two cycling
 * buttons, pressure (0–5 PU) and priority, at y=100, in three 90×20 columns from x = width/2 − 150.
 * Everything is sent to the pump as one control packet when the screen closes, exactly as 1.7 did in
 * {@code onGuiClosed}. The buttons are drawn here rather than as vanilla widgets.
 */
public final class GUIPump extends CompatScreen {

    private final TileEntityFlowControlPump pump;
    private EditBox throughput;
    private int pressure;
    private int priority;

    public GUIPump(TileEntityFlowControlPump pump) {
        super(Component.translatable("block.hbm.flow_control_pump"));
        this.pump = pump;
        this.pressure = pump.tank[0].getPressure();
        this.priority = pump.priority.ordinal();
    }

    @Override
    protected void init() {
        throughput = new EditBox(font, this.width / 2 - 150, 100, 90, 20, Component.empty());
        throughput.setValue("" + pump.bufferSize);
        throughput.setMaxLength(5);
        addRenderableWidget(throughput);
    }

    @Override
    protected void extractBackgroundCompat(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        GuiDraw.text(g, font, "Throughput:", this.width / 2 - 150, 80, 0xFFA0A0A0, false);
        GuiDraw.text(g, font, "(max. 10,000mB)", this.width / 2 - 150, 90, 0xFFA0A0A0, false);
        GuiDraw.text(g, font, "Pressure:", this.width / 2 - 50, 80, 0xFFA0A0A0, false);
        button(g, this.width / 2 - 50, 100, pressure + " PU", mouseX, mouseY);
        GuiDraw.text(g, font, "Priority:", this.width / 2 + 50, 80, 0xFFA0A0A0, false);
        button(g, this.width / 2 + 50, 100, priorityName(), mouseX, mouseY);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, String label, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + 90 && mouseY >= y && mouseY < y + 20;
        GuiDraw.fill(g, x, y, x + 90, y + 20, 0xFF000000);
        GuiDraw.fill(g, x + 1, y + 1, x + 89, y + 19, hover ? 0xFF8A8AC0 : 0xFF6F6F6F);
        int w = font.width(label);
        GuiDraw.text(g, font, label, x + 45 - w / 2, y + 6, hover ? 0xFFFFFFA0 : 0xFFE0E0E0, true);
    }

    private String priorityName() {
        ConnectionPriority[] values = ConnectionPriority.values();
        return values[Math.floorMod(priority, values.length)].name();
    }

    @Override
    protected boolean mouseClickedCompat(MouseInput e, boolean doubleClick) {
        int x = (int) e.x(), y = (int) e.y();
        if (x >= this.width / 2 - 50 && x < this.width / 2 + 40 && y >= 100 && y < 120) {
            pressure++;
            if (pressure > 5) pressure = 0;
            click();
            return true;
        }
        if (x >= this.width / 2 + 50 && x < this.width / 2 + 140 && y >= 100 && y < 120) {
            priority++;
            if (priority >= ConnectionPriority.values().length) priority = 0;
            click();
            return true;
        }
        return false;
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void onClose() {
        CompoundTag data = new CompoundTag();
        data.putByte("pressure", (byte) pressure);
        data.putByte("priority", (byte) priority);
        try {
            data.putInt("capacity", Integer.parseInt(throughput.getValue()));
        } catch (Exception ignored) {
        }
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(pump.getBlockPos(), data));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
