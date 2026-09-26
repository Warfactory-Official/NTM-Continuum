package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.inventory.container.ContainerCoreEmitter;
import com.hbm.lib.Library;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * The emitter's panel. The field sets how hard it fires, from one to a hundred watts; the button beside it
 * commits that and the one after it switches the beam on. The green stripe across the top lights while the
 * emitter has the power to sustain what it has been asked for.
 */
public class GUICoreEmitter extends GUIDFCBase<ContainerCoreEmitter> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/dfc/gui_emitter.png");

    private EditBox field;

    public GUICoreEmitter(ContainerCoreEmitter menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        String previous = this.field == null ? null : this.field.getValue();
        this.field = new EditBox(this.font, leftPos + 88, topPos + 62, 24, 10, Component.empty());
        this.field.setTextColor(0xFF5BBC00);
        this.field.setBordered(false);
        this.field.setMaxLength(3);
        TileEntityCoreEmitter emitter = menu.getEmitter();
        this.field.setValue(previous != null ? previous : String.valueOf(emitter == null ? 1 : emitter.watts));
        addRenderableWidget(this.field);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityCoreEmitter emitter = menu.getEmitter();
        if (emitter == null) return;

        if (field != null && field.isFocused()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 81, topPos + 58, 210F, 0F, 32, 14, 256, 256);
        }
        if (emitter.isOn) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 151, topPos + 56, 192F, 0F, 18, 18, 256, 256);
        }

        drawWattsBar(graphics, 81, 17, emitter.getWattsScaled(35));
        drawPowerBar(graphics, 49, 21, emitter.power, TileEntityCoreEmitter.maxPower);

        if (emitter.isOn && emitter.power > 500_000) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 149, topPos + 33, 176F, 87F, 80, 3, 256, 256);
        }

        drawTank(graphics, emitter.tank, 13, 21, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCoreEmitter emitter = menu.getEmitter();
        if (emitter == null) return;

        if (isHovering(13, 21, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(emitter.tank), mouseX, mouseY);
        } else if (isHovering(49, 21, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(emitter.power, TileEntityCoreEmitter.maxPower), mouseX, mouseY);
        } else if (isHovering(79, 14, 8, 39, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Output: " + Library.getShortNumber(emitter.prev) + "SPK")),
                    mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityCoreEmitter emitter = menu.getEmitter();
        if (emitter == null) return false;

        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;

        if (x >= 124 && x < 142 && y >= 56 && y < 74) {
            int watts;
            try {
                watts = Mth.clamp(Integer.parseInt(field.getValue().trim()), 1, 100);
            } catch (NumberFormatException e) {
                return false;
            }
            field.setValue(String.valueOf(watts));
            CompoundTag data = new CompoundTag();
            data.putInt("watts", watts);
            send(emitter, data);
            return true;
        }

        if (x >= 151 && x < 169 && y >= 56 && y < 74) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            send(emitter, data);
            return true;
        }

        return false;
    }

    private void send(TileEntityCoreEmitter emitter, CompoundTag data) {
        GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                new ControlEventPayload(emitter.getBlockPos(), data));
        Gauges.click();
    }

    @Override
    protected boolean keyPressed(dev.golem.core.gui.KeyInput event) {
        if (this.field != null && this.field.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(this.field);
            return true;
        }
        return false;
    }
}
