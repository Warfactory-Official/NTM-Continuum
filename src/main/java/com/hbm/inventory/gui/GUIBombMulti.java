package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerBombMulti;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1.7 {@code GUIBombMulti}: {@code bombGeneric.png} (176x166) with an 18x18 payload indicator at
 * (124, 34) taken from column 176 of the texture, row {@code type * 18}, when both payload slots agree,
 * and the mismatch icon at row 7 when they do not.
 */
public class GUIBombMulti extends CompatContainerScreen<ContainerBombMulti> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/weapon/bomb_generic.png");

    public GUIBombMulti(ContainerBombMulti menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        int type2 = menu.return2type();
        int type5 = menu.return5type();
        if (type2 == type5) {
            if (type2 >= 1 && type2 <= 6) {
                GuiDraw.blit(graphics, TEXTURE, x + 124, y + 34, 176.0F, (type2 - 1) * 18, 18, 18, 256, 256);
            }
        } else {
            GuiDraw.blit(graphics, TEXTURE, x + 124, y + 34, 176.0F, 7 * 18, 18, 18, 256, 256);
        }
    }
}
