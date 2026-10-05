package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineTurbine;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.machine.TileEntityMachineTurbine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the steam turbine — 1.7.10 {@code GUIMachineTurbine} ({@code gui_turbine.png}, 176×168). Input tank
 * 16×52 at x=62 and output tank at x=134 (bottom y=69), the 7×34 power bar at (123,35) filling from (176,34), and
 * the 14×14 steam-stage icon at (99,18) whose source row picks STEAM / HOTSTEAM / SUPERHOTSTEAM / ULTRAHOTSTEAM.
 * When the output tank is NONE the "large red !" panel appears left of the GUI with "Error: Invalid fluid!".
 */
public class GUIMachineTurbine extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineTurbine> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_turbine.png");

    public GUIMachineTurbine(ContainerMachineTurbine menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 168);
    }

    /** 1.7: centred title at y=6, inventory at (8, ySize-96+2), both 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineTurbine te = this.menu.getTurbine();
        if (te == null) return;

        var type = te.input.getTankType();
        int stage = type == Fluids.STEAM ? 0 : type == Fluids.HOTSTEAM ? 1 : type == Fluids.SUPERHOTSTEAM ? 2 : type == Fluids.ULTRAHOTSTEAM ? 3 : -1;
        if (stage >= 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 99, this.topPos + 18, 183.0F, (float) (stage * 14), 14, 14, 256, 256);
        }

        int i = (int) te.getPowerScaled(34);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 123, this.topPos + 69 - i, 176.0F, (float) (34 - i), 7, i, 256, 256);

        if (te.output.getTankType() == Fluids.NONE) {
            GuiUtil.drawInfoPanel(graphics, this.leftPos - 16, this.topPos + 36 + 32, 6);
        }

        GuiUtil.tank(graphics, te.input, this.leftPos + 62, this.topPos + 69, 16, 52);
        GuiUtil.tank(graphics, te.output, this.leftPos + 134, this.topPos + 69, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineTurbine te = this.menu.getTurbine();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;
        if (GuiUtil.hover(mouseX, mouseY, gl + 62, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.input), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 134, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.output), mouseX, mouseY);
        } else if (te.output.getTankType() == Fluids.NONE && GuiUtil.hover(mouseX, mouseY, gl - 16, gt + 36 + 32, 16, 16)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Error: Invalid fluid!"), gl - 8, gt + 36 + 16 + 32);
        } else if (GuiUtil.hover(mouseX, mouseY, gl + 123, gt + 69 - 34, 7, 34)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.getMaxPower()), mouseX, mouseY);
        }
    }
}
