package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCoreReceiver;
import com.hbm.util.BobMathUtil;
import com.hbm.tileentity.machine.dfc.TileEntityCoreReceiver;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The receiver's panel, ported from 1.7 {@code GUICoreReceiver}: what came down the beam last tick, and what that is worth in HE. */
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

        drawTank(graphics, receiver.tank, 8, 17, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);

        TileEntityCoreReceiver receiver = menu.getReceiver();
        if (receiver != null) {
            // 1.7 GUICoreReceiver: input in Spk, output as joules * 5000 HE, all in 0xFF7F7F.
            GuiDraw.text(graphics, font, Component.literal("Input:"), 40, 25, 0xFF7F7F, false);
            GuiDraw.text(graphics, font, Component.literal(BobMathUtil.getShortNumber(receiver.joules) + "Spk"),
                    50, 35, 0xFF7F7F, false);
            GuiDraw.text(graphics, font, Component.literal("Output:"), 40, 45, 0xFF7F7F, false);
            GuiDraw.text(graphics, font, Component.literal(BobMathUtil.getShortNumber(receiver.joules * 5000) + "HE"),
                    50, 55, 0xFF7F7F, false);
        }

        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCoreReceiver receiver = menu.getReceiver();
        if (receiver == null) return;
        if (isHovering(8, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(receiver.tank), mouseX, mouseY);
        }
    }
}
