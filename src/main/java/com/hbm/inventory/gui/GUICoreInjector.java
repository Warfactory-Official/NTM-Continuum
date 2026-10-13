package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCoreInjector;
import com.hbm.tileentity.machine.dfc.TileEntityCoreInjector;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** The injector's panel: the two fuel tanks and the identifier bay that picks what each one carries. */
public class GUICoreInjector extends GUIDFCBase<ContainerCoreInjector> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/dfc/gui_injector.png");

    public GUICoreInjector(ContainerCoreInjector menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityCoreInjector injector = menu.getInjector();
        if (injector == null) return;

        drawTank(graphics, injector.tanks[0], 44, 17, 16, 52);
        drawTank(graphics, injector.tanks[1], 116, 17, 16, 52);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityCoreInjector injector = menu.getInjector();
        if (injector == null) return;

        if (isHovering(44, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(injector.tanks[0]), mouseX, mouseY);
        } else if (isHovering(116, 17, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(injector.tanks[1]), mouseX, mouseY);
        }
    }
}
