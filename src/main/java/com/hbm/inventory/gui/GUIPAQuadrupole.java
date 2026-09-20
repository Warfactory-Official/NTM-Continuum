package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerPAQuadrupole;
import com.hbm.items.machine.ItemPACoil;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.albion.TileEntityPAQuadrupole;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** Quadrupole panel: three lamps for power, coolant and coil, the three things a focus pass needs. */
public class GUIPAQuadrupole extends GUIPABase<ContainerPAQuadrupole> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/particleaccelerator/gui_quadrupole.png");

    public GUIPAQuadrupole(ContainerPAQuadrupole menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected @Nullable TileEntityCooledBase machine() {
        return menu.getMachine();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityPAQuadrupole quadrupole = menu.getMachine();
        if (quadrupole == null) return;

        drawPowerBar(graphics, x + 26, y + 18, quadrupole.power, quadrupole.getMaxPower());

        if (quadrupole.power >= TileEntityPAQuadrupole.usage) drawLamp(graphics, x + 65, y + 64);
        if (Math.ceil(quadrupole.temperature) <= COOL_ENOUGH) drawLamp(graphics, x + 75, y + 64);
        if (ItemPACoil.typeOf(quadrupole.inventory.getStackInSlot(1)) != null) drawLamp(graphics, x + 85, y + 64);

        drawTank(graphics, quadrupole.coolantTanks[0], x + 116, y + 36);
        drawTank(graphics, quadrupole.coolantTanks[1], x + 134, y + 36);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics, 6);
        drawTemperature(graphics, 118, 22, 148, 12);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        commonTooltip(graphics, mouseX, mouseY, 26, 18, 116, 36);
    }
}
