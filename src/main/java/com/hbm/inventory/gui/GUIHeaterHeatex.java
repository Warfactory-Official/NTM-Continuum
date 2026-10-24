package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerHeaterHeatex;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.ControlEventPayload;
import com.hbm.tileentity.machine.TileEntityHeaterHeatex;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.KeyInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** The legacy 176x204 Heatex screen, including live tank fills and the two control fields. */
public class GUIHeaterHeatex extends CompatContainerScreen<ContainerHeaterHeatex> {

    private static final Identifier TEXTURE =
            Ids.of("hbm", "textures/gui/machine/gui_heatex.png");

    private EditBox fieldCycles;
    private EditBox fieldDelay;

    public GUIHeaterHeatex(ContainerHeaterHeatex menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    protected void init() {
        super.init();

        String previousCycles = fieldCycles == null ? null : fieldCycles.getValue();
        TileEntityHeaterHeatex heater = menu.getHeater();
        fieldCycles = field(leftPos + 73, topPos + 31, 30, previousCycles,
                heater == null ? "" : String.valueOf(heater.amountToCool));
        fieldCycles.setResponder(text -> sendInt("toCool", text));

        String previousDelay = fieldDelay == null ? null : fieldDelay.getValue();
        fieldDelay = field(leftPos + 73, topPos + 49, 30, previousDelay,
                heater == null ? "" : String.valueOf(heater.tickDelay));
        fieldDelay.setResponder(text -> sendInt("delay", text));
    }

    private EditBox field(int x, int y, int width, String previous, String initial) {
        EditBox field = new EditBox(this.font, x, y, width, 10, Component.empty());
        field.setTextColor(0xFF00FF00);
        field.setBordered(false);
        field.setMaxLength(5);
        if (previous != null) {
            field.setValue(previous);
        } else {
            field.setValue(initial);
        }
        addRenderableWidget(field);
        return field;
    }

    private void sendInt(String key, String text) {
        TileEntityHeaterHeatex heater = menu.getHeater();
        if (heater == null) return;

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            value = 0;
        }

        CompoundTag data = new CompoundTag();
        data.putInt(key, value);
        GolemNetwork.sendToServer(ControlEventPayload.CODEC,
                new ControlEventPayload(heater.getBlockPos(), data));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityHeaterHeatex heater = menu.getHeater();
        if (heater == null) return;
        drawTank(graphics, heater.tanks[0], 44, 36, 16, 52);
        drawTank(graphics, heater.tanks[1], 116, 36, 16, 52);
    }

    /** Repeats the 16x16 fluid texture, matching the old GL_REPEAT tank quad. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int max = tank.getMaxFill();
        int filled = max <= 0 ? 0 : (int) ((long) tank.getFill() * height / max);
        if (filled <= 0) return;

        int x0 = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - filled;
        int tint = 0xFF000000 | (tank.getTankType().clientMetadata().getTint() & 0xFFFFFF);
        Identifier fluidTexture = tank.getTankType().clientMetadata().getTexture();

        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + width, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x0, tileY, 0F, 0F, width, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x0, top, x0 + width, bottom,
                    0xFF000000 | (tank.getTankType().clientMetadata().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityHeaterHeatex heater = menu.getHeater();
        if (heater == null) return;

        if (legacyHover(70, 26, 36, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.translatable("gui.heatex.amount")), mouseX, mouseY);
        } else if (legacyHover(70, 44, 36, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.translatable("gui.heatex.cycle")), mouseX, mouseY);
        } else if (legacyHover(44, 36, 16, 52, mouseX, mouseY)) {
            fluidTooltip(graphics, heater.tanks[0], 44, 36, 16, 52, mouseX, mouseY);
        } else if (legacyHover(116, 36, 16, 52, mouseX, mouseY)) {
            fluidTooltip(graphics, heater.tanks[1], 116, 36, 16, 52, mouseX, mouseY);
        }
    }

    /** The old GUI used an open lower bound and a closed upper bound for painted hover regions. */
    private boolean legacyHover(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY > topPos + y && mouseY <= topPos + y + height;
    }

    private void fluidTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height,
                               int mouseX, int mouseY) {
        if (legacyHover(x, y, width, height, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(tank), mouseX, mouseY);
        }
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> lines, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
    }

    @Override
    protected boolean keyPressed(KeyInput event) {
        // A focused field must consume the inventory key too, just as the old keyTyped path did.
        if (event.key() != 256 && event.key() != 258) {
            if (fieldCycles != null && fieldCycles.isFocused()) {
                event.keyPressed(fieldCycles);
                return true;
            }
            if (fieldDelay != null && fieldDelay.isFocused()) {
                event.keyPressed(fieldDelay);
                return true;
            }
        }
        return false;
    }
}
