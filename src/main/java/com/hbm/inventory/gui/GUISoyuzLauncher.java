package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerSoyuzLauncher;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.soyuz.TileEntitySoyuzLauncher;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import dev.golem.core.gui.MouseInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The launch complex's panel. The column on the right is power and the two propellants; the readout across
 * the middle is the hold-down clock, which only runs while everything the rocket needs is aboard. The
 * button under it starts and the same button stops it.
 */
public class GUISoyuzLauncher extends CompatContainerScreen<ContainerSoyuzLauncher> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_soyuz.png");

    public GUISoyuzLauncher(ContainerSoyuzLauncher menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 194, 244);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntitySoyuzLauncher launcher = menu.getLauncher();
        if (launcher == null) return;

        int power = (int) launcher.getPowerScaled(52);
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 134, topPos + 96 - power,
                    194F, (float) (52 - power), 16, power, 256, 256);
        }

        GuiDraw.blit(graphics, TEXTURE, leftPos + 97, topPos + 79,
                210F + (launcher.hasRocket() ? 18F : 0F), 8F, 18, 18, 256, 256);

        drawTank(graphics, launcher.tanks[0], 152, 44, 16, 52);
        drawTank(graphics, launcher.tanks[1], 170, 44, 16, 52);
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

        int ticks = launcher.starting ? launcher.countdown : TileEntitySoyuzLauncher.maxCount;
        Component clock = Component.literal(String.format(Locale.US, "%d:%02d", ticks / 20, (ticks % 20) * 5));
        GuiDraw.text(graphics, font, clock, 85, 121, 0xFFFF0000, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntitySoyuzLauncher launcher = menu.getLauncher();
        if (launcher == null) return;

        if (isHovering(134, 44, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(launcher.power, TileEntitySoyuzLauncher.maxPower), mouseX, mouseY);
        } else if (isHovering(152, 44, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(launcher.tanks[0]), mouseX, mouseY);
        } else if (isHovering(170, 44, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(launcher.tanks[1]), mouseX, mouseY);
        } else if (isHovering(88, 97, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(launcher.canLaunch()
                    ? Component.translatable("gui.soyuz.launch")
                    : Component.translatable("gui.soyuz.notready")), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        int x = (int) event.x() - leftPos;
        int y = (int) event.y() - topPos;
        if (x < 88 || x >= 106 || y < 97 || y >= 115) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;
        mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
        GuiUtil.playClickSound();
        return true;
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, text, Optional.empty(), mouseX, mouseY);
    }
}
