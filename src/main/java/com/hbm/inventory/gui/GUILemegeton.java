package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerLemegeton;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.Fonts;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.7 {@code GUILemegeton}: {@code gui_lemegeton.png}, the two labels in the standard galactic alphabet, and
 * the conversion band (0, 166) lit while the result slot holds something.
 */
public class GUILemegeton extends CompatContainerScreen<ContainerLemegeton> {

    public static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_lemegeton.png");
    private static final int X_SIZE = 176;
    private static final int Y_SIZE = 166;

    public GUILemegeton(ContainerLemegeton menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, X_SIZE, Y_SIZE);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, Fonts.galactic("Material Upgrade Conversion"), 28, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, Fonts.galactic("Standard Inventory"), 8, Y_SIZE - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, X_SIZE, Y_SIZE, 256, 256);
        if (menu.getSlot(0).hasItem()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 7, topPos + 22, 0F, 166F, 162, 42, 256, 256);
        }
    }
}
