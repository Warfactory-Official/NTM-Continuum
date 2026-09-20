package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerDroneRequester;
import com.hbm.tileentity.network.TileEntityDroneRequester;
import com.hbm.modules.ModulePatternMatcher;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/** 1.7 {@code GUIDroneRequester}: the delivery bank, the filters, and the filters' match-mode tooltips. */
public class GUIDroneRequester extends PneumaticScreenBase<ContainerDroneRequester> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/storage/gui_drone_requester.png");

    public GUIDroneRequester(ContainerDroneRequester menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 186);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityDroneRequester requester = menu.tile();
        if (requester == null || !menu.getCarried().isEmpty()) return;

        for (int i = 0; i < TileEntityDroneRequester.FILTER_COUNT; ++i) {
            Slot slot = menu.slots.get(i);
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && requester.matcher.modes[i] != null) {
                GuiDraw.tooltip(graphics, font, List.of(
                        Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                        Component.literal(ModulePatternMatcher.getLabel(requester.matcher.modes[i]))),
                        Optional.empty(), mouseX, mouseY - 30);
                return;
            }
        }
    }
}
