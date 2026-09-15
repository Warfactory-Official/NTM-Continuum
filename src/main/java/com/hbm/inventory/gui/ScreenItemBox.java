package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuItemBox;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Shared source-shaped screen for the ammo and casing bag menus. */
public final class ScreenItemBox extends CompatContainerScreen<MenuItemBox> {

    public ScreenItemBox(MenuItemBox menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, menu.layout().width(), menu.layout().height());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        MenuItemBox.Layout layout = menu.layout();
        GuiDraw.blit(graphics, layout.texture(), leftPos, topPos, 0F, 0F,
                layout.width(), layout.height(), 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        MenuItemBox.Layout layout = menu.layout();
        GuiDraw.text(graphics, font, title, (layout.width() - font.width(title)) / 2,
                layout.titleY(), 0xFFFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, layout.inventoryLabelY(),
                0xFF404040, false);
    }
}
