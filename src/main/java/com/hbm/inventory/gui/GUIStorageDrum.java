package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerStorageDrum;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/** Faithful 176x237 storage-drum screen, including the two vertical tank columns. */
public class GUIStorageDrum extends dev.golem.core.gui.CompatContainerScreen<ContainerStorageDrum> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_drum.png");

    public GUIStorageDrum(ContainerStorageDrum menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 237);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        if (menu.getDrum() == null) return;
        FluidTankNTM liquid = menu.getDrum().liquid;
        FluidTankNTM gas = menu.getDrum().gas;
        int liquidHeight = liquid.getMaxFill() <= 0 ? 0 : liquid.getFill() * 106 / liquid.getMaxFill();
        if (liquidHeight > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 17, topPos + 130 - liquidHeight,
                    176F, 106F - liquidHeight, 7, liquidHeight, 256, 256);
        }
        int gasHeight = gas.getMaxFill() <= 0 ? 0 : gas.getFill() * 106 / gas.getMaxFill();
        if (gasHeight > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 130 - gasHeight,
                    183F, 106F - gasHeight, 7, gasHeight, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.getDrum() == null) return;
        if (isHovering(16, 23, 9, 108, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(menu.getDrum().liquid), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(151, 23, 9, 108, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(menu.getDrum().gas), Optional.empty(), mouseX, mouseY);
        }
    }
}
