package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeGadget;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** The Gadget schematic screen and its four lens/ready overlays. */
public class GUINukeGadget extends dev.golem.core.gui.CompatContainerScreen<ContainerNukeGadget> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/weapon/gadgetschematic.png");

    public GUINukeGadget(ContainerNukeGadget menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (menu.exp1()) GuiDraw.blit(graphics, TEXTURE, leftPos + 82, topPos + 19, 176F, 0F, 24, 24, 256, 256);
        if (menu.exp2()) GuiDraw.blit(graphics, TEXTURE, leftPos + 106, topPos + 19, 200F, 0F, 24, 24, 256, 256);
        if (menu.exp3()) GuiDraw.blit(graphics, TEXTURE, leftPos + 82, topPos + 43, 176F, 24F, 24, 24, 256, 256);
        if (menu.exp4()) GuiDraw.blit(graphics, TEXTURE, leftPos + 106, topPos + 43, 200F, 24F, 24, 24, 256, 256);
        if (menu.isReady()) GuiDraw.blit(graphics, TEXTURE, leftPos + 134, topPos + 35, 176F, 48F, 16, 16, 256, 256);
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 16, 2);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.translatable("desc.gui.nukeGadget.desc.0"),
                    Component.translatable("desc.gui.nukeGadget.desc.1"),
                    Component.translatable("desc.gui.nukeGadget.desc.2"),
                    Component.translatable("desc.gui.nukeGadget.desc.3")),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
