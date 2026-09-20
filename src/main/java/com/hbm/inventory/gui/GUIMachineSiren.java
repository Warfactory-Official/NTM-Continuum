package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineSiren;
import com.hbm.items.machine.ItemCassette.TrackType;
import com.hbm.tileentity.machine.TileEntityMachineSiren;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 1.7 {@code GUIMachineSiren}: {@code gui_siren.png}, 176x166; the cassette's title, type and range in its colour. */
public final class GUIMachineSiren extends CompatContainerScreen<ContainerMachineSiren> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_siren.png");

    public GUIMachineSiren(ContainerMachineSiren menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);

        // Draw record meta here //
        TileEntityMachineSiren siren = menu.getSiren();
        if (siren == null) return;
        TrackType track = siren.getCurrentType();
        if (track != TrackType.NULL) {
            int color = track.getColor();
            GuiDraw.text(graphics, font, track.getTrackTitle(), 46, 28, color, false);
            GuiDraw.text(graphics, font, "Type: " + track.getType().name(), 46, 40, color, false);
            GuiDraw.text(graphics, font, "Volume: " + track.getVolume(), 46, 52, color, false);
        }
    }
}
