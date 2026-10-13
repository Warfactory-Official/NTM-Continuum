package com.hbm.inventory.gui;

import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerWasteDrum;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Spent-fuel pool drum screen, a straight port of 1.7 {@code GUIWasteDrum} ({@code gui_waste_drum.png}, 176x189). */
public class GUIWasteDrum extends CompatContainerScreen<ContainerWasteDrum> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/gui_waste_drum.png");

    public GUIWasteDrum(ContainerWasteDrum menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 189);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        // 1.7: large blue "i" off the left edge.
        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 36, 2);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 draws "Inventory" at ySize - 96 + 5, three pixels lower than most screens.
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, Component.translatable("container.inventory"), 8, imageHeight - 96 + 5, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 hard-codes this text and draws it at a fixed spot below the panel.
        if (GuiUtil.hover(mouseX, mouseY, leftPos - 16, topPos + 36, 16, 16)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.lines(
                    "The drum will cool down hot nuclear",
                    "waste when submerged in water. More",
                    "water speeds up the process."), Optional.empty(), leftPos - 8, topPos + 36 + 16);
        }
    }
}
