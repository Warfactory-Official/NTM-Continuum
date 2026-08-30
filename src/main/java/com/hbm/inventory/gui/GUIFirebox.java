package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerFirebox;
import com.hbm.tileentity.machine.TileEntityFireboxBase;
import com.hbm.tileentity.machine.TileEntityHeaterOven;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the firebox family. Reads the heat/burn state off the synced client
 * {@link TileEntityFireboxBase} (BufPacket): the horizontal heat bar, the burn-time bar and the flame
 * icon while burning — texture layout matches the 1.12.2 {@code gui_firebox.png} (176×168, bars at
 * tex 176,0 / 176,5 and the 18×18 flame at 176,10).
 */
public class GUIFirebox extends dev.golem.core.gui.CompatContainerScreen<ContainerFirebox> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_firebox.png");
    private static final Identifier OVEN_TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_heating_oven.png");

    public GUIFirebox(ContainerFirebox menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 168);
    }

    /** The 1.12.2 GUI centred its title. */
    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TileEntityFireboxBase te = this.menu.getFirebox();
        Identifier texture = te instanceof TileEntityHeaterOven ? OVEN_TEXTURE : TEXTURE;
        GuiDraw.blit(graphics, texture, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (te == null) return;

        // Heat bar: 69×5 at (81, 28) from tex (176, 0).
        int i = te.heatEnergy * 69 / te.getMaxHeat();
        if (i > 0) {
            GuiDraw.blit(graphics, texture, this.leftPos + 81, this.topPos + 28, 176.0F, 0.0F, i, 5, 256, 256);
        }

        // Burn-time bar: 70×5 at (81, 37) from tex (176, 5).
        int j = te.burnTime * 70 / Math.max(te.maxBurnTime, 1);
        if (j > 0) {
            GuiDraw.blit(graphics, texture, this.leftPos + 81, this.topPos + 37, 176.0F, 5.0F, j, 5, 256, 256);
        }

        // Flame while burning: 18×18 at (25, 26) from tex (176, 10).
        if (te.wasOn) {
            GuiDraw.blit(graphics, texture, this.leftPos + 25, this.topPos + 26, 176.0F, 10.0F, 18, 18, 256, 256);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFireboxBase te = this.menu.getFirebox();
        if (te == null) return;
        if (this.isHovering(80, 27, 71, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal(String.format("%,d", te.heatEnergy) + " / " + String.format("%,d", te.getMaxHeat()) + " TU")), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(80, 36, 71, 7, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal(te.burnHeat + " TU/t"),
                    Component.literal((te.burnTime / 20) + "s")), Optional.empty(), mouseX, mouseY);
        }
    }
}
