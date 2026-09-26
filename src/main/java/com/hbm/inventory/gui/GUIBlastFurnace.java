package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.inventory.container.ContainerBlastFurnace;
import com.hbm.tileentity.machine.TileEntityMachineBlastFurnace;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Blast furnace screen preserving the legacy 176x222 layout and two fluid gauges. */
public class GUIBlastFurnace extends dev.golem.core.gui.CompatContainerScreen<ContainerBlastFurnace> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_blast_furnace.png");

    public GUIBlastFurnace(ContainerBlastFurnace menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
        titleLabelY = 6;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineBlastFurnace furnace = menu.getFurnace();
        if (furnace == null) return;

        int fuel = Math.round(furnace.fuel * 26F / TileEntityMachineBlastFurnace.MAX_FUEL);
        int progress = Math.round(furnace.progress * (88F - fuel));
        if (progress > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 106 - progress - fuel,
                    176F, 102F - progress - fuel, 56, progress, 256, 256);
        }
        if (fuel > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 106 - fuel,
                    176F, 128F - fuel, 56, fuel, 256, 256);
        }
        if (furnace.isProgressing) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 81, topPos + 64, 176F, 0F, 14, 14, 256, 256);
        }

        Gauges.needle(graphics, leftPos + 34, topPos + 80,
                (double) furnace.tanks[0].getFill() / furnace.tanks[0].getMaxFill(), 0x800000);
        Gauges.needle(graphics, leftPos + 34, topPos + 26,
                (double) furnace.tanks[1].getFill() / furnace.tanks[1].getMaxFill(), 0x800000);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineBlastFurnace furnace = menu.getFurnace();
        if (furnace == null) return;
        if (isHovering(79, 62, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    List.of(Component.literal("Speed: " + (int) (furnace.speed * 100F) + "%")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(25, 71, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.translatable("hbmfluid.airblast"),
                    Component.literal(furnace.tanks[0].getFill() + " / " + furnace.tanks[0].getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(25, 17, 18, 18, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.translatable("hbmfluid.flue"),
                    Component.literal(furnace.tanks[1].getFill() + " / " + furnace.tanks[1].getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(62, 18, 56, 88, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(String.format(Locale.US,
                    "%.0f%%", furnace.progress * 100F))), Optional.empty(), mouseX, mouseY);
        }
    }
}
