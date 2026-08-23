package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuRBMKOutgasser;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public final class ScreenRBMKOutgasser extends RBMKContainerScreen<MenuRBMKOutgasser> {
    private static final Identifier TEX = Ids.of("hbm", "textures/gui/rbmk/gui_rbmk_outgasser.png");

    public ScreenRBMKOutgasser(MenuRBMKOutgasser menu, Inventory inventory, Component title) { super(menu, inventory, title, 176, 186); }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        texture(graphics, TEX);
        BlockEntityRBMKOutgasser outgasser = menu.blockEntity();
        if (outgasser == null) return;
        int progress = outgasser.duration > 0 ? (int) (outgasser.progress * 45 / outgasser.duration) : 0;
        if (progress > 0) GuiDraw.blit(graphics, TEX, leftPos + 66, topPos + 58, 190, 0, progress, 6, 256, 256);
        int gas = outgasser.gas.getMaxFill() > 0 ? outgasser.gas.getFill() * 58 / outgasser.gas.getMaxFill() : 0;
        if (gas > 0) GuiDraw.blit(graphics, TEX, leftPos + 143, topPos + 82 - gas, 176, 58 - gas, 14, gas, 256, 256);
    }

    @Override protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        BlockEntityRBMKOutgasser outgasser = menu.blockEntity();
        if (outgasser == null) return;
        fluidTooltip(graphics, outgasser.gas, 143, 24, 14, 58, mouseX, mouseY);
        if (region(20, 32, 103, 10, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("Flux: " + (int) outgasser.progress + " / " + outgasser.duration)), mouseX, mouseY);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        BlockEntityRBMKOutgasser outgasser = menu.blockEntity();
        if (outgasser == null) return;
        GuiDraw.text(graphics, font, Component.literal("Flux"), 21, 34, 0xFF404040, false);
        String flux = formatNumber((float) outgasser.progress) + "/" + formatNumber((float) outgasser.duration);
        GuiDraw.text(graphics, font, Component.literal(flux), 123 - font.width(flux), 34, 0xFF46EA00, false);
    }

    private static String formatNumber(float number) {
        if (number < 1_000D) return String.format("%5.1f ", number);
        if (number < 1_000_000D) return String.format("%5.1fk", number / 1_000F);
        if (number < 1_000_000_000D) return String.format("%5.1fM", number / 1_000_000F);
        if (number < 1_000_000_000_000D) return String.format("%5.1fG", number / 1_000_000_000F);
        if (number < 1_000_000_000_000_000D) return String.format("%5.1fT", number / 1_000_000_000_000F);
        return "";
    }
}
