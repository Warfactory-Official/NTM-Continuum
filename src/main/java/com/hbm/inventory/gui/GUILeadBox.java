package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerLeadBox;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUILeadBox}: {@code gui_containment.png}, 176x186, the box's name centred at y 6. */
public class GUILeadBox extends CompatContainerScreen<ContainerLeadBox> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_containment.png");
    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 186;

    public GUILeadBox(ContainerLeadBox menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, X_SIZE, Y_SIZE);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        String name = title.getString();
        GuiDraw.text(graphics, font, name, X_SIZE / 2 - font.width(name) / 2, 6, 0xFF404040, false);
        String inv = Component.translatable("container.inventory").getString();
        GuiDraw.text(graphics, font, inv, 8, Y_SIZE - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, X_SIZE, Y_SIZE, 256, 256);
    }
}
