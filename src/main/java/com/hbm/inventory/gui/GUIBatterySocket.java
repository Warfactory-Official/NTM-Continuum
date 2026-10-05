package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerBatterySocket;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Client screen for the battery socket — 1.7.10 {@code GUIBatterySocket} ({@code storage/gui_battery_socket.png},
 * 176×181). The 34×52 bar at (62,17) shows the inserted battery item's charge; redstone-low (106,16) and
 * redstone-high (106,52) mode buttons and the priority button (125,35). Clicks go through {@code clickMenuButton}.
 */
public class GUIBatterySocket extends dev.golem.core.gui.CompatContainerScreen<ContainerBatterySocket> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_battery_socket.png");

    public GUIBatterySocket(ContainerBatterySocket menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 181);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (!this.menu.getBattery().isEmpty()) {
            long power = this.menu.getPower();
            long max = this.menu.getMaxPower();
            if (power > Long.MAX_VALUE / 100) { power /= 100; max /= 100; }
            if (max <= 1) max = 1;
            int p = (int) (power * 52 / max); // won't work when flying too close to the sun (1.7 comment: LONG limits)
            if (p > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 62, this.topPos + 69 - p, 176.0F, (float) (52 - p), 34, p, 256, 256);
        }

        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 106, this.topPos + 16, 176.0F, (float) (52 + this.menu.getRedLow() * 18), 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 106, this.topPos + 52, 176.0F, (float) (52 + this.menu.getRedHigh() * 18), 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 125, this.topPos + 35, 194.0F, (float) (52 + this.menu.getPriorityOrdinal() * 16 - 16), 16, 16, 256, 256);
    }

    /** 1.7: centred title at y=6, inventory at (8, ySize-96+2), both 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    /** 1.7 {@code drawScreen}: the charge readout over the bar (battery present only), then the priority text. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int gl = this.leftPos, gt = this.topPos;
        if (!this.menu.getBattery().isEmpty() && GuiUtil.hover(mouseX, mouseY, gl + 62, gt + 69 - 52, 34, 52)) {
            long delta = this.menu.getDelta();
            String deltaText = BobMathUtil.getShortNumber(Math.abs(delta)) + "HE/s";
            if (delta > 0) deltaText = ChatFormatting.GREEN + "+" + deltaText;
            else if (delta < 0) deltaText = ChatFormatting.RED + "-" + deltaText;
            else deltaText = ChatFormatting.YELLOW + "+" + deltaText;
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines(
                    BobMathUtil.getShortNumber(this.menu.getPower()) + "/" + BobMathUtil.getShortNumber(this.menu.getMaxPower()) + "HE",
                    deltaText), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 125, gt + 35, 16, 16)) {
            int ordinal = this.menu.getPriorityOrdinal();
            String lang = ordinal == 1 ? "low" : ordinal == 3 ? "high" : "normal";
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("battery.priority." + lang));
            lines.add(Component.translatable("battery.priority.recommended"));
            lines.addAll(GuiUtil.keyArray("battery.priority." + lang + ".desc"));
            GuiUtil.tooltip(graphics, this.font, lines, mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int button = -1;
        // 1.7 checkClick boxes: x [x, x+w), y (y, y+h].
        if (GuiUtil.hover(event.x(), event.y(), this.leftPos + 106, this.topPos + 16, 18, 18)) button = 0;
        else if (GuiUtil.hover(event.x(), event.y(), this.leftPos + 106, this.topPos + 52, 18, 18)) button = 1;
        else if (GuiUtil.hover(event.x(), event.y(), this.leftPos + 125, this.topPos + 35, 16, 16)) button = 2;

        if (button >= 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                GuiUtil.playClickSound();
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
                return true;
            }
        }
        return false;
    }
}
