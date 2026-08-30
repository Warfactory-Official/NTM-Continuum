package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFurnaceBrick;
import com.hbm.tileentity.machine.TileEntityFurnaceBrick;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Brick furnace screen, preserving the old 176x166 layout. */
public class GUIFurnaceBrick extends dev.golem.core.gui.CompatContainerScreen<ContainerFurnaceBrick> {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_furnace_brick.png");

    public GUIFurnaceBrick(ContainerFurnaceBrick menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override protected void init() {
        super.init();
        titleLabelX = (imageWidth - font.width(title)) / 2;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityFurnaceBrick furnace = menu.getFurnace();
        if (furnace == null || furnace.burnTime <= 0) return;
        int burn = furnace.burnTime * 13 / Math.max(furnace.maxBurnTime, 1);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 54 + 12 - burn,
                176F, 12F - burn, 14, burn + 1, 256, 256);
        int progress = furnace.progress * 24 / 200;
        GuiDraw.blit(graphics, TEXTURE, leftPos + 85, topPos + 34,
                176F, 14F, progress + 1, 16, 256, 256);
    }
}
