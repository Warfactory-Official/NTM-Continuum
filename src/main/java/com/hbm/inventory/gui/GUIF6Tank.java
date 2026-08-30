package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineF6Tank;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineF6Tank;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Shared tank screen, preserving the old 16x52 fluid readout. */
public class GUIF6Tank extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineF6Tank> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/uf6tank.png");
    private static final Identifier PUF6_TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/puf6tank.png");

    public GUIF6Tank(ContainerMachineF6Tank menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = imageWidth / 2 - font.width(title) / 2;
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TileEntityMachineF6Tank tank = this.menuTank();
        Identifier background = tank != null && tank.tank.getTankType() == Fluids.PUF6 ? PUF6_TEXTURE : TEXTURE;
        GuiDraw.blit(graphics, background, this.leftPos, this.topPos, 0F, 0F,
                this.imageWidth, this.imageHeight, 256, 256);

        if (tank == null || tank.tank.getFill() <= 0) return;

        FluidTankNTM fluid = tank.tank;
        int height = fluid.getMaxFill() <= 0 ? 0 : fluid.getFill() * 52 / fluid.getMaxFill();
        if (height <= 0) return;

        Identifier fluidTexture = fluid.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            int x = this.leftPos + 80;
            int bottom = this.topPos + 97;
            int top = bottom - height;
            graphics.enableScissor(x, top, x + 16, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x, tileY, 0F, 0F, 16, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            int color = 0xFF000000 | (fluid.getTankType().getColor() & 0xFFFFFF);
            graphics.fill(this.leftPos + 80, this.topPos + 97 - height,
                    this.leftPos + 96, this.topPos + 97, color);
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineF6Tank tank = this.menuTank();
        if (tank != null && this.isHovering(80, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    tank.tank.getTankType().getDisplayName(),
                    Component.literal(tank.tank.getFill() + " / " + tank.tank.getMaxFill() + " mB")
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    private TileEntityMachineF6Tank menuTank() {
        return this.menu.getTank();
    }
}
