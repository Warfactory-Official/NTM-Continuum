package com.hbm.inventory.gui;

import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerCentrifuge;
import com.hbm.tileentity.machine.TileEntityMachineCentrifuge;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Client screen for the standard centrifuge, a straight port of 1.7 {@code GUIMachineCentrifuge}
 * ({@code processing/gui_centrifuge.png}, 182x189): power bar 16x37 at (8,18), four 12x36 progress columns
 * at x=72+20i filled one after another, and the upgrade info glyph at (160,16).
 */
public class GUICentrifuge extends CompatContainerScreen<ContainerCentrifuge>
        implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_centrifuge.png");

    public GUICentrifuge(ContainerCentrifuge menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 182, 189);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int guiLeft = this.leftPos, guiTop = this.topPos;
        GuiDraw.blit(graphics, TEXTURE, guiLeft, guiTop, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCentrifuge centrifuge = this.menu.getCentrifuge();
        if (centrifuge == null) return;

        if (centrifuge.hasPower()) {
            int i1 = (int) centrifuge.getPowerScaled(37);
            if (i1 > 0) {
                GuiDraw.blit(graphics, TEXTURE, guiLeft + 8, guiTop + 55 - i1, 182.0F, 37.0F - i1, 16, i1, 256, 256);
            }
        }

        if (centrifuge.isProcessing()) {
            int p = centrifuge.getProgressScaled(145);
            for (int i = 0; i < 4; i++) {
                int h = Math.min(p, 36);
                if (h > 0) {
                    GuiDraw.blit(graphics, TEXTURE, guiLeft + 72 + i * 20, guiTop + 57 - h, 182.0F, 73.0F - h, 12, h, 256, 256);
                }
                p -= h;
                if (p <= 0) break;
            }
        }

        GuiUtil.drawInfoPanel(graphics, guiLeft + 160, guiTop + 16, 8);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        // 1.7: white title centred on xSize/2 + 36/2; "Inventory" at x=11.
        GuiDraw.text(graphics, this.font, this.title, this.imageWidth / 2 + 36 / 2 - this.font.width(this.title) / 2, 6, 0xffffff, false);
        GuiDraw.text(graphics, this.font, Component.translatable("container.inventory"), 11, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCentrifuge centrifuge = this.menu.getCentrifuge();
        if (centrifuge == null) return;
        if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 8, this.topPos + 18, 16, 37)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(centrifuge.power, TileEntityMachineCentrifuge.MAX_POWER), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, this.leftPos + 160, this.topPos + 16, 8, 8)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.upgradeInfo(centrifuge), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getCentrifuge();
    }
}
