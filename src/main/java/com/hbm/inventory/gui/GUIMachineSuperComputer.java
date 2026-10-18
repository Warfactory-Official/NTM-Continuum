package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineSuperComputer;
import com.hbm.module.machine.ModuleMachineBase;
import com.hbm.tileentity.machine.TileEntityMachineSuperComputer;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code GUIMachineSuperComputer}. */
public class GUIMachineSuperComputer extends GuiInfoContainerProcessor<ContainerMachineSuperComputer> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_supercomputer.png");

    public GUIMachineSuperComputer(ContainerMachineSuperComputer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 211);
    }

    @Override public int[][] getSelectorPositions() { return new int[][] {{7, 80, 1}}; }
    @Override public Identifier getTexture() { return TEXTURE; }

    @Override
    protected ModuleMachineBase @Nullable [] processorModules() {
        TileEntityMachineSuperComputer te = this.menu.getComputer();
        return te == null ? null : new ModuleMachineBase[] {te.computerModule};
    }

    @Override
    protected BlockPos machinePos() {
        TileEntityMachineSuperComputer te = this.menu.getComputer();
        return te == null ? BlockPos.ZERO : te.getBlockPos();
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        TileEntityMachineSuperComputer computer = this.menu.getComputer();
        if (computer == null) return;

        int p = computer.maxPower <= 0 ? 0 : (int) (computer.power * 61 / computer.maxPower);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 79 - p, 176.0F, (float) (61 - p), 16, p, 256, 256);

        if (computer.computerModule.progress > 0) {
            int j = (int) Math.ceil(70 * computer.computerModule.progress);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 81, 176.0F, 61.0F, j, 16, 256, 256);
        }

        renderStandardLEDs(graphics, computer.didProcess, computer.computerModule.getRecipe(), computer.power, 51, 76, 195, 0);
        renderRecipeIcons(graphics);

        GuiUtil.tankHorizontal(graphics, computer.inputTank, leftPos + 8, topPos + 70, 52, 16);
        GuiUtil.tankHorizontal(graphics, computer.outputTank, leftPos + 80, topPos + 70, 52, 16);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractOwnTooltip(graphics, mouseX, mouseY);
        TileEntityMachineSuperComputer computer = this.menu.getComputer();
        if (computer == null) return;

        if (GuiUtil.hover(mouseX, mouseY, leftPos + 8, topPos + 54, 52, 16)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(computer.inputTank), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 80, topPos + 54, 52, 16)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(computer.outputTank), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 152, topPos + 18, 16, 61)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(computer.power, computer.maxPower), mouseX, mouseY);
        }
    }
}
