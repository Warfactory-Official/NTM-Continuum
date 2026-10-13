package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeBoy;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Little Boy schematic screen, ported from 1.7 {@code GUINukeBoy}: the five installed-part overlays and the info panel. */
public class GUINukeBoy extends CompatContainerScreen<ContainerNukeBoy> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/weapon/lilboyschematic.png");

    public GUINukeBoy(ContainerNukeBoy menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (menu.isReady()) GuiDraw.blit(graphics, TEXTURE, leftPos + 142, topPos + 90, 176F, 0F, 16, 16, 256, 256);
        if (menu.hasPart(0)) GuiDraw.blit(graphics, TEXTURE, leftPos + 27, topPos + 87, 176F, 16F, 21, 22, 256, 256);
        if (menu.hasPart(1)) GuiDraw.blit(graphics, TEXTURE, leftPos + 27, topPos + 89, 176F, 38F, 21, 18, 256, 256);
        if (menu.hasPart(2)) GuiDraw.blit(graphics, TEXTURE, leftPos + 74, topPos + 94, 176F, 57F, 19, 8, 256, 256);
        if (menu.hasPart(3)) GuiDraw.blit(graphics, TEXTURE, leftPos + 92, topPos + 95, 176F, 66F, 12, 6, 256, 256);
        if (menu.hasPart(4)) GuiDraw.blit(graphics, TEXTURE, leftPos + 107, topPos + 91, 176F, 75F, 16, 14, 256, 256);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 16, 2);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 drawCustomInfoStat: the text is anchored at (guiLeft - 8, guiTop + 32), not at the mouse.
        if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, lines("desc.gui.nukeBoy.desc"), Optional.empty(), leftPos - 8, topPos + 16 + 16);
        }
    }

    /** 1.7 {@code I18nUtil.resolveKeyArray}; the port's lang stores the {@code $} parts as {@code <key>.0}, {@code .1}, ... */
    static List<Component> lines(String key) {
        List<Component> lines = new ArrayList<>();
        Language language = Language.getInstance();
        for (int i = 0; language.has(key + "." + i); i++) lines.add(Component.translatable(key + "." + i));
        return lines;
    }
}
