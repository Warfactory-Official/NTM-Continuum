package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerPARFC;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.albion.TileEntityPARFC;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** RF cavity panel: power, coolant and temperature, with no controls of its own. */
public class GUIPARFC extends GUIPABase<ContainerPARFC> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/particleaccelerator/gui_rfc.png");

    public GUIPARFC(ContainerPARFC menu, Inventory playerInventory, Component title) {
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

        TileEntityPARFC rfc = menu.getMachine();
        if (rfc == null) return;

        drawPowerBar(graphics, x + 53, y + 18, rfc.power, rfc.getMaxPower());
        drawTank(graphics, rfc.coolantTanks[0], x + 89, y + 36);
        drawTank(graphics, rfc.coolantTanks[1], x + 107, y + 36);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
        drawTemperature(graphics, 91, 22, 121, 12);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        commonTooltip(graphics, mouseX, mouseY, 53, 18, 89, 36);
    }
}
