package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineTurbine;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineTurbine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the steam turbine — 1.12.2 {@code GUIMachineTurbine} (texture {@code gui_turbine.png},
 * 176×<b>168</b>). Input tank 16×52 at (62,17) and output tank at (134,17) (both bottom-anchored at
 * y+69), the 7×34 power bar at (123,35) filling from (176,34), and the 14×14 steam-stage icon at (99,18)
 * whose source row picks STEAM / HOTSTEAM / SUPERHOTSTEAM / ULTRAHOTSTEAM. When the input fluid can't be
 * cooled the output tank goes NONE and the "large red !" panel appears left of the GUI.
 */
public class GUIMachineTurbine extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineTurbine> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_turbine.png");

    public GUIMachineTurbine(ContainerMachineTurbine menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 168);
        this.inventoryLabelY = this.imageHeight - 94; // the old ySize - 96 + 2
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2; // old GUI centred the name
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineTurbine te = this.menu.getTurbine();
        if (te == null) return;

        int stage = stageRow(te);
        if (stage >= 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 99, this.topPos + 18,
                    183.0F, (float) (stage * 14), 14, 14, 256, 256);
        }

        int i = (int) te.getPowerScaled(34);
        if (i > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 123, this.topPos + 69 - i,
                    176.0F, (float) (34 - i), 7, i, 256, 256);
        }

        if (te.output.getTankType() == Fluids.NONE) {
            GuiUtil.drawWarningPanel(graphics, this.leftPos - 16, this.topPos + 68);
        }

        drawTank(graphics, te.input, 62, 17);
        drawTank(graphics, te.output, 134, 17);
    }

    /** Row of the steam-stage icon strip for the current input fluid, or -1 when it isn't steam. */
    private static int stageRow(TileEntityMachineTurbine te) {
        var type = te.input.getTankType();
        if (type == Fluids.STEAM) return 0;
        if (type == Fluids.HOTSTEAM) return 1;
        if (type == Fluids.SUPERHOTSTEAM) return 2;
        if (type == Fluids.ULTRAHOTSTEAM) return 3;
        return -1;
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * 52 / maxF);
        if (h <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + 52;
        int yTop = yBottom - h;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + 16, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, 16, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + 16, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineTurbine te = this.menu.getTurbine();
        if (te == null) return;

        if (this.isHovering(123, 35, 7, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.getMaxPower()), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(62, 17, 16, 52, mouseX, mouseY)) {
            tankTooltip(graphics, te.input, mouseX, mouseY);
            return;
        }
        if (this.isHovering(134, 17, 16, 52, mouseX, mouseY)) {
            tankTooltip(graphics, te.output, mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, List.of(
                tank.getTankType().getDisplayName(),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
    }
}
