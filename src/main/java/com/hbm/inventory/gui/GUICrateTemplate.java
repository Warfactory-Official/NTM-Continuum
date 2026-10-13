package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCrateTemplate;
import com.hbm.tileentity.machine.storage.TemplateCrateLayout;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Client screen for the ordinary template crate, using its exact 176x168 old layout. */
public class GUICrateTemplate extends CompatContainerScreen<ContainerCrateTemplate> {

    public GUICrateTemplate(ContainerCrateTemplate menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, TemplateCrateLayout.GUI_WIDTH, TemplateCrateLayout.GUI_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TemplateCrateLayout.TEXTURE, leftPos, topPos, 0F, 0F,
                imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 6,
                TemplateCrateLayout.LABEL_COLOR, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, TemplateCrateLayout.LABEL_X,
                imageHeight - 96 + 2, TemplateCrateLayout.LABEL_COLOR, false);
    }
}
