package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerNukeSolinium;
import com.hbm.tileentity.bomb.TileEntityNukeSolinium;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUINukeSolinium}: overlays drawn from the texture's bottom strip (v = 222 / 236), plus the ready lamp. */
public class GUINukeSolinium extends CompatContainerScreen<ContainerNukeSolinium> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/weapon/soliniumschematic.png");

    /** Per slot: destination x, y, source u, v, width, height. */
    private static final int[][] OVERLAYS = {
            {24, 84, 0, 222, 22, 14},
            {46, 84, 22, 222, 18, 14},
            {76, 84, 52, 222, 18, 14},
            {94, 84, 70, 222, 22, 14},
            {64, 84, 40, 222, 12, 28},
            {24, 98, 0, 236, 22, 14},
            {46, 98, 22, 236, 18, 14},
            {76, 98, 52, 236, 18, 14},
            {94, 98, 70, 236, 22, 14}
    };

    public GUINukeSolinium(ContainerNukeSolinium menu, Inventory playerInventory, Component title) {
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
        TileEntityNukeSolinium bomb = menu.getSolinium();
        if (bomb == null) return;
        for (int i = 0; i < OVERLAYS.length; i++) {
            if (!bomb.hasPart(i)) continue;
            int[] o = OVERLAYS[i];
            GuiDraw.blit(graphics, TEXTURE, leftPos + o[0], topPos + o[1], o[2], o[3], o[4], o[5], 256, 256);
        }
        if (bomb.isReady()) GuiDraw.blit(graphics, TEXTURE, leftPos + 134, topPos + 90, 176F, 0F, 16, 16, 256, 256);
    }
}
