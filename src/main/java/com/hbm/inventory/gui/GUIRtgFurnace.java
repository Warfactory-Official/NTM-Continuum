package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerRtgFurnace;
import com.hbm.tileentity.machine.TileEntityRtgFurnace;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUIRtgFurnace}: {@code RTGfurnace.png}, 176x166. */
public final class GUIRtgFurnace extends CompatContainerScreen<ContainerRtgFurnace> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/RTGfurnace.png");

    public GUIRtgFurnace(ContainerRtgFurnace menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityRtgFurnace furnace = menu.getFurnace();
        if (furnace == null) return;

        if (furnace.hasPower()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 55, topPos + 35, 176F, 0F, 18, 16, 256, 256);
        }

        int j1 = furnace.getDiFurnaceProgressScaled(24);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 79, topPos + 34, 176F, 16F, j1 + 1, 17, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }
}
