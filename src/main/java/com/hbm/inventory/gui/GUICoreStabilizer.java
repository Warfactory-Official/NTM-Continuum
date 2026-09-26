package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.inventory.container.ContainerCoreStabilizer;
import com.hbm.items.machine.ItemLens;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.dfc.TileEntityCoreStabilizer;
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
import net.minecraft.world.item.ItemStack;

/**
 * The stabiliser's panel. The field is the strength of field it holds, and the cost of that field goes up
 * with its fourth power, so the number is the whole difficulty of running a core. The lamp beside the lens
 * slot reports that a lens is in and not yet spent.
 */
public class GUICoreStabilizer extends GUIDFCBase<ContainerCoreStabilizer> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/dfc/gui_stabilizer.png");

    private EditBox field;

    public GUICoreStabilizer(ContainerCoreStabilizer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 170);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected void init() {
        super.init();
        String previous = this.field == null ? null : this.field.getValue();
        this.field = new EditBox(this.font, leftPos + 60, topPos + 62, 24, 10, Component.empty());
        this.field.setTextColor(0xFF5BBC00);
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
            GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 58, 192F, 0F, 32, 14, 256, 256);
        }

        drawWattsBar(graphics, 81, 17, stabilizer.getWattsScaled(35));
        drawPowerBar(graphics, 13, 21, stabilizer.power, TileEntityCoreStabilizer.maxPower);

        ItemStack lens = menu.slots.get(0).getItem();
        if (lens.getItem() instanceof ItemLens item && ItemLens.getLensDamage(lens) < item.maxDamage) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 43, topPos + 33, 0F, 170F, 25, 2, 256, 256);
        }
        if (stabilizer.beam > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 149, topPos + 33, 176F, 87F, 80, 3, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCoreStabilizer stabilizer = menu.getStabilizer();
        if (stabilizer == null) return;
        if (isHovering(13, 21, 16, 52, mouseX, mouseY)) {
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
        if (x < 115 || x >= 133 || y < 56 || y >= 74) return false;

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
        Gauges.click();
        return true;
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
