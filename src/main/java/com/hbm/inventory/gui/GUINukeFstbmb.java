package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeFstbmb;
import com.hbm.tileentity.bomb.TileEntityNukeBalefire;
import com.hbm.packet.ControlEventPayload;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.7 {@code GUINukeFstbmb}: egg/battery overlays, the start button at (142,35) and a three-digit timer field at
 * (94,40) whose value (seconds, clamped 1..999) reaches the tile as it is typed. The running countdown is
 * printed in red at three-quarter scale under the battery.
 */
public class GUINukeFstbmb extends CompatContainerScreen<ContainerNukeFstbmb> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/weapon/fstbmbschematic.png");

    private EditBox timer;

    public GUINukeFstbmb(ContainerNukeFstbmb menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        String previous = timer == null ? null : timer.getValue();
        TileEntityNukeBalefire bomb = menu.getBalefire();
        timer = new EditBox(this.font, leftPos + 94, topPos + 40, 29, 12, Component.empty());
        timer.setTextColor(0xFFFF0000);
        timer.setBordered(false);
        timer.setMaxLength(3);
        timer.setValue(previous != null ? previous : String.valueOf(bomb == null ? 900 : bomb.timer / 20));
        timer.setResponder(this::sendTimer);
        addRenderableWidget(timer);
    }

    private void sendTimer(String text) {
        TileEntityNukeBalefire bomb = menu.getBalefire();
        if (bomb == null) return;
        double parsed;
        try {
            parsed = Double.parseDouble(text);
        } catch (NumberFormatException ignored) {
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putInt("timer", Mth.clamp((int) parsed, 1, 999));
        GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(bomb.getBlockPos(), data));
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityNukeBalefire bomb = menu.getBalefire();
        if (bomb == null || bomb.started) return false;
        double x = event.x(), y = event.y();
        if (leftPos + 142 <= x && leftPos + 142 + 18 > x && topPos + 35 < y && topPos + 35 + 18 >= y) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("start", true);
            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(bomb.getBlockPos(), data));
            return true;
        }
        return false;
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        // A focused field must consume the inventory key too, just as the old keyTyped path did.
        if (event.key() != 256 && event.key() != 258 && timer != null && timer.isFocused()) {
            event.keyPressed(timer);
            return true;
        }
        return false;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);

        TileEntityNukeBalefire bomb = menu.getBalefire();
        if (bomb != null && bomb.hasBattery()) {
            String text = bomb.getMinutes() + ":" + bomb.getSeconds();
            double scale = 0.75;
            GuiDraw.pushMatrix(graphics);
            GuiDraw.scale(graphics, (float) scale, (float) scale);
            // 1.7 halved the UNscaled width, so the readout sits a little right of centre - kept.
            GuiDraw.text(graphics, font, text, (int) ((69 - font.width(text) / 2) * (1 / scale)),
                    (int) (95.5 * (1 / scale)), 0xFFFF0000, false);
            GuiDraw.popMatrix(graphics);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityNukeBalefire bomb = menu.getBalefire();
        if (bomb == null) return;

        if (bomb.hasEgg()) GuiDraw.blit(graphics, TEXTURE, leftPos + 19, topPos + 90, 176F, 0F, 30, 16, 256, 256);

        int battery = bomb.getBattery();
        if (battery == 1) GuiDraw.blit(graphics, TEXTURE, leftPos + 88, topPos + 93, 176F, 16F, 18, 10, 256, 256);
        else if (battery == 2) GuiDraw.blit(graphics, TEXTURE, leftPos + 88, topPos + 93, 194F, 16F, 18, 10, 256, 256);

        if (bomb.started) GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 35, 176F, 26F, 18, 18, 256, 256);
    }
}
