package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeFleija;
import com.hbm.tileentity.bomb.TileEntityNukeFleija;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUINukeFleija}: the schematic with one overlay per correctly filled slot. */
public class GUINukeFleija extends CompatContainerScreen<ContainerNukeFleija> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/weapon/fleijaschematic.png");

    /** Per slot: destination x, y, source u, v, width, height. */
    private static final int[][] OVERLAYS = {
            {7, 88, 176, 0, 30, 20},
            {139, 88, 206, 0, 30, 20},
            {57, 77, 176, 62, 18, 14},
            {57, 91, 176, 76, 18, 14},
            {57, 105, 176, 90, 18, 14},
            {85, 77, 176, 20, 18, 15},
            {103, 77, 194, 20, 18, 15},
            {85, 92, 176, 35, 18, 12},
            {103, 92, 194, 35, 18, 12},
            {85, 104, 176, 47, 18, 15},
            {103, 104, 194, 47, 18, 15}
    };

    public GUINukeFleija(ContainerNukeFleija menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityNukeFleija bomb = menu.getFleija();
        if (bomb == null) return;
        for (int i = 0; i < OVERLAYS.length; i++) {
            if (!bomb.hasPart(i)) continue;
            int[] o = OVERLAYS[i];
            GuiDraw.blit(graphics, TEXTURE, leftPos + o[0], topPos + o[1], o[2], o[3], o[4], o[5], 256, 256);
        }
    }
}
