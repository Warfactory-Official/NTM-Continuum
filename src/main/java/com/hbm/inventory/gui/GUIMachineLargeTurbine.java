package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineLargeTurbine;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Screen for the legacy four-long steam turbine. */
public class GUIMachineLargeTurbine extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineLargeTurbine> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of(
            "hbm", "textures/gui/generators/gui_turbine_large.png");

    public GUIMachineLargeTurbine(ContainerMachineLargeTurbine menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 168);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineLargeTurbine turbine = menu.getTurbine();
        if (turbine == null) return;

        int stage = stageRow(turbine);
        if (stage >= 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 99, topPos + 18,
                    183F, stage * 14F, 14, 14, 256, 256);
        }

        int power = (int) turbine.getPowerScaled(34);
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 123, topPos + 69 - power,
                    176F, 34F - power, 7, power, 256, 256);
        }
        if (turbine.tanks[1].getTankType() == Fluids.NONE) {
            GuiUtil.drawWarningPanel(graphics, leftPos - 16, topPos + 68);
        }
        drawTank(graphics, turbine.tanks[0], 62, 17);
        drawTank(graphics, turbine.tanks[1], 134, 17);
    }

    private static int stageRow(TileEntityMachineLargeTurbine turbine) {
        var type = turbine.tanks[0].getTankType();
        if (type == Fluids.STEAM) return 0;
        if (type == Fluids.HOTSTEAM) return 1;
        if (type == Fluids.SUPERHOTSTEAM) return 2;
        if (type == Fluids.ULTRAHOTSTEAM) return 3;
        return -1;
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop) {
        int max = tank.getMaxFill();
        int height = max <= 0 ? 0 : (int) ((long) tank.getFill() * 52 / max);
        if (height <= 0) return;
        int x = leftPos + barX;
        int bottom = topPos + barTop + 52;
        int top = bottom - height;
        Identifier fluidTexture = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x, top, x + 16, bottom);
            for (int y = bottom - 16; y > top - 16; y -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x, y, 0F, 0F, 16, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x, top, x + 16, bottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineLargeTurbine turbine = menu.getTurbine();
        if (turbine == null) return;
        if (isHovering(123, 35, 7, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(turbine.power, turbine.getMaxPower()),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(62, 17, 16, 52, mouseX, mouseY)) {
            tankTooltip(graphics, turbine.tanks[0], mouseX, mouseY);
        } else if (isHovering(134, 17, 16, 52, mouseX, mouseY)) {
            tankTooltip(graphics, turbine.tanks[1], mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, List.of(tank.getTankType().getDisplayName(),
                        Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")),
                Optional.empty(), mouseX, mouseY);
    }
}
