package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.inventory.container.ContainerBatteryREDD;
import com.hbm.lib.Library;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** The current FEnSU screen, preserving the old 176x181 texture and button coordinates. */
public final class GUIBatteryREDD extends dev.golem.core.gui.CompatContainerScreen<ContainerBatteryREDD> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(
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
        GuiDraw.text(graphics, font, title, (imageWidth - font.width(title)) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);

        GuiDraw.pushMatrix(graphics);
        GuiDraw.scale(graphics, 0.5F, 0.5F);
        String power = String.format(Locale.US, "%,d", menu.getPower()) + " HE";
        String delta = String.format(Locale.US, "%,d", menu.getDelta()) + " HE/s";
        if (menu.getDelta().signum() >= 0) delta = "+" + delta;
        GuiDraw.text(graphics, font, power, 242 - font.width(power), 45, 0xFF00FF00, false);
        GuiDraw.text(graphics, font, delta, 242 - font.width(delta), 65, deltaColor(menu.getDelta()), false);
        GuiDraw.popMatrix(graphics);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (isHovering(133, 16, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, "No redstone: ", mode(menu.getRedLow()), mouseX, mouseY);
        } else if (isHovering(133, 52, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, "Redstone on: ", mode(menu.getRedHigh()), mouseX, mouseY);
        } else if (isHovering(152, 35, 16, 16, mouseX, mouseY)) {
            String name = priority(menu.getPriorityOrdinal());
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.translatable("battery.priority." + name.toLowerCase(Locale.ROOT)),
                    Component.translatable("battery.priority.recommended"),
                    Component.translatable("battery.priority." + name.toLowerCase(Locale.ROOT) + ".desc")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private void tooltip(GuiGraphicsExtractor graphics, String prefix, String value, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, List.of(Component.literal(prefix + value)), Optional.empty(), mouseX, mouseY);
    }

    private static String mode(int mode) {
        return switch (mode) {
            case 0 -> "Input only";
            case 1 -> "Buffer (both)";
            case 2 -> "Output only";
            case 3 -> "Disabled";
            default -> "?";
        };
    }

    private static String priority(int ordinal) {
        return switch (ordinal) {
            case 1 -> "Low";
            case 3 -> "High";
            default -> "Normal";
        };
    }

    private static int deltaColor(java.math.BigInteger delta) {
        return delta.signum() > 0 ? 0xFF00FF00 : delta.signum() < 0 ? 0xFFFF0000 : 0xFFFFFF00;
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int button = -1;
        if (isHovering(133, 16, 18, 18, event.x(), event.y())) button = 0;
        else if (isHovering(133, 52, 18, 18, event.x(), event.y())) button = 1;
        else if (isHovering(152, 35, 16, 16, event.x(), event.y())) button = 2;
        if (button >= 0 && Minecraft.getInstance().gameMode != null) {
            Gauges.click();
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, button);
            return true;
        }
        return false;
    }
}
