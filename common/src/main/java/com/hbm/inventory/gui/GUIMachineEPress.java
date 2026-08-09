package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineEPress;
import com.hbm.tileentity.machine.TileEntityMachineEPress;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Client screen for the electric press. Reads {@code power}/{@code progress} off the synced client
 * {@link TileEntityMachineEPress} (BufPacket, not ContainerData). Energy bar (left) + descending stamp
 * progress bar, with an energy-bar hover tooltip — matching the 1.12.2 GUI.
 */
public class GUIMachineEPress extends AbstractContainerScreen<ContainerMachineEPress> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/gui_epress.png");

    public GUIMachineEPress(ContainerMachineEPress menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineEPress press = this.menu.getPress();
        if (press == null) return;

        int i = (int) press.getPowerScaled(52);
        if (i > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 17, this.topPos + 69 - i, 176.0F, (float) (52 - i), 16, i, 256, 256);
        }
        int k = press.getProgressScaled(16);
        if (k > 0) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 79, this.topPos + 35, 192.0F, 0.0F, 18, k, 256, 256);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        TileEntityMachineEPress press = this.menu.getPress();
        if (press == null) return;
        if (this.isHovering(17, 17, 16, 52, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font,
                    GuiUtil.energyTooltip(press.power, TileEntityMachineEPress.maxPower),
                    Optional.empty(), mouseX, mouseY);
        }
        // (No "Acceptable Upgrades" info panel here — the 1.12.2 e‑press GUI has none; the upgrade item's
        // own tooltip [ItemMachineUpgrade + IUpgradeInfoProviderSource] shows this machine's numbers.)
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getPress();
    }
}
