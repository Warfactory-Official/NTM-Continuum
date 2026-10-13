package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerBatteryREDD;
import com.hbm.lib.Library;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/** The FEnSU (REDD) screen, ported from 1.7.10 {@code GUIBatteryREDD} ({@code storage/gui_battery_redd.png}, 176x181). 1.7 has no hover tooltips here. */
public final class GUIBatteryREDD extends CompatContainerScreen<ContainerBatteryREDD> {

    private static final Identifier TEXTURE = Ids.of(
            "hbm", "textures/gui/storage/gui_battery_redd.png");

    public GUIBatteryREDD(ContainerBatteryREDD menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 181);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 133, topPos + 16,
                176F, 52F + menu.getRedLow() * 18F, 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 133, topPos + 52,
                176F, 52F + menu.getRedHigh() * 18F, 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 35,
                194F, 52F + (menu.getPriorityOrdinal() - 1) * 16F, 16, 16, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);

        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 0.5F, 0.5F);
        String power = String.format(Locale.US, "%,d", menu.getPower()) + " HE";
        String delta = String.format(Locale.US, "%,d", menu.getDelta()) + " HE/s";
        // 1.7: the colour comes from the formatting prefix (the 0x00ff00 base colour only shows for the power line).
        int comp = menu.getDelta().signum();
        if (comp > 0) delta = ChatFormatting.GREEN + "+" + delta;
        else if (comp < 0) delta = ChatFormatting.RED + delta;
        else delta = ChatFormatting.YELLOW + "+" + delta;
        GuiDraw.text(graphics, font, power, 242 - font.width(power), 45, 0x00ff00, false);
        GuiDraw.text(graphics, font, delta, 242 - font.width(delta), 65, 0x00ff00, false);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int button = -1;
        // 1.7 checkClick boxes: x [x, x+w), y (y, y+h].
        if (GuiUtil.hover(event.x(), event.y(), leftPos + 133, topPos + 16, 18, 18)) button = 0;
        else if (GuiUtil.hover(event.x(), event.y(), leftPos + 133, topPos + 52, 18, 18)) button = 1;
        else if (GuiUtil.hover(event.x(), event.y(), leftPos + 152, topPos + 35, 16, 16)) button = 2;
        if (button >= 0 && Minecraft.getInstance().gameMode != null) {
            GuiUtil.playClickSound();
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, button);
            return true;
        }
        return false;
    }
}
