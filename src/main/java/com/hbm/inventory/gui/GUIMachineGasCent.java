package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineGasCent;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.PseudoFluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.machine.TileEntityMachineGasCent;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Gas centrifuge screen, 1.7 {@code GUIMachineGasCent} ({@code processing/gui_centrifuge_gas.png}, 206x204). */
public class GUIMachineGasCent extends CompatContainerScreen<ContainerMachineGasCent>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm",
            "textures/gui/processing/gui_centrifuge_gas.png");
    private static final int SHEET = 256;

    public GUIMachineGasCent(ContainerMachineGasCent menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 206, 204);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, SHEET, SHEET);
        GuiUtil.drawInfoPanel(graphics, leftPos - 12, topPos + 16, 3);
        GuiUtil.drawInfoPanel(graphics, leftPos - 12, topPos + 32, 2);

        TileEntityMachineGasCent gasCent = menu.getGasCent();
        if (gasCent == null) return;

        // 1.7 GUIMachineGasCent: power bar, progress arrow (getCentrifugeProgressScaled(36), not clamped), four 6x52 tanks.
        int power = (int) gasCent.getPowerScaled(52);
        if (power > 0) {
            GuiDraw.blit(graphics, TEXTURE, leftPos + 182, topPos + 69 - power,
                    206F, 52F - power, 16, power, SHEET, SHEET);
        }

        int progress = gasCent.getProgressScaled(36);
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
        Identifier texture = textureTank.getTankType().clientMetadata().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + 6, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, texture, x0, tileY, 0F, 0F, 6, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, top, x0 + 6, bottom,
                    0xFF000000 | (textureTank.getTankType().clientMetadata().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, Component.translatable("container.inventory"), 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineGasCent gasCent = menu.getGasCent();
        if (gasCent == null) return;

        // 1.7 order: input tank, output tank, power, then the two info panels.
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 15, topPos + 15, 24, 55)) {
            tankTooltip(graphics, gasCent.inputTank.getTankType(),
                    gasCent.inputTank.getFill(), gasCent.inputTank.getMaxFill(), true, mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 137, topPos + 15, 25, 55)) {
            tankTooltip(graphics, gasCent.outputTank.getTankType(),
                    gasCent.outputTank.getFill(), gasCent.outputTank.getMaxFill(), false, mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 182, topPos + 69 - 52, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(gasCent.power, TileEntityMachineGasCent.MAX_POWER), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos - 12, topPos + 16, 16, 16)) {
            GuiDraw.tooltip(graphics, font, GasCentTooltips.enrichment(),
                    Optional.empty(), leftPos - 8, topPos + 32);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos - 12, topPos + 32, 16, 16)) {
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
        // 1.7: the input name is dark red unless the speed upgrade is in (processing speed 150 - 70), then gold;
        // the output name is always gold.
        TileEntityMachineGasCent gasCent = menu.getGasCent();
        boolean slow = gasCent != null && gasCent.getProcessingSpeed() > TileEntityMachineGasCent.PROCESSING_SPEED - 70;
        return Component.translatable(type.getNameKey())
                .withStyle(input && slow ? ChatFormatting.DARK_RED : ChatFormatting.GOLD);
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return menu.getGasCent();
    }
}
