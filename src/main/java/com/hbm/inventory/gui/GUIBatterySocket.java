package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerBatterySocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
//? if >=26.1 {
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the battery socket. Mirrors {@link GUIMachineBattery} (energy bar + two redstone I/O
 * mode buttons + a priority button), but the energy reflects the inserted battery item's charge rather
 * than an internal buffer. Reads the live (client) BlockEntity through the container; clicks dispatch
 * through the vanilla {@code clickMenuButton} path. 26.1 render API (extractBackground + blit).
 */
public class GUIBatterySocket extends dev.golem.core.gui.CompatContainerScreen<ContainerBatterySocket> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/storage/gui_battery_socket.png");
    private static final String[] MODE_NAMES = {"Input only", "Buffer (both)", "Output only", "Disabled"};
    private static final String[] PRIORITY_NAMES = {"Lowest", "Low", "Normal", "High", "Highest"};

    // Energy bar (GUI-relative): 34×52, bottom anchored at (62, 69).
    private static final int BAR_X = 62, BAR_W = 34, BAR_H = 52, BAR_BOTTOM = 69, BAR_TOP = BAR_BOTTOM - BAR_H;
    // Buttons.
    private static final int REDLOW_X = 106, REDLOW_Y = 16, REDHIGH_X = 106, REDHIGH_Y = 52, RED_W = 18;
    private static final int PRIO_X = 125, PRIO_Y = 35, PRIO_W = 16;

    public GUIBatterySocket(ContainerBatterySocket menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 181);
    }

    @Override
    //? if >=26.1 {
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
    *///?}
        //? if >=26.1 {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        //?}
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        if (!this.menu.getBattery().isEmpty()) {
            long power = this.menu.getPower();
            long max = this.menu.getMaxPower();
            // Guard the long->int multiply for very large capacities (the limits of the LONG data type).
            if (power > Long.MAX_VALUE / 100) { power /= 100; max /= 100; }
            if (max <= 1) max = 1;
            int p = (int) (power * BAR_H / max);
            if (p > 0) {
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + BAR_X, this.topPos + BAR_BOTTOM - p,
                        176.0F, (float) (BAR_H - p), BAR_W, p, 256, 256);
            }
        }

        GuiDraw.blit(graphics, TEXTURE, this.leftPos + REDLOW_X, this.topPos + REDLOW_Y,
                176.0F, (float) (52 + this.menu.getRedLow() * 18), RED_W, RED_W, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + REDHIGH_X, this.topPos + REDHIGH_Y,
                176.0F, (float) (52 + this.menu.getRedHigh() * 18), RED_W, RED_W, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + PRIO_X, this.topPos + PRIO_Y,
                194.0F, (float) (52 + (this.menu.getPriorityOrdinal() - 1) * 16), PRIO_W, PRIO_W, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // Centre the title (the 1.12.2 socket drew it at xSize/2 - width/2); keep the inventory label default.
        GuiDraw.text(graphics, this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, this.titleLabelY, -12566464, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    @Override
    //? if >=26.1 {
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    //?} else {
    /*protected void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    *///?}
        //? if >=26.1 {
        super.extractTooltip(graphics, mouseX, mouseY);
        //?} else {
        /*super.renderTooltip(graphics, mouseX, mouseY);
        *///?}
        if (!this.menu.getBattery().isEmpty() && this.isHovering(BAR_X, BAR_TOP, BAR_W, BAR_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(this.menu.getPower(), this.menu.getMaxPower(), this.menu.getDelta()),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(REDLOW_X, REDLOW_Y, RED_W, RED_W, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal("No redstone: " + mode(this.menu.getRedLow()))), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(REDHIGH_X, REDHIGH_Y, RED_W, RED_W, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal("Redstone on: " + mode(this.menu.getRedHigh()))), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(PRIO_X, PRIO_Y, PRIO_W, PRIO_W, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal("Priority: " + priority(this.menu.getPriorityOrdinal()))), Optional.empty(), mouseX, mouseY);
        }
    }

    private static String mode(int m) { return m >= 0 && m < MODE_NAMES.length ? MODE_NAMES[m] : "?"; }
    private static String priority(int o) { return o >= 0 && o < PRIORITY_NAMES.length ? PRIORITY_NAMES[o] : "?"; }

    @Override
    //? if >=26.1 {
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    //?} else {
    /*public boolean mouseClicked(double hbm$mx, double hbm$my, int hbm$btn) {
        dev.golem.core.gui.MouseInput event = new dev.golem.core.gui.MouseInput(hbm$mx, hbm$my, hbm$btn);
    *///?}
        int button = -1;
        if (this.isHovering(REDLOW_X, REDLOW_Y, RED_W, RED_W, event.x(), event.y())) button = 0;
        else if (this.isHovering(REDHIGH_X, REDHIGH_Y, RED_W, RED_W, event.x(), event.y())) button = 1;
        else if (this.isHovering(PRIO_X, PRIO_Y, PRIO_W, PRIO_W, event.x(), event.y())) button = 2;

        if (button >= 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
                return true;
            }
        }
        //? if >=26.1 {
        return super.mouseClicked(event, doubleClick);
        //?} else {
        /*return super.mouseClicked(hbm$mx, hbm$my, hbm$btn);
        *///?}
    }
}
