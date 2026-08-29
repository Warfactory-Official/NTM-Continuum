package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerCentrifuge;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/** Client screen for the standard centrifuge, preserving the legacy four-column progress display. */
public class GUICentrifuge extends dev.golem.core.gui.CompatContainerScreen<ContainerCentrifuge>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/processing/gui_centrifuge.png");

    public GUICentrifuge(ContainerCentrifuge menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCentrifuge centrifuge = this.menu.getCentrifuge();
        if (centrifuge == null) return;

        if (centrifuge.hasPower()) {
            int power = (int) centrifuge.getPowerScaled(35);
            if (power > 0) {
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + 9, this.topPos + 48 - power,
                        176.0F, 35.0F - power, 16, power, 256, 256);
            }
        }

        if (centrifuge.isProcessing()) {
            int progress = centrifuge.getProgressScaled(145);
            for (int i = 0; i < 4; i++) {
                int height = Math.min(progress, 36);
                GuiDraw.blit(graphics, TEXTURE, this.leftPos + 65 + i * 20,
                        this.topPos + 50 - height, 176.0F, 71.0F - height,
                        12, height, 256, 256);
                progress -= height;
                if (progress <= 0) break;
            }
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCentrifuge centrifuge = this.menu.getCentrifuge();
        if (centrifuge != null && this.isHovering(9, 13, 16, 35, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font,
                    GuiUtil.energyTooltip(centrifuge.power, TileEntityMachineCentrifuge.MAX_POWER),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getCentrifuge();
    }
}
