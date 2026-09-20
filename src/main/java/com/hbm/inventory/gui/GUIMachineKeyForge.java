package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineKeyForge;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The locksmith table screen, port of 1.7.10's {@code GUIMachineKeyForge}: the 176×186
 * {@code gui_keyforge.png} background, the white centred title, and two info-panel glyphs at
 * (12,28) and (12,44) whose hover text is the {@code desc.gui.keyforge.key} / {@code .random} lines
 * (split on the old {@code $} line break).
 */
public class GUIMachineKeyForge extends CompatContainerScreen<ContainerMachineKeyForge> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_keyforge.png");

    public GUIMachineKeyForge(ContainerMachineKeyForge menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        GuiUtil.drawInfoPanel(graphics, this.leftPos + 12, this.topPos + 28, 2);
        GuiUtil.drawInfoPanel(graphics, this.leftPos + 12, this.topPos + 28 + 16, 3);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, this.imageWidth / 2 - font.width(title) / 2, 6, 0xffffff, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.isHovering(12, 28, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, lines("desc.gui.keyforge.key"), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(12, 28 + 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, lines("desc.gui.keyforge.random"), Optional.empty(), mouseX, mouseY);
        }
    }

    /** The old {@code I18nUtil.resolveKeyArray}: one translation, split on {@code $}. */
    private static List<Component> lines(String key) {
        List<Component> lines = new ArrayList<>();
        for (String line : I18n.get(key).split("\\$")) lines.add(Component.literal(line));
        return lines;
    }
}
