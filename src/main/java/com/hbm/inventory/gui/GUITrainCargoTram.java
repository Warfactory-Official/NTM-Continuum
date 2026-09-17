package com.hbm.inventory.gui;

import com.hbm.inventory.container.MenuTrainCargoTram;
import com.hbm.lib.Library;
import com.hbm.util.BobMathUtil;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** 1.7 GUITrainCargoTram (ntm-next ScreenTrainCargoTram): cargo grid, charge slot and the power gauge. */
public class GUITrainCargoTram extends CompatContainerScreen<MenuTrainCargoTram> {

    private static final Identifier TEXTURE = Library.id("textures/gui/vehicles/gui_cargo_tram.png");

    private static final int POWER_X = 152, POWER_Y = 18, POWER_W = 16, POWER_H = 52;
    private static final int POWER_BOTTOM = 70, POWER_U = 176, POWER_V_BOTTOM = 52;
    private static final int LIT_X = 156, LIT_Y = 4;

    public GUITrainCargoTram(MenuTrainCargoTram menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 204);
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        int fill = menu.getMaxPower() <= 0 ? 0 : menu.getPower() * POWER_H / menu.getMaxPower();
        if (fill > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + POWER_X, topPos + POWER_BOTTOM - fill,
                    POWER_U, POWER_V_BOTTOM - fill, POWER_W, fill, 256, 256);
        }

        if (menu.getPower() > menu.getPowerConsumption()) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + LIT_X, topPos + LIT_Y, POWER_U, POWER_V_BOTTOM, 9, 12, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 centred the name on 140 rather than on the 176 the window actually is
        GuiDraw.text(graphics, font, title, 70 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, inventoryLabelY, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(POWER_X, POWER_Y, POWER_W, POWER_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal(
                    BobMathUtil.getShortNumber(menu.getPower()) + "/" + BobMathUtil.getShortNumber(menu.getMaxPower()) + "HE")),
                    Optional.empty(), mouseX, mouseY);
        }
    }
}
