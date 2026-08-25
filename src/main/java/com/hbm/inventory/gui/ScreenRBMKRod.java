package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKRod;
import com.hbm.items.machine.ItemRBMKRod;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ScreenRBMKRod extends RBMKContainerScreen<MenuRBMKRod> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/rbmk/gui_rbmk_element.png");

    public ScreenRBMKRod(MenuRBMKRod menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        texture(graphics, TEX);
        ItemStack fuel = menu.getSlot(0).getItem();
        if (!(fuel.getItem() instanceof ItemRBMKRod)) return;
        int depletion = (int) ((1D - ItemRBMKRod.getEnrichment(fuel)) * 67);
        GuiDraw.blit(graphics, TEX, leftPos + 34, topPos + 21, 176, 0, 18, 67, 256, 256);
        GuiDraw.blit(graphics, TEX, leftPos + 34, topPos + 21, 194, 0, 18, depletion, 256, 256);
        int xenon = (int) (ItemRBMKRod.getPoisonLevel(fuel) * 58);
        GuiDraw.blit(graphics, TEX, leftPos + 126, topPos + 82 - xenon, 212, 58 - xenon, 14, xenon, 256, 256);
        double hullHeat = ItemRBMKRod.getHullHeat(fuel);
        if (hullHeat > 1_000D) GuiUtil.drawWarningPanel(graphics, leftPos - 16, topPos + 20);
        if (hullHeat > 200D) GuiUtil.drawWarningPanel(graphics, leftPos - 16, topPos + 36);
    }

    @Override protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        ItemStack fuel = menu.getSlot(0).getItem();
        if (!(fuel.getItem() instanceof ItemRBMKRod)) return;
        if (region(34, 21, 18, 67, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Depletion: " + format((1D - ItemRBMKRod.getEnrichment(fuel)) * 100D) + "%")), mouseX, mouseY);
        } else if (region(126, 24, 14, 58, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Xenon poison: " + format(ItemRBMKRod.getPoison(fuel)) + "%")), mouseX, mouseY);
        }
        if (ItemRBMKRod.getHullHeat(fuel) > 1_000D && region(-16, 20, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Fuel skin temperature has exceeded 1,000 C,"), Component.literal("autoloaders can no longer cycle fuel!")), mouseX, mouseY);
        } else if (ItemRBMKRod.getHullHeat(fuel) > 200D && region(-16, 36, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Fuel skin temperature has exceeded 200 C,"), Component.literal("fuel can no longer be removed by hand!")), mouseX, mouseY);
        }
    }

    private static String format(double value) { return String.valueOf(Math.round(value * 100D) / 100D); }
}
