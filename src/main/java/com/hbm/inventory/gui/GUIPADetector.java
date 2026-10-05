package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerPADetector;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import com.hbm.tileentity.machine.albion.TileEntityPADetector;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** Detector panel: the collision chamber's power and coolant readouts, plus its product slots. */
public class GUIPADetector extends GUIPABase<ContainerPADetector> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/particleaccelerator/gui_detector.png");

    public GUIPADetector(ContainerPADetector menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected @Nullable TileEntityCooledBase machine() {
        return menu.getDetector();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityPADetector detector = menu.getDetector();
        if (detector == null) return;

        drawPowerBar(graphics, x + 8, y + 18, detector.power, detector.getMaxPower());

        if (Math.ceil(detector.temperature) <= COOL_ENOUGH) drawLamp(graphics, x + 43, y + 18);
        if (detector.power >= TileEntityPADetector.usage) drawLamp(graphics, x + 43, y + 43);

        drawTank(graphics, detector.coolantTanks[0], x + 134, y + 36);
        drawTank(graphics, detector.coolantTanks[1], x + 152, y + 36);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        drawTitleAndInventory(graphics, -8, 5, 0xffffff);
        drawTemperature(graphics, 136, 22, 166, 12);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        commonTooltip(graphics, mouseX, mouseY, 8, 18, 134, 36);
    }
}
