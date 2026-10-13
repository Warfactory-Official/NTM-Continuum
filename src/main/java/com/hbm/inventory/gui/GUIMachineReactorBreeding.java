package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineReactorBreeding;
import com.hbm.tileentity.machine.TileEntityMachineReactorBreeding;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** The 176x166 breeder screen: the 70-wide progress arrow and the flux readout above it. */
public final class GUIMachineReactorBreeding extends CompatContainerScreen<ContainerMachineReactorBreeding> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_breeder.png");

    public GUIMachineReactorBreeding(ContainerMachineReactorBreeding menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineReactorBreeding te = menu.getBreeder();
        if (te != null) {
            int i = te.getProgressScaled(70);
            if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 53, topPos + 32, 176F, 0F, i, 20, 256, 256);
        }
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 16, 3);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
        TileEntityMachineReactorBreeding te = menu.getBreeder();
        String flux = te == null ? "0" : String.valueOf(te.flux);
        GuiDraw.text(graphics, font, Component.literal(flux), 88 - font.width(flux) / 2, 21, 0xFF08FF00, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(-16, 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("The reactor has to recieve"),
                    Component.literal("neutron flux from adjacent"),
                    Component.literal("research reactors to breed.")), Optional.empty(), mouseX, mouseY);
        }
    }
}
