package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineRockMill;
import com.hbm.module.machine.ModuleMachineBase;
import com.hbm.tileentity.machine.TileEntityMachineRockMill;
import dev.golem.core.Ids;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jspecify.annotations.Nullable;

/** 1.7 {@code GUIMachineRockMill}, which draws the same selector, ghosts and LEDs as the processor screens. */
public class GUIMachineRockMill extends GuiInfoContainerProcessor<ContainerMachineRockMill> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_rockmill.png");

    public GUIMachineRockMill(ContainerMachineRockMill menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 220);
    }

    @Override public int[][] getSelectorPositions() { return new int[][] {{7, 89, 1}}; }
    @Override public Identifier getTexture() { return TEXTURE; }

    @Override
    protected ModuleMachineBase @Nullable [] processorModules() {
        TileEntityMachineRockMill te = this.menu.getMill();
        return te == null ? null : new ModuleMachineBase[] {te.rockMillModule};
    }

    @Override
    protected BlockPos machinePos() {
        TileEntityMachineRockMill te = this.menu.getMill();
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

        TileEntityMachineRockMill mill = this.menu.getMill();
        if (mill == null) return;

        int p = mill.maxPower <= 0 ? 0 : (int) (mill.power * 71 / mill.maxPower);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 152, topPos + 89 - p, 176.0F, (float) (71 - p), 16, p, 256, 256);

        if (mill.rockMillModule.progress > 0) {
            int j = (int) Math.ceil(70 * mill.rockMillModule.progress);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 62, topPos + 90, 176.0F, 71.0F, j, 16, 256, 256);
        }

        renderStandardLEDs(graphics, mill.didProcess, mill.rockMillModule.getRecipe(), mill.power, 51, 85, 195, 0);
        renderRecipeIcons(graphics);

        GuiUtil.tankHorizontal(graphics, mill.inputTanks[0], leftPos + 8, topPos + 79, 52, 16);
        GuiUtil.tankHorizontal(graphics, mill.outputTanks[0], leftPos + 80, topPos + 79, 52, 16);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractOwnTooltip(graphics, mouseX, mouseY);
        TileEntityMachineRockMill mill = this.menu.getMill();
        if (mill == null) return;

        if (GuiUtil.hover(mouseX, mouseY, leftPos + 8, topPos + 63, 52, 16)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(mill.inputTanks[0]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 80, topPos + 63, 52, 16)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(mill.outputTanks[0]), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 152, topPos + 18, 16, 71)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(mill.power, mill.maxPower), mouseX, mouseY);
        }
    }
}
