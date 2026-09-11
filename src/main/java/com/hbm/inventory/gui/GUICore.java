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
 * The core's own panel: the two fuel columns down the sides, the two bars across the middle that are the
 * heat the reaction is making and the field the stabilisers are holding against it, and the lamp that
 * lights when an exotic core is seated.
 */
public class GUICore extends GUIDFCBase<ContainerCore> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/dfc/gui_core.png");

    public GUICore(ContainerCore menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
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

        int heat = Math.min(core.getHeatScaled(70), 70);
        if (heat > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 98, 0F, 204F, heat, 4, 256, 256);
        int field = Math.min(core.getFieldScaled(70), 70);
        if (field > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 102, 0F, 208F, field, 4, 256, 256);

        if (core.hasCore()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 70, topPos + 29, 220F, 0F, 36, 36, 256, 256);
        }

        drawTank(graphics, core.tanks[0], 8, 8, 16, 80);
        drawTank(graphics, core.tanks[1], 152, 8, 16, 80);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth - 8 - font.width(title), imageHeight - 96 + 2,
                0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCore core = menu.getCore();
        if (core == null) return;

        if (isHovering(8, 8, 16, 80, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(core.tanks[0]), mouseX, mouseY);
        } else if (isHovering(152, 8, 16, 80, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(core.tanks[1]), mouseX, mouseY);
        } else if (isHovering(53, 97, 70, 4, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Heat Saturation: " + core.heat + "%")), mouseX, mouseY);
        } else if (isHovering(53, 101, 70, 4, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Restriction Field: " + core.field + "%")), mouseX, mouseY);
        }
    }
}
