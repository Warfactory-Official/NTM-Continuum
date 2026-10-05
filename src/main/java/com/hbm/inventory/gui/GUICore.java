package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCore;
import com.hbm.tileentity.machine.dfc.TileEntityCore;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * The core's own panel, ported from 1.7 {@code GUICore}: 176x166, the restriction-field column on the left
 * (u 176), the heat-saturation column on the right (u 192), the two fuel tanks inside them and the three
 * catalyst/core slots in the middle.
 */
public class GUICore extends GUIDFCBase<ContainerCore> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/dfc/gui_core.png");

    public GUICore(ContainerCore menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityCore core = menu.getCore();
        if (core == null) return;

        int i = core.getFieldScaled(52);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 8, topPos + 69 - i, 176F, 52 - i, 16, i, 256, 256);

        int j = core.getHeatScaled(52);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 69 - j, 192F, 52 - j, 16, j, 256, 256);

        drawTank(graphics, core.tanks[0], 26, 17, 16, 52);
        drawTank(graphics, core.tanks[1], 134, 17, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCore core = menu.getCore();
        if (core == null) return;

        if (isHovering(26, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(core.tanks[0]), mouseX, mouseY);
        } else if (isHovering(134, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(core.tanks[1]), mouseX, mouseY);
        } else if (isHovering(8, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Restriction Field: " + core.field + "%")), mouseX, mouseY);
        } else if (isHovering(152, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Heat Saturation: " + core.heat + "%")), mouseX, mouseY);
        }
    }
}
