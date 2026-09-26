package com.hbm.inventory.gui;

import dev.golem.core.gui.Gauges;
import com.hbm.inventory.container.ContainerMachineGiantTank;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineGiantTank;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Faithful shared 176x166 barrel screen for both giant storage machines. */
public class GUIMachineGiantTank extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineGiantTank> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_barrel.png");
    private static final int TANK_X = 71, TANK_TOP = 17, TANK_WIDTH = 34, TANK_HEIGHT = 52;
    private static final int MODE_X = 151, MODE_Y = 34, MODE_SIZE = 18;

    public GUIMachineGiantTank(ContainerMachineGiantTank menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineGiantTank tank = menu.getTank();
        if (tank == null) return;

        drawTank(graphics, tank.tankNew);
        int mode = Math.max(0, Math.min(3, tank.mode));
        GuiDraw.blit(graphics, TEXTURE, leftPos + MODE_X, topPos + MODE_Y,
                176F, mode * 18F, MODE_SIZE, MODE_SIZE, 256, 256);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank) {
        int height = tank.getMaxFill() <= 0 ? 0 : (int) ((long) tank.getFill() * TANK_HEIGHT / tank.getMaxFill());
        if (height <= 0) return;

        int x = leftPos + TANK_X;
        int bottom = topPos + TANK_TOP + TANK_HEIGHT;
        int top = bottom - height;
        Identifier fluidTexture = tank.getTankType().getTexture();
        int tint = 0xFF000000 | (tank.getTankType().getTint() & 0xFFFFFF);
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x, top, x + TANK_WIDTH, bottom);
            for (int y = bottom - 16; y > top - 16; y -= 16) {
                for (int tileX = 0; tileX < TANK_WIDTH; tileX += 16) {
                    GuiDraw.blit(graphics, fluidTexture, x + tileX, y, 0F, 0F,
                            Math.min(16, TANK_WIDTH - tileX), 16, 16, 16, tint);
                }
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x, top, x + TANK_WIDTH, bottom, tint);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineGiantTank tank = menu.getTank();
        if (tank == null) return;
        if (isHovering(TANK_X, TANK_TOP, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(tank.tankNew), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    List.of(Component.literal("Mode: " + modeName(tank.mode))), Optional.empty(), mouseX, mouseY);
        }
    }

    private static String modeName(short mode) {
        return switch (mode) {
            case 1 -> "Buffer (in+out)";
            case 2 -> "Output only";
            case 3 -> "Disabled";
            default -> "Input only";
        };
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        if (isHovering(MODE_X, MODE_Y, MODE_SIZE, MODE_SIZE, event.x(), event.y())) {
            Gauges.click();
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
        }
        return false;
    }
}
