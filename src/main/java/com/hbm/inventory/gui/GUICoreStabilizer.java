package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCoreStabilizer;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
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
 * The stabiliser's panel. The field is the strength of field it holds, and the cost of that field goes up
 * with its fourth power, so the number is the whole difficulty of running a core. Layout ported from 1.7
 * {@code GUICoreStabilizer}: power column at (35,17), watts bar at (71,45), field at (75,57), set button at (124,52).
 */
public class GUICoreStabilizer extends GUIDFCBase<ContainerCoreStabilizer> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/dfc/gui_stabilizer.png");

    private EditBox field;

    public GUICoreStabilizer(ContainerCoreStabilizer menu, Inventory playerInventory, Component title) {
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
        // 1.7 GuiTextField(guiLeft + 75, guiTop + 57, 29, 12), no background, white text.
        this.field = new EditBox(this.font, leftPos + 75, topPos + 57, 29, 12, Component.empty());
        this.field.setTextColor(0xFFFFFFFF);
        this.field.setBordered(false);
        this.field.setMaxLength(3);
        TileEntityCoreStabilizer stabilizer = menu.getStabilizer();
        this.field.setValue(previous != null ? previous : String.valueOf(stabilizer == null ? 1 : stabilizer.watts));
        addRenderableWidget(this.field);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityCoreStabilizer stabilizer = menu.getStabilizer();
        if (stabilizer == null) return;

        if (field != null && field.isFocused()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 71, topPos + 53, 192F, 4F, 34, 16, 256, 256);
        }

        int w = stabilizer.watts * 34 / 100;
        if (w > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 71, topPos + 45, 192F, 0F, w, 4, 256, 256);

        int i = (int) stabilizer.getPowerScaled(52);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 35, topPos + 69 - i, 176F, 52 - i, 16, i, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCoreStabilizer stabilizer = menu.getStabilizer();
        if (stabilizer == null) return;
        if (isHovering(35, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(stabilizer.power, TileEntityCoreStabilizer.maxPower),
                    mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        TileEntityCoreStabilizer stabilizer = menu.getStabilizer();
        if (stabilizer == null) return false;

        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        // 1.7 hit box: x in [124, 142), y in (52, 70].
        if (x < 124 || x >= 142 || y <= 52 || y > 70) return false;

        int watts;
        try {
            watts = Mth.clamp(Integer.parseInt(field.getValue().trim()), 1, 100);
        } catch (NumberFormatException e) {
            return false;
        }
        field.setValue(String.valueOf(watts));

        CompoundTag data = new CompoundTag();
        data.putInt("watts", watts);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                new ControlEventPayload(stabilizer.getBlockPos(), data));
        GuiUtil.playClickSound();
        return true;
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
