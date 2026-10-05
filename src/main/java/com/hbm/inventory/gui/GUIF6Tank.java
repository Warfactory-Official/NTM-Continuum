package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineF6Tank;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineF6Tank;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Port-only UF6/PUF6 tank screen (1.12.2 {@code GUIMachineUF6Tank}/{@code GUIMachinePuF6Tank}; 1.7 has no such GUI).
 * Labels follow the standard 1.7 container style.
 */
public class GUIF6Tank extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineF6Tank> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/uf6tank.png");
    private static final Identifier PUF6_TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/puf6tank.png");

    public GUIF6Tank(ContainerMachineF6Tank menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    /** The standard 1.7 container labels (also 1.12.2 {@code GUIMachineUF6Tank}): centred title at y 6, "Inventory" at (8, ySize - 94). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        TileEntityMachineF6Tank tank = this.menuTank();
        Identifier background = tank != null && tank.tank.getTankType() == Fluids.PUF6 ? PUF6_TEXTURE : TEXTURE;
        GuiDraw.blit(graphics, background, this.leftPos, this.topPos, 0F, 0F,
                this.imageWidth, this.imageHeight, 256, 256);

        if (tank == null) return;
        // The 1.12.2 GUIMachineUF6Tank called renderTank(guiLeft + 80, guiTop + 97, z, 16, 52), which puts the fluid
        // column over the player inventory, 28 px below the texture's tank window (rows 17..69) and below its own
        // renderTankInfo box (80, 17, 16, 52). The port draws it in the window, bottom at y 69, matching the tooltip.
        Production2Gui.drawTank(graphics, tank.tank, this.leftPos + 80, this.topPos + 69, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineF6Tank tank = this.menuTank();
        if (tank == null) return;
        // renderTankInfo hit test: x <= mouse < x + w, y < mouse <= y + h.
        int mx = mouseX - this.leftPos, my = mouseY - this.topPos;
        if (80 <= mx && 96 > mx && 17 < my && 69 >= my) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.tankTooltip(tank.tank), Optional.empty(), mouseX, mouseY);
        }
    }

    private TileEntityMachineF6Tank menuTank() {
        return this.menu.getTank();
    }
}
