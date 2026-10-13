package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.blocks.machine.MachineElectricFurnace;
import com.hbm.inventory.container.ContainerMachineElectricFurnace;
import com.hbm.tileentity.machine.TileEntityMachineElectricFurnace;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Client screen for the electric furnace, a straight port of 1.7 {@code GUIMachineElectricFurnace}
 * ({@code processing/gui_electric_furnace.png}, 176x186). Reads {@code power}/{@code progress} off the synced
 * client {@link TileEntityMachineElectricFurnace} (BufPacket); the flames follow the block's LIT state, as 1.7
 * checked for the {@code machine_electric_furnace_on} block.
 */
public class GUIMachineElectricFurnace extends CompatContainerScreen<ContainerMachineElectricFurnace> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_electric_furnace.png");

    // 1.7 drawInfoPanel(guiLeft + 115, guiTop + 19, 8, 8, 8).
    private static final int INFO_X = 115, INFO_Y = 19;

    public GUIMachineElectricFurnace(ContainerMachineElectricFurnace menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineElectricFurnace furnace = this.menu.getFurnace();
        if (furnace == null) return;

        if (furnace.hasPower()) {
            int p = (int) furnace.getPowerScaled(34);
            if (p > 0) {
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 52 - p, 176.0F, (float) (64 - p), 16, p, 256, 256);
            }
        }

        BlockState state = furnace.getBlockState();
        if (state.hasProperty(MachineElectricFurnace.LIT) && state.getValue(MachineElectricFurnace.LIT)) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 45, this.topPos + 20, 192.0F, 12.0F, 18, 16, 256, 256);
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 46, this.topPos + 47, 192.0F, 28.0F, 18, 16, 256, 256);
        }

        int p = furnace.getProgressScaled(28);
        if (p > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 43, this.topPos + 36, 176.0F, 0.0F, p, 12, 256, 256);
        }

        GuiUtil.drawInfoPanel(graphics, this.leftPos + INFO_X, this.topPos + INFO_Y, 8);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7 centres the title on x=70, not xSize/2.
        GuiDraw.text(graphics, this.font, this.title, 70 - this.font.width(this.title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineElectricFurnace furnace = this.menu.getFurnace();
        if (furnace == null) return;
        if (this.isHovering(152, 52 - 34, 16, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(furnace.power, TileEntityMachineElectricFurnace.maxPower),
                    Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(INFO_X, INFO_Y, GuiUtil.INFO_PANEL_SIZE, GuiUtil.INFO_PANEL_SIZE, mouseX, mouseY)) {
            // 1.7 lists "Acceptable Upgrades: Speed, Power-Saving"; the port's provider-driven list says the same.
            GuiDraw.tooltip(graphics, this.font, GuiUtil.upgradeInfo(furnace), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getFurnace();
    }
}
