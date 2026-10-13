package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCoreEmitter;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.dfc.TileEntityCoreEmitter;
import com.hbm.util.BobMathUtil;
import dev.golem.core.Ids;
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
 * The emitter's panel, ported from 1.7 {@code GUICoreEmitter}: coolant tank at (8,17), power column at (26,17),
 * the watts field at (57,57) with its set button at (97,52) and the on/off button at (133,52).
 */
public class GUICoreEmitter extends GUIDFCBase<ContainerCoreEmitter> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/dfc/gui_emitter.png");

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
        // 1.7 GuiTextField(guiLeft + 57, guiTop + 57, 29, 12), no background, white text: an unbordered
        // EditBox draws its text at its own x/y exactly like the background-less 1.7 field.
        this.field = new EditBox(this.font, leftPos + 57, topPos + 57, 29, 12, Component.empty());
        this.field.setTextColor(0xFFFFFFFF);
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
            GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 53, 210F, 4F, 34, 16, 256, 256);
        }
        if (emitter.isOn) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 133, topPos + 52, 192F, 0F, 18, 18, 256, 256);
        }

        int w = emitter.watts * 34 / 100;
        if (w > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 45, 210F, 0F, w, 4, 256, 256);

        int i = (int) emitter.getPowerScaled(52);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 26, topPos + 69 - i, 176F, 52 - i, 16, i, 256, 256);

        drawTank(graphics, emitter.tank, 8, 17, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        TileEntityCoreEmitter emitter = menu.getEmitter();
        if (emitter != null) {
            GuiDraw.text(graphics, font, Component.literal("Output: " + BobMathUtil.getShortNumber(emitter.prev) + "Spk"),
                    50, 30, 0xFF7F7F, false);
        }
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCoreEmitter emitter = menu.getEmitter();
        if (emitter == null) return;

        if (isHovering(8, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(emitter.tank), mouseX, mouseY);
        } else if (isHovering(26, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(emitter.power, TileEntityCoreEmitter.maxPower), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityCoreEmitter emitter = menu.getEmitter();
        if (emitter == null) return false;

        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;

        // 1.7 hit boxes: x in [97, 115), y in (52, 70] (the y test is exclusive at the top, inclusive below).
        if (x >= 97 && x < 115 && y > 52 && y <= 70) {
            int watts;
            try {
                watts = Mth.clamp((int) Double.parseDouble(field.getValue().trim()), 1, 100);
            } catch (NumberFormatException e) {
                return false;
            }
            field.setValue(String.valueOf(watts));
            CompoundTag data = new CompoundTag();
            data.putInt("watts", watts);
            send(emitter, data);
            return true;
        }

        if (x >= 133 && x < 151 && y > 52 && y <= 70) {
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
        GuiUtil.playClickSound();
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        if (this.field != null && this.field.isFocused() && event.key() != 256 && event.key() != 258) {
            event.keyPressed(this.field);
            return true;
        }
        return false;
    }
}
