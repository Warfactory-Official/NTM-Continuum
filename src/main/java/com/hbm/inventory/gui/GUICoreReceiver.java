package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCoreReceiver;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The receiver's panel: what came down the beam last tick, and what that is worth in HE. */
public class GUICoreReceiver extends GUIDFCBase<ContainerCoreReceiver> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/dfc/gui_receiver.png");

    public GUICoreReceiver(ContainerCoreReceiver menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityCoreReceiver receiver = menu.getReceiver();
        if (receiver == null) return;

        drawTank(graphics, receiver.tank, 17, 17, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics);

        TileEntityCoreReceiver receiver = menu.getReceiver();
        if (receiver == null) return;

        GuiDraw.text(graphics, font, Component.literal("Input:"), 54, 22, 0xFF404040, false);
        Component sparks = Component.literal(Library.getShortNumber(receiver.joules) + "SPK");
        GuiDraw.text(graphics, font, sparks, 161 - font.width(sparks), 22, 0xFF4EB3DB, false);

        GuiDraw.text(graphics, font, Component.literal("Output:"), 54, 58, 0xFF404040, false);
        Component power = Component.literal(Library.getShortNumber(receiver.power) + "HE/t");
        GuiDraw.text(graphics, font, power, 161 - font.width(power), 58, 0xFF4EB3DB, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCoreReceiver receiver = menu.getReceiver();
        if (receiver == null) return;
        if (isHovering(17, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(receiver.tank), mouseX, mouseY);
        }
    }
}
