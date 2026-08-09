package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCrate;
import com.hbm.tileentity.machine.storage.CrateType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the storage crates. One screen serves all three: it sizes itself and picks its
 * background texture / label colours from the menu's {@link CrateType} (ported from the 1.12.2
 * {@code GUICrateBase}, which read the same constants off the crate tile). 26.1 render API
 * (extractBackground + blit; labels via extractLabels).
 */
public class GUICrate extends AbstractContainerScreen<ContainerCrate> {

    private final CrateType type;

    public GUICrate(ContainerCrate menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, menu.getCrateType().guiWidth, menu.getCrateType().guiHeight);
        this.type = menu.getCrateType();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, type.texture, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Title centred in the type's colour; player-inventory label at the type's offset/colour.
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, this.titleLabelY,
                0xFF000000 | type.titleColor, false);
        graphics.text(this.font, this.playerInventoryTitle, type.inventoryLabelX, this.imageHeight - 96 + 2,
                0xFF000000 | type.inventoryLabelColor, false);
    }
}
