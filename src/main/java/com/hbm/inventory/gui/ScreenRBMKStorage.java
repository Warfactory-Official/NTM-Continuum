package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKStorage;
import dev.golem.core.Ids;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Twelve-slot fuel storage column inventory. */
public final class ScreenRBMKStorage extends RBMKContainerScreen<MenuRBMKStorage> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/rbmk/gui_rbmk_storage.png");

    public ScreenRBMKStorage(MenuRBMKStorage menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) { texture(graphics, TEX); }
}
