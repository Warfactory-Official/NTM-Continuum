package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineRadGen;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Compact RadGen screen with one progress bar and one output bar per channel. */
public class GUIMachineRadGen extends CompatContainerScreen<ContainerMachineRadGen> {
    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_radgen.png");

    public GUIMachineRadGen(ContainerMachineRadGen menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 184);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineRadGen generator = getGenerator();
        if (generator == null) return;

        for (int i = 0; i < TileEntityMachineRadGen.CHANNELS; i++) {
            int y = topPos + 19 + i * 5;
            int progress = generator.getProgressScaled(i, 44);
            if (progress > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 66, y, 176F, 0F, progress, 3, 256, 256);
        }
        int power = generator.getPowerScaled(48);
        if (power > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 64, topPos + 83, 176F, 3F, power, 4, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, 90, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineRadGen generator = getGenerator();
        if (generator == null) return;

        for (int i = 0; i < TileEntityMachineRadGen.CHANNELS; i++) {
            if (generator.maxProgress[i] <= 0 || !isHovering(65, 18 + i * 5, 46, 5, mouseX, mouseY)) continue;
            int remaining = generator.maxProgress[i] - generator.progress[i];
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("Slot " + (i + 1) + ":"),
                    Component.literal(generator.production[i] + "HE/t for"),
                    Component.literal(remaining + " ticks (" + remaining * 100 / generator.maxProgress[i] + "%)")),
                    Optional.empty(), mouseX, mouseY);
            return;
        }

        if (isHovering(64, 83, 48, 4, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    GuiUtil.energyTooltip(generator.power, TileEntityMachineRadGen.MAX_POWER),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private TileEntityMachineRadGen getGenerator() {
        return menu.getGenerator();
    }
}
