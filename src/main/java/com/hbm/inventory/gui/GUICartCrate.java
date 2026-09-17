package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuCrate;
import com.hbm.tileentity.machine.storage.CrateType;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The crate minecart's screen: {@link GUICrate}'s drawing over the cart's {@link MenuCrate}. */
public class GUICartCrate extends dev.golem.core.gui.CompatContainerScreen<MenuCrate> {

    private final CrateType type;

    public GUICartCrate(MenuCrate menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, menu.getCrateType().guiWidth, menu.getCrateType().guiHeight);
        this.type = menu.getCrateType();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, type.texture, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, this.titleLabelY,
                0xFF000000 | type.titleColor, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, type.inventoryLabelX, this.imageHeight - 96 + 2,
                0xFF000000 | type.inventoryLabelColor, false);
    }
}
