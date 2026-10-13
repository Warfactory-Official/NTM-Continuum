package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeTsar;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Tsar Bomba schematic, including the optional sixth-stage core overlay. */
public class GUINukeTsar extends CompatContainerScreen<ContainerNukeTsar> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/weapon/tsarbombaschematic.png");
    private static final Identifier MIKE_TEXTURE = Ids.of("hbm", "textures/gui/weapon/ivymikeschematic.png");

    public GUINukeTsar(ContainerNukeTsar menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 256, 233);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 48, imageHeight - 94, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (menu.isFilled()) GuiDraw.blit(graphics, MIKE_TEXTURE, leftPos + 18, topPos + 50, 176F, 18F, 16, 16, 256, 256);
        else if (menu.isReady()) GuiDraw.blit(graphics, MIKE_TEXTURE, leftPos + 18, topPos + 50, 176F, 0F, 16, 16, 256, 256);

        if (menu.hasPart(0)) GuiDraw.blit(graphics, MIKE_TEXTURE, leftPos + 40, topPos + 36, 209F, 1F, 23, 23, 256, 256);
        if (menu.hasPart(1)) GuiDraw.blit(graphics, MIKE_TEXTURE, leftPos + 63, topPos + 36, 232F, 1F, 23, 23, 256, 256);
        if (menu.hasPart(2)) GuiDraw.blit(graphics, MIKE_TEXTURE, leftPos + 40, topPos + 59, 209F, 24F, 23, 23, 256, 256);
        if (menu.hasPart(3)) GuiDraw.blit(graphics, MIKE_TEXTURE, leftPos + 63, topPos + 59, 232F, 24F, 23, 23, 256, 256);
        if (menu.hasPart(5)) GuiDraw.blit(graphics, MIKE_TEXTURE, leftPos + 91, topPos + 41, 176F, 220F, 80, 36, 256, 256);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 16, 2);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.translatable("desc.gui.nukeTsar.desc.0"),
                    Component.translatable("desc.gui.nukeTsar.desc.1"),
                    Component.translatable("desc.gui.nukeTsar.desc.2"),
                    Component.translatable("desc.gui.nukeTsar.desc.3"),
                    Component.translatable("desc.gui.nukeTsar.desc.4")),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
