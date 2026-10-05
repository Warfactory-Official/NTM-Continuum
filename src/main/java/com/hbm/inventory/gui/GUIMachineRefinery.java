package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineRefinery;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.tileentity.machine.oil.TileEntityMachineRefinery;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the refinery, a straight port of 1.7 {@code GUIMachineRefinery} ({@code processing/gui_refinery.png},
 * 256x256 source, 182x240 GUI): power bar 16x88 at (158,18), the input column tank 16x70 at (12,17), the four
 * product pipes along the top (tinted with each fraction's colour, untinted with no recipe) and the four output
 * tanks 16x52 at x=64/82/100/118.
 */
public class GUIMachineRefinery extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineRefinery> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_refinery.png");

    public GUIMachineRefinery(ContainerMachineRefinery menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 182, 240);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int guiLeft = this.leftPos, guiTop = this.topPos;
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineRefinery refinery = this.menu.getRefinery();
        if (refinery == null) return;

        // power
        int j = (int) refinery.getPowerScaled(88);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, guiLeft + 158, guiTop + 106 - j, 182.0F, (float) (88 - j), 16, j, 256, 256);

        // input tank
        FluidTankNTM inputOil = refinery.tanks[0];
        if (inputOil.getFill() != 0) {
            GuiUtil.tank(graphics, inputOil, guiLeft + 12, guiTop + 88, 16, 70);
        }

        // pipes: heavy, naphtha, light, gaseous (1.7 func_146110_a on the 256x256 sheet)
        RefineryRecipes.RefineryRecipe recipe = RefineryRecipes.getRefinery(inputOil.getTankType());
        int cHeavy = recipe != null ? tint(recipe.frac1().type.getColor()) : 0xFFFFFFFF;
        int cNaph = recipe != null ? tint(recipe.frac2().type.getColor()) : 0xFFFFFFFF;
        int cLight = recipe != null ? tint(recipe.frac3().type.getColor()) : 0xFFFFFFFF;
        int cGas = recipe != null ? tint(recipe.frac4().type.getColor()) : 0xFFFFFFFF;
        GuiDraw.blit(graphics, TEXTURE, guiLeft + 30, guiTop + 30, 0.0F, 248.0F, 43, 4, 256, 256, cHeavy);
        GuiDraw.blit(graphics, TEXTURE, guiLeft + 30, guiTop + 26, 0.0F, 240.0F, 61, 8, 256, 256, cNaph);
        GuiDraw.blit(graphics, TEXTURE, guiLeft + 30, guiTop + 22, 61.0F, 240.0F, 79, 12, 256, 256, cLight);
        GuiDraw.blit(graphics, TEXTURE, guiLeft + 30, guiTop + 18, 140.0F, 240.0F, 97, 16, 256, 256, cGas);

        // output tanks
        GuiUtil.tank(graphics, refinery.tanks[1], guiLeft + 64, guiTop + 88, 16, 52);
        GuiUtil.tank(graphics, refinery.tanks[2], guiLeft + 82, guiTop + 88, 16, 52);
        GuiUtil.tank(graphics, refinery.tanks[3], guiLeft + 100, guiTop + 88, 16, 52);
        GuiUtil.tank(graphics, refinery.tanks[4], guiLeft + 118, guiTop + 88, 16, 52);
    }

    private static int tint(int rgb) {
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7: title centred on xSize/2 - 36/2; "Inventory" at x=11 (the player inventory is shifted right by 3).
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 - 36 / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 11, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineRefinery te = this.menu.getRefinery();
        if (te == null) return;
        int guiLeft = this.leftPos, guiTop = this.topPos;

        if (GuiUtil.hover(mouseX, mouseY, guiLeft + 12, guiTop + 17, 16, 70)) { GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[0]), mouseX, mouseY); return; }
        int[] xs = {64, 82, 100, 118};
        for (int i = 0; i < 4; i++) {
            if (GuiUtil.hover(mouseX, mouseY, guiLeft + xs[i], guiTop + 35, 16, 52)) {
                GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tanks[i + 1]), mouseX, mouseY);
                return;
            }
        }
        if (GuiUtil.hover(mouseX, mouseY, guiLeft + 158, guiTop + 18, 16, 88)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineRefinery.maxPower), mouseX, mouseY);
        }
    }
}
