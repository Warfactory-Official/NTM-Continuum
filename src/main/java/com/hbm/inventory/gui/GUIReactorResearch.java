package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerReactorResearch;
import com.hbm.tileentity.machine.TileEntityReactorResearch;
import com.hbm.packet.ControlEventPayload;
import com.hbm.sound.ModSounds;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import dev.golem.core.net.GolemNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;
import com.hbm.module.NumberDisplay;

/** The 176x222 research reactor screen: three seven-segment readouts, the level field and its confirm button. */
public final class GUIReactorResearch extends dev.golem.core.gui.CompatContainerScreen<ContainerReactorResearch> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_research_reactor.png");

    private final NumberDisplay[] displays = new NumberDisplay[3];
    private byte timer;
    private EditBox field;

    public GUIReactorResearch(ContainerReactorResearch menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
        displays[0] = new NumberDisplay(14, 25, 0x08FF00).setDigitLength(4);
        displays[1] = new NumberDisplay(12, 63, 0x08FF00).setDigitLength(3);
        displays[2] = new NumberDisplay(5, 101, 0x08FF00).setDigitLength(3);
    }

    @Override
    protected void init() {
        super.init();
        String previous = field == null ? null : field.getValue();
        TileEntityReactorResearch reactor = menu.getReactor();
        field = new EditBox(this.font, leftPos + 8, topPos + 99, 33, 16, Component.empty());
        field.setBordered(false);
        field.setMaxLength(3);
        field.setValue(previous != null ? previous : String.valueOf(reactor == null ? 0 : (int) (reactor.rodLevel * 100)));
        addRenderableWidget(field);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityReactorResearch reactor = menu.getReactor();

        if (reactor != null && reactor.rodLevel <= 0.5D) {
            for (int x = 0; x < 3; x++)
                for (int y = 0; y < 3; y++)
                    GuiDraw.blit(graphics, TEXTURE, leftPos + 81 + 36 * x, topPos + 26 + 36 * y, 176F, 0F, 8, 8, 256, 256);
            for (int x = 0; x < 2; x++)
                for (int y = 0; y < 2; y++)
                    GuiDraw.blit(graphics, TEXTURE, leftPos + 99 + 36 * x, topPos + 44 + 36 * y, 176F, 0F, 8, 8, 256, 256);
        }

        if (timer > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 44, topPos + 97, 176F, 8F, 11, 20, 256, 256);
            timer--;
        }

        int[] data = reactor == null ? new int[]{0, 0} : reactor.getDisplayData();
        for (int i = 0; i < 2; i++) displays[i].drawNumber(graphics, leftPos, topPos, data[i]);

        String text = field == null ? "0" : field.getValue();
        if (isDigits(text)) {
            int level = (int) Mth.clamp(Double.parseDouble(text), 0, 100);
            if (!text.equals(level + "")) field.setValue(level + "");
            displays[2].drawNumber(graphics, leftPos, topPos, level);
        } else {
            if (field != null) field.setValue("0");
            displays[2].drawNumber(graphics, leftPos, topPos, 0);
        }

        GuiUtil.drawInfoPanel(graphics, leftPos - 14, topPos + 23, 3);
        GuiUtil.drawInfoPanel(graphics, leftPos - 14, topPos + 61, 2);
    }

    private static boolean isDigits(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) if (!Character.isDigit(text.charAt(i))) return false;
        return true;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 121 - font.width(title) / 2, 6, 0xFFE5E5E5, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
        GuiDraw.text(graphics, font, Component.literal("Flux"), 6, 13, 0xFFE5E5E5, false);
        GuiDraw.text(graphics, font, Component.literal("Heat"), 6, 51, 0xFFE5E5E5, false);
        GuiDraw.text(graphics, font, Component.literal("Control"), 6, 89, 0xFFE5E5E5, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(-14, 23, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("The reactor has to be submerged"),
                    Component.literal("in water on its sides to cool."),
                    Component.literal("The neutron flux is provided to"),
                    Component.literal("adjacent breeding reactors.")), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(-14, 61, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("This reactor is fueled with plate fuel."),
                    Component.literal("The reaction needs a neutron source to start.")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        TileEntityReactorResearch reactor = menu.getReactor();

        displays[2].setBlinks(leftPos + 8 <= mouseX && leftPos + 8 + 33 > mouseX && topPos + 99 < mouseY && topPos + 99 + 16 >= mouseY);

        if (reactor != null && leftPos + 44 <= mouseX && leftPos + 44 + 11 > mouseX && topPos + 97 < mouseY && topPos + 97 + 20 >= mouseY) {
            double level;
            String text = field.getValue();
            if (isDigits(text)) {
                int j = (int) Mth.clamp(Double.parseDouble(text), 0, 100);
                field.setValue(j + "");
                level = j * 0.01D;
            } else {
                return false;
            }

            CompoundTag control = new CompoundTag();
            control.putDouble("level", level);
            timer = 15;

            GolemNetwork.sendToServer(ControlEventPayload.CODEC, new ControlEventPayload(reactor.getBlockPos(), control));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.RBMK_AZ5_COVER.get(), 0.5F));
            return true;
        }
        return false;
    }
}
