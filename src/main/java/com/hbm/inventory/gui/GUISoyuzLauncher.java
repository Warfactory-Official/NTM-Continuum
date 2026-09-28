package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerSoyuzLauncher;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code GUISoyuzLauncher}: {@code gui_soyuz.png} at 194x244. Power and the two propellant tanks on the right
 * with their three ready lamps; the rocket, designator, orbital-module and satellite status lamps; the CARGO /
 * SATELLITE mode switch at (79,52)/(97,52); the launch button at (88,97) lit while counting; and the red
 * {@code SS:CC} clock at (85,121). The title is white and centred at y=4.
 */
public class GUISoyuzLauncher extends CompatContainerScreen<ContainerSoyuzLauncher> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_soyuz.png");

    public GUISoyuzLauncher(ContainerSoyuzLauncher menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 194, 244);
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        GuiDraw.blit(graphics, TEXTURE, leftPos + x, topPos + y, (float) u, (float) v, w, h, 256, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntitySoyuzLauncher launcher = menu.getLauncher();
        if (launcher == null) return;

        int i = (int) launcher.getPowerScaled(52);
        if (i > 0) blit(graphics, 134, 96 - i, 194, 52 - i, 16, i);

        blit(graphics, 97, 79, 210 + (launcher.hasRocket() ? 18 : 0), 8, 18, 18);
        int j = launcher.designator();
        if (j > 0) blit(graphics, 79, 79, 210 + (j - 1) * 18, 8, 18, 18);

        int k = launcher.mode;
        blit(graphics, 97 - k * 18, 52, 228 - k * 18, 26, 18, 18);

        int l = launcher.orbital();
        if (l > 0) blit(graphics, 79, 25, 210 + (l - 1) * 18, 8, 18, 18);

        int m = launcher.satellite();
        if (m > 0) blit(graphics, 97, 25, 210 + (m - 1) * 18, 8, 18, 18);

        if (launcher.starting) blit(graphics, 88, 97, 210, 44, 18, 18);

        blit(graphics, 157, 31, launcher.hasFuel() ? 210 : 216, 0, 6, 8);
        blit(graphics, 175, 31, launcher.hasOxy() ? 210 : 216, 0, 6, 8);
        blit(graphics, 139, 31, launcher.hasPower() ? 210 : 216, 0, 6, 8);

        drawTank(graphics, launcher.tanks[0], 152, 44, 16, 52);
        drawTank(graphics, launcher.tanks[1], 170, 44, 16, 52);

        GuiUtil.drawInfoPanel(graphics, leftPos - 16, topPos + 53, 2);
    }

    /** Repeats the 16x16 fluid texture, matching the old GL_REPEAT tank quad. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int max = tank.getMaxFill();
        int filled = max <= 0 ? 0 : (int) ((long) tank.getFill() * height / max);
        if (filled <= 0) return;

        int x0 = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - filled;
        int tint = 0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF);
        Identifier fluidTexture = tank.getTankType().getTexture();

        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + width, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x0, tileY, 0F, 0F, width, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x0, top, x0 + width, bottom,
                    0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 4, 0xFFFFFFFF, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 17, imageHeight - 96 + 2, 0xFF404040, false);

        TileEntitySoyuzLauncher launcher = menu.getLauncher();
        if (launcher == null) return;

        // 1.7: seconds padded in front, hundredths padded BEHIND ("5" becomes "50")
        String secs = "" + launcher.countdown / 20;
        String cents = "" + (launcher.countdown % 20) * 5;
        if (secs.length() == 1) secs = "0" + secs;
        if (cents.length() == 1) cents += "0";
        GuiDraw.text(graphics, font, Component.literal(secs + ":" + cents), 85, 121, 0xFFFF0000, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntitySoyuzLauncher launcher = menu.getLauncher();
        if (launcher == null) return;

        if (isHovering(152, 44, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(launcher.tanks[0]), mouseX, mouseY);
        } else if (isHovering(170, 44, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(launcher.tanks[1]), mouseX, mouseY);
        } else if (isHovering(134, 44, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(launcher.power, TileEntitySoyuzLauncher.maxPower), mouseX, mouseY);
        } else if (isHovering(-16, 53, 16, 16, mouseX, mouseY)) {
            tooltip(graphics, lines("desc.gui.soyuz.desc"), leftPos - 8, topPos + 53 + 16);
        } else if (isHovering(79, 52, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, lines("desc.gui.soyuz.cargo"), mouseX, mouseY);
        } else if (isHovering(97, 52, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, lines("desc.gui.soyuz.satellite"), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        double x = event.x();
        double y = event.y();
        int button = -1;

        if (leftPos + 97 <= x && leftPos + 97 + 18 > x && topPos + 52 < y && topPos + 52 + 18 >= y)
            button = ContainerSoyuzLauncher.BUTTON_MODE_SATELLITE;
        if (leftPos + 79 <= x && leftPos + 79 + 18 > x && topPos + 52 < y && topPos + 52 + 18 >= y)
            button = ContainerSoyuzLauncher.BUTTON_MODE_CARGO;
        if (leftPos + 88 <= x && leftPos + 88 + 18 > x && topPos + 97 < y && topPos + 97 + 18 >= y)
            button = ContainerSoyuzLauncher.BUTTON_LAUNCH;

        if (button < 0) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;
        GuiUtil.playClickSound();
        mc.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
        return true;
    }

    /** 1.7 {@code I18nUtil.resolveKeyArray}: one translation split on {@code $}. */
    private static List<Component> lines(String key) {
        List<Component> lines = new ArrayList<>();
        for (String line : I18n.get(key).split("\\$")) lines.add(Component.literal(line));
        return lines;
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, text, Optional.empty(), mouseX, mouseY);
    }
}
