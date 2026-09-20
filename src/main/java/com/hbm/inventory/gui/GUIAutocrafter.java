package com.hbm.inventory.gui;

import com.hbm.modules.ModulePatternMatcher;
import com.hbm.inventory.container.ContainerAutocrafter;
import com.hbm.tileentity.machine.TileEntityMachineAutocrafter;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/** 1.7 {@code GUIAutocrafter}: {@code gui_autocrafter.png}, 176x240, power bar 16x52 at (17,45). */
public final class GUIAutocrafter extends CompatContainerScreen<ContainerAutocrafter> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_autocrafter.png");

    public GUIAutocrafter(ContainerAutocrafter menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 240);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineAutocrafter crafter = menu.getAutocrafter();
        if (crafter == null) return;

        int i = (int) (crafter.getPower() * 52 / crafter.getMaxPower());
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 17, topPos + 97 - i, 176F, 52F - i, 16, i, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineAutocrafter crafter = menu.getAutocrafter();
        if (crafter == null) return;

        if (isHovering(17, 45, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(crafter.getPower(), crafter.getMaxPower()),
                    Optional.empty(), mouseX, mouseY);
            return;
        }

        if (!menu.getCarried().isEmpty()) return;

        for (int i = 0; i < 9; ++i) {
            Slot slot = menu.slots.get(i);
            if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && crafter.matcher.modes[i] != null) {
                GuiDraw.tooltip(graphics, font, List.of(
                        Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                        Component.literal(ModulePatternMatcher.getLabel(crafter.matcher.modes[i]))),
                        Optional.empty(), mouseX, mouseY - 30);
                return;
            }
        }

        Slot slot = menu.slots.get(9);
        if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && slot.hasItem()) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                    Component.literal((crafter.recipeIndex + 1) + " / " + crafter.recipeCount).withStyle(ChatFormatting.YELLOW)),
                    Optional.empty(), mouseX, mouseY - 30);
        }
    }
}
