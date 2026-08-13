package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerCrystallizer;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Client screen for the ore acidizer. Reads {@code power}/{@code progress} off the synced client
 * {@link TileEntityMachineCrystallizer} (BufPacket, not ContainerData): the vertical energy bar
 * (right), the horizontal progress arrow, and the acid gauge (solid-colour fill, like the other
 * machine screens), each with a hover tooltip. Texture layout matches the 1.12.2
 * {@code gui_crystallizer_alt.png}.
 */
public class GUICrystallizer extends dev.golem.core.gui.CompatContainerScreen<ContainerCrystallizer> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/processing/gui_crystallizer_alt.png");
    // Acid gauge (GUI-relative): a 16×52 column at (35, 18)..(35, 70), matching the 1.12.2 layout.
    private static final int FLUID_X = 35, FLUID_TOP = 18, FLUID_W = 16, FLUID_H = 52;

    public GUICrystallizer(ContainerCrystallizer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    //? if >=26.1 {
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
    *///?}
        //? if >=26.1 {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        //?}
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCrystallizer te = this.menu.getCrystallizer();
        if (te == null) return;

        // Power bar: 16×52 at (152, 18)..(152, 70), filled bottom-up from tex (176, 12..64).
        int p = (int) (te.power * 52 / TileEntityMachineCrystallizer.MAX_POWER);
        if (p > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 70 - p, 176.0F, (float) (64 - p), 16, p, 256, 256);
        }

        // Progress arrow: 28×12 at (80, 47) from tex (176, 0).
        int j = te.progress * 28 / Math.max(te.duration, 1);
        if (j > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 80, this.topPos + 47, 176.0F, 0.0F, j, 12, 256, 256);
        }

        // Acid gauge — a solid-colour level (the fluid's colour), bottom-anchored in the 16×52 column.
        FluidTankNTM ft = te.tank;
        int max = ft.getMaxFill();
        int fh = max <= 0 ? 0 : (int) ((long) ft.getFill() * FLUID_H / max);
        if (fh > 0) {
            int x0 = this.leftPos + FLUID_X;
            int yBottom = this.topPos + FLUID_TOP + FLUID_H;
            graphics.fill(x0, yBottom - fh, x0 + FLUID_W, yBottom, 0xFF000000 | (ft.getTankType().getColor() & 0xFFFFFF));
        }

        // The "Acceptable Upgrades" info-panel glyph (1.12.2 drawInfoPanel(117, 22, …, 8)).
        GuiUtil.drawUpgradeInfoPanel(graphics, this.leftPos + INFO_X, this.topPos + INFO_Y);
    }

    // "Acceptable Upgrades" info-panel glyph (1.12.2 drawInfoPanel at 117, 22).
    private static final int INFO_X = 117, INFO_Y = 22;

    @Override
    //? if >=26.1 {
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    //?} else {
    /*protected void renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    *///?}
        //? if >=26.1 {
        super.extractTooltip(graphics, mouseX, mouseY);
        //?} else {
        /*super.renderTooltip(graphics, mouseX, mouseY);
        *///?}
        TileEntityMachineCrystallizer te = this.menu.getCrystallizer();
        if (te == null) return;
        if (this.isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineCrystallizer.MAX_POWER), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(FLUID_X, FLUID_TOP, FLUID_W, FLUID_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal(te.tank.getTankType().getName()),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(INFO_X, INFO_Y, GuiUtil.INFO_PANEL_SIZE, GuiUtil.INFO_PANEL_SIZE, mouseX, mouseY)) {
            // Info-panel glyph hover: the "Acceptable Upgrades" panel (1.12.2 getUpgradeInfo).
            GuiDraw.tooltip(graphics, this.font, GuiUtil.upgradeInfo(te), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getCrystallizer();
    }
}
