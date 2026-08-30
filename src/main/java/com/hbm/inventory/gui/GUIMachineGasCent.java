package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineGasCent;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.PseudoFluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineGasCent;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Gas centrifuge screen preserving the old 206x204 layout and tank/progress gauges. */
public class GUIMachineGasCent extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineGasCent>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm",
            "textures/gui/processing/gui_centrifuge_gas.png");
    private static final int SHEET = 256;

    public GUIMachineGasCent(ContainerMachineGasCent menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 206, 204);
    }

    @Override
    protected void init() {
        super.init();
        inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, SHEET, SHEET);
        GuiUtil.drawInfoPanel(graphics, leftPos - 12, topPos + 16, 3);
        GuiUtil.drawInfoPanel(graphics, leftPos - 12, topPos + 32, 2);

        TileEntityMachineGasCent gasCent = menu.getGasCent();
        if (gasCent == null) return;

        int power = (int) gasCent.getPowerScaled(52);
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 182, topPos + 69 - power,
                    206F, 52F - power, 16, power, SHEET, SHEET);
        }

        int progress = Math.min(36, gasCent.progress * 36 / Math.max(gasCent.getProcessingSpeed(), 1));
        if (progress > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 70, topPos + 35,
                    206F, 52F, progress, 13, SHEET, SHEET);
        }

        drawTank(graphics, gasCent.tank, gasCent.inputTank.getFill(), gasCent.inputTank.getMaxFill(), 16, 16);
        drawTank(graphics, gasCent.tank, gasCent.inputTank.getFill(), gasCent.inputTank.getMaxFill(), 32, 16);
        drawTank(graphics, gasCent.tank, gasCent.outputTank.getFill(), gasCent.outputTank.getMaxFill(), 138, 16);
        drawTank(graphics, gasCent.tank, gasCent.outputTank.getFill(), gasCent.outputTank.getMaxFill(), 154, 16);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM textureTank,
                          int fill, int maxFill, int x, int y) {
        int height = maxFill <= 0 ? 0 : fill * 52 / maxFill;
        if (height <= 0) return;

        int x0 = leftPos + x;
        int bottom = topPos + y + 52;
        int top = bottom - height;
        Identifier texture = textureTank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + 6, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, texture, x0, tileY, 0F, 0F, 6, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, top, x0 + 6, bottom,
                    0xFF000000 | (textureTank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 94, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineGasCent gasCent = menu.getGasCent();
        if (gasCent == null) return;

        if (isHovering(182, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font,
                    GuiUtil.energyTooltip(gasCent.power, TileEntityMachineGasCent.MAX_POWER),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(15, 15, 24, 55, mouseX, mouseY)) {
            tankTooltip(graphics, gasCent.inputTank.getTankType(),
                    gasCent.inputTank.getFill(), gasCent.inputTank.getMaxFill(), true, mouseX, mouseY);
        } else if (isHovering(137, 15, 25, 55, mouseX, mouseY)) {
            tankTooltip(graphics, gasCent.outputTank.getTankType(),
                    gasCent.outputTank.getFill(), gasCent.outputTank.getMaxFill(), false, mouseX, mouseY);
        } else if (isHovering(-12, 16, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GasCentTooltips.enrichment(),
                    Optional.empty(), leftPos - 8, topPos + 32);
        } else if (isHovering(-12, 32, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GasCentTooltips.output(),
                    Optional.empty(), leftPos - 8, topPos + 48);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, PseudoFluidType type,
                             int fill, int maxFill, boolean input, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, List.of(pseudoFluidName(type, input),
                Component.literal(fill + " / " + maxFill + " mB")),
                Optional.empty(), mouseX, mouseY);
    }

    private Component pseudoFluidName(PseudoFluidType type, boolean input) {
        if (!type.getIfHighSpeed()) return Component.translatable(type.getNameKey());
        return Component.translatable(type.getNameKey())
                .withStyle(input ? ChatFormatting.DARK_RED : ChatFormatting.GOLD);
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getGasCent();
    }
}
