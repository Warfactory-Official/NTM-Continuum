package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineBattery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Client screen for the energy storage blocks — 1.7.10 {@code GUIMachineBattery} (texture
 * {@code storage/gui_battery.png}, 176×166): energy bar 52×52 at (62,17), the unpowered and redstone-powered
 * I/O-mode buttons at (133,16) and (133,52), the priority button at (152,35), and the title followed by
 * the stored power in HE. Clicks dispatch through the vanilla {@code clickMenuButton} path.
 */
public class GUIMachineBattery extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineBattery> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_battery.png");

    public GUIMachineBattery(ContainerMachineBattery menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component name = Component.literal(this.title.getString() + " (" + this.menu.getPower() + " HE)");
        GuiDraw.text(graphics, font, name, this.imageWidth / 2 - font.width(name) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0xFF404040, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        drawBattery(graphics, TEXTURE, leftPos, topPos, imageWidth, imageHeight, scaled(this.menu.getPower(), this.menu.getMaxPower()),
                this.menu.getRedLow(), this.menu.getRedHigh(), this.menu.getPriorityOrdinal());
    }

    static int scaled(long power, long max) {
        if (max <= 0L || power <= 0L) return 0;
        if (power >= max) return 52;
        return (int) ((double) power / (double) max * 52D);
    }

    /** The 1.7 battery background: bar, two mode buttons and the priority button. */
    static void drawBattery(GuiGraphicsExtractor graphics, Identifier texture, int left, int top, int w, int h,
                            int bar, int redLow, int redHigh, int priority) {
        GuiDraw.blit(graphics, texture, left, top, 0.0F, 0.0F, w, h, 256, 256);
        if (bar > 0) GuiDraw.blit(graphics, texture, left + 62, top + 69 - bar, 176.0F, (float) (52 - bar), 52, bar, 256, 256);
        GuiDraw.blit(graphics, texture, left + 133, top + 16, 176.0F, (float) (52 + redLow * 18), 18, 18, 256, 256);
        GuiDraw.blit(graphics, texture, left + 133, top + 52, 176.0F, (float) (52 + redHigh * 18), 18, 18, 256, 256);
        GuiDraw.blit(graphics, texture, left + 152, top + 35, 194.0F, (float) (52 + priority * 16 - 16), 16, 16, 256, 256);
    }

    /** 1.7 hover text: the bar shows power/max and the signed delta. */
    static List<Component> barInfo(long power, long max, long delta) {
        String deltaText = BobMathUtil.getShortNumber(Math.abs(delta)) + "HE/s";
        if (delta > 0) deltaText = ChatFormatting.GREEN + "+" + deltaText;
        else if (delta < 0) deltaText = ChatFormatting.RED + "-" + deltaText;
        else deltaText = ChatFormatting.YELLOW + "+" + deltaText;
        return List.of(Component.literal(BobMathUtil.getShortNumber(power) + "/" + BobMathUtil.getShortNumber(max) + "HE"),
                Component.literal(deltaText));
    }

    /** 1.7 priority hover: the level, "Recommended for:" and its description. */
    static List<Component> priorityInfo(int ordinal) {
        String lang = switch (ordinal) {
            case 1 -> "low";
            case 2 -> "normal";
            case 3 -> "high";
            default -> "low";
        };
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("battery.priority." + lang));
        lines.add(Component.translatable("battery.priority.recommended"));
        lines.add(Component.translatable("battery.priority." + lang + ".desc"));
        return lines;
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.isHovering(62, 17, 52, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, barInfo(this.menu.getPower(), this.menu.getMaxPower(), this.menu.getDelta()),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(152, 35, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, priorityInfo(this.menu.getPriorityOrdinal()), Optional.empty(), mouseX, mouseY);
        }
    }

    /** 1.7 hit boxes: (133..151, 17..34) low mode, (133..151, 53..70) high mode, (152..168, 36..51) priority. */
    static int button(double x, double y, int left, int top) {
        if (left + 133 <= x && left + 133 + 18 > x && top + 16 < y && top + 16 + 18 >= y) return 0;
        if (left + 133 <= x && left + 133 + 18 > x && top + 52 < y && top + 52 + 18 >= y) return 1;
        if (left + 152 <= x && left + 152 + 16 > x && top + 35 < y && top + 35 + 16 >= y) return 2;
        return -1;
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int button = button(event.x(), event.y(), leftPos, topPos);
        if (button >= 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
                return true;
            }
        }
        return false;
    }
}
