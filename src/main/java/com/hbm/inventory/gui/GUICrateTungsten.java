package com.hbm.inventory.gui;

import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.inventory.container.ContainerCrateTungsten;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.7 {@code GUICrateTungsten}: 176x168, the hot sheet while a laser heats the crate, white labels (the sheet is
 * dark).
 */
public class GUICrateTungsten extends CompatContainerScreen<ContainerCrateTungsten> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_crate_tungsten.png");
    private static final Identifier TEXTURE_HOT = Ids.of("hbm", "textures/gui/storage/gui_crate_tungsten_hot.png");

    public GUICrateTungsten(ContainerCrateTungsten menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 168);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean hot = menu.tile() != null && menu.tile().getBlockState().hasProperty(BlockStorageCrate.HOT)
                && menu.tile().getBlockState().getValue(BlockStorageCrate.HOT);
        GuiDraw.blit(graphics, hot ? TEXTURE_HOT : TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFFFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFFFFFFFF, false);
    }
}
