package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerOilburner;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import dev.golem.core.gui.MouseInput;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

import java.util.Locale;

/**
 * Screen for the fluid burner, 1.7 {@code GUIOilburner} ({@code machine/gui_oilburner.png}, 176×203):
 * heat bar 16×52 at (116,17) (fill u=194), tank 16×52 at (44,17), burn rate slider x [70,106) y (23,31]
 * (knob u=210 v=14), burn button 35×14 at (70,54) (lit u=210), flame 18×18 at (79,34) (u=176) while burning.
 */
public class GUIOilburner extends CompatContainerScreen<ContainerOilburner> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/machine/gui_oilburner.png");

    /** The slider's own value, sent on change; the knob follows it rather than the synced rate. */
    private int burnRate;
    private boolean usingSlider = false;

    public GUIOilburner(ContainerOilburner menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
        TileEntityHeaterOilburner te = menu.getHeater();
        this.burnRate = te != null ? te.burnRate : 1;
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

        TileEntityHeaterOilburner te = this.menu.getHeater();
        if (te == null) return;

        // 1.7: the grabbed slider follows mouseX alone every frame until release.
        if (this.usingSlider) applySlider(te, mouseX);

        int i = Math.min(te.heatEnergy, te.maxHeatEnergy) * 52 / te.maxHeatEnergy;
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 116, this.topPos + 69 - i, 194.0F, (float) (52 - i), 16, i, 256, 256);

        int j = (this.burnRate - 1) * 32 / (te.maxBurnRate - 1);
        if (j > 32) { // a removed upgrade lowered maxBurnRate under the cached rate
            j = 32;
            this.burnRate = te.burnRate;
        }
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 71 + j, this.topPos + 23, 210.0F, 14.0F, 4, 8, 256, 256);

        if (te.isOn) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 70, this.topPos + 54, 210.0F, 0.0F, 35, 14, 256, 256);

            if (te.tank.getFill() > 0 && FluidCombustion.isFlammable(te.tank.getTankType())) {
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 34, 176.0F, 0.0F, 18, 18, 256, 256);
            }
        }

        GuiUtil.tank(graphics, te.tank, this.leftPos + 44, this.topPos + 69, 16, 52);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityHeaterOilburner te = this.menu.getHeater();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;

        if (this.usingSlider || GuiUtil.hover(mouseX, mouseY, gl + 70, gt + 23, 36, 8)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines(this.burnRate + " mB/t"),
                    Mth.clamp(mouseX, gl + 70, gl + 106), Mth.clamp(mouseY, gt + 23, gt + 31));
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 116, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines(String.format(Locale.US, "%,d", te.heatEnergy)
                    + " / " + String.format(Locale.US, "%,d", te.maxHeatEnergy) + " TU"), mouseX, mouseY);
            return;
        }
        if (FluidCombustion.isFlammable(te.tank.getTankType()) && GuiUtil.hover(mouseX, mouseY, gl + 79, gt + 34, 18, 18)) {
            int heat = (int) (FluidCombustion.getFlammableEnergy(te.tank.getTankType()) * this.burnRate / 1000);
            String c = ChatFormatting.YELLOW + "";
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines(
                    c + String.format(Locale.US, "%,d", heat) + " TU/t",
                    c + String.format(Locale.US, "%,d", heat * 20) + " TU/s"), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 70, gt + 54, 35, 15)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Ignition"), mouseX, mouseY);
            return;
        }
        if (GuiUtil.hover(mouseX, mouseY, gl + 44, gt + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return false;

        // burn toggle, 1.7 hit area x [80,96), y (54,68]
        if (GuiUtil.hover(event.x(), event.y(), this.leftPos + 80, this.topPos + 54, 16, 14)) {
            GuiUtil.playClickSound();
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }
        if (GuiUtil.hover(event.x(), event.y(), this.leftPos + 70, this.topPos + 23, 36, 8)) {
            GuiUtil.playClickSound();
            this.usingSlider = true;
            return true;
        }
        return false;
    }

    @Override
    protected boolean mouseReleased(MouseInput event) {
        if (this.usingSlider && (event.button() == 0 || event.button() == 1)) this.usingSlider = false;
        return false;
    }

    private void applySlider(TileEntityHeaterOilburner te, int mouseX) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return;
        int rate = Mth.clamp((mouseX - this.leftPos - 70) * te.maxBurnRate / 32, 1, te.maxBurnRate);
        if (rate != this.burnRate) {
            this.burnRate = rate;
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 1000 + rate);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getHeater();
    }
}
