package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeMike;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Ivy Mike's 176x217 legacy assembly schematic and installed-component overlays. */
public class GUINukeMike extends CompatContainerScreen<ContainerNukeMike> {

    private static final Identifier TEXTURE =
            Ids.of("hbm", "textures/gui/weapon/ivymikeschematic.png");

    public GUINukeMike(ContainerNukeMike menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 217);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6,
                0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94,
                0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        if (menu.isFilled()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 5, topPos + 35, 177F, 19F, 16, 16, 256, 256);
        } else if (menu.isReady()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 5, topPos + 35, 177F, 1F, 16, 16, 256, 256);
        }
        if (menu.hasPart(5)) GuiDraw.blit(graphics, TEXTURE, leftPos + 75, topPos + 25, 176F, 49F, 80, 36, 256, 256);
        if (menu.hasPart(6)) GuiDraw.blit(graphics, TEXTURE, leftPos + 79, topPos + 30, 180F, 88F, 58, 26, 256, 256);
        if (menu.hasPart(7)) GuiDraw.blit(graphics, TEXTURE, leftPos + 140, topPos + 30, 240F, 88F, 12, 26, 256, 256);
        if (menu.hasPart(0)) GuiDraw.blit(graphics, TEXTURE, leftPos + 24, topPos + 20, 209F, 1F, 23, 23, 256, 256);
        if (menu.hasPart(2)) GuiDraw.blit(graphics, TEXTURE, leftPos + 47, topPos + 20, 232F, 1F, 23, 23, 256, 256);
        if (menu.hasPart(1)) GuiDraw.blit(graphics, TEXTURE, leftPos + 24, topPos + 43, 209F, 24F, 23, 23, 256, 256);
        if (menu.hasPart(3)) GuiDraw.blit(graphics, TEXTURE, leftPos + 47, topPos + 43, 232F, 24F, 23, 23, 256, 256);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 16, 2);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.translatable("desc.gui.nukeMike.desc.0"),
                    Component.translatable("desc.gui.nukeMike.desc.1"),
                    Component.translatable("desc.gui.nukeMike.desc.2"),
                    Component.translatable("desc.gui.nukeMike.desc.3"),
                    Component.translatable("desc.gui.nukeMike.desc.4"),
                    Component.translatable("desc.gui.nukeMike.desc.5")),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
