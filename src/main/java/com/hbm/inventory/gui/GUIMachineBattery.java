package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineBattery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the machine battery. Uses the 26.1 rendering API: screens override
 * {@code extractBackground(GuiGraphicsExtractor, ...)} and draw via {@code blit} with a
 * {@code RenderPipelines} pipeline. Registered per-loader (NeoForge RegisterMenuScreensEvent / Fabric
 * MenuScreens.register on the client entrypoint).
 *
 * <p>Three buttons (ported from 1.12.2): the unpowered I/O-mode toggle (left), the redstone-powered
 * I/O-mode toggle (right), and the priority toggle (top-right). Their state is read off the synced
 * client BlockEntity; clicks dispatch through the vanilla {@code clickMenuButton} path.
 */
public class GUIMachineBattery extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineBattery> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_battery.png");
    private static final String[] MODE_NAMES = {"Input only", "Buffer (both)", "Output only", "Disabled"};
    private static final String[] PRIORITY_NAMES = {"Lowest", "Low", "Normal", "High", "Highest"};

    // Button regions (GUI-relative). The redstone-mode buttons are 18×18, the priority 16×16.
    private static final int REDLOW_X = 6, REDLOW_Y = 33, REDHIGH_X = 150, RED_Y = 33, RED_W = 18;
    private static final int PRIO_X = 151, PRIO_Y = 16, PRIO_W = 16;

    public GUIMachineBattery(ContainerMachineBattery menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Energy bar — a 34px-wide textured bar at u=176, anchored at the bottom (topPos+69) and growing
        // upward as power fills, sampled from v=52-i.
        long max = this.menu.getMaxPower();
        int i = max <= 0L ? 0 : (int) (this.menu.getPower() * 52L / max);
        if (i > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 71, this.topPos + 69 - i, 176.0F, (float) (52 - i), 34, i, 256, 256);
        }

        // Button-state overlays (strips below the energy bar in the texture: modes at u=176, v=52+mode*18;
        // priority at u=194, v=52+(ordinal-1)*16).
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 7, this.topPos + 34, 176.0F, (float) (52 + this.menu.getRedLow() * 18), 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 151, this.topPos + 34, 176.0F, (float) (52 + this.menu.getRedHigh() * 18), 18, 18, 256, 256);
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 17, 194.0F, (float) (52 + (this.menu.getPriorityOrdinal() - 1) * 16), 16, 16, 256, 256);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.isHovering(71, 17, 34, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(this.menu.getPower(), this.menu.getMaxPower(), this.menu.getDelta()),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(REDLOW_X, REDLOW_Y, RED_W, RED_W, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal("No redstone: " + mode(this.menu.getRedLow()))), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(REDHIGH_X, RED_Y, RED_W, RED_W, mouseX, mouseY)) {
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
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        int button = -1;
        if (this.isHovering(REDLOW_X, REDLOW_Y, RED_W, RED_W, event.x(), event.y())) button = 0;
        else if (this.isHovering(REDHIGH_X, RED_Y, RED_W, RED_W, event.x(), event.y())) button = 1;
        else if (this.isHovering(PRIO_X, PRIO_Y, PRIO_W, PRIO_W, event.x(), event.y())) button = 2;

        if (button >= 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
                return true;
            }
        }
        return false;
    }
}
