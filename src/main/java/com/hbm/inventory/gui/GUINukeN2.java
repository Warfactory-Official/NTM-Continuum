package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeN2;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** N² schematic screen, ported from 1.7 {@code GUINukeN2}: twelve slots and the stacked charge-fill indicator. */
public class GUINukeN2 extends dev.golem.core.gui.CompatContainerScreen<ContainerNukeN2> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/weapon/n2schematic.png");

    public GUINukeN2(ContainerNukeN2 menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int count = this.menu.countCharges();
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        if (count > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 35, topPos + 120 - 6 * count,
                    176F, 0F, 34, 6 * count, 256, 256);
        }
    }
}
