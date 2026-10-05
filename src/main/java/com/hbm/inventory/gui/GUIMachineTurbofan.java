package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineTurbofan;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;


/** Screen for the turbofan — 1.7.10 {@code GUIMachineTurbofan} ({@code generators/gui_turbofan.png}, 176x203). */
public class GUIMachineTurbofan extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineTurbofan>
        implements IUpgradeInfoProviderSource {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/generators/gui_turbofan.png");
    private static final Identifier BLOOD_GAUGE = dev.golem.core.Ids.of("hbm", "textures/gui/gauges/small_round.png");

    public GUIMachineTurbofan(ContainerMachineTurbofan menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
    }

    /** 1.7: title centred on x=43 at y=6, inventory at (8, ySize-96+2), both 4210752. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 43 - font.width(title) / 2, 6, 4210752, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineTurbofan fan = menu.getFan();
        if (fan == null) return;
        if (fan.power > 0) {
            int height = (int) fan.getPowerScaled(52);
            GuiDraw.blit(graphics, TEXTURE, leftPos + 143, topPos + 69 - height,
                    192F, 52F - height, 16, height, 256, 256);
        }
        if (fan.afterburner > 0) {
            int row = Math.min(fan.afterburner, 6) - 1;
            GuiDraw.blit(graphics, TEXTURE, leftPos + 98, topPos + 44, 176F, row * 16F, 16, 16, 256, 256);
        }
        if (fan.showBlood) {
            int frame = fan.blood.getMaxFill() <= 0 ? 0
                    : (int) Math.round(12D * fan.blood.getFill() / fan.blood.getMaxFill());
            frame = Math.max(0, Math.min(12, frame));
            // 1.7 GUIElements.renderGauge(ROUND_SMALL): 13 frames of 18x18 stacked VERTICALLY in an 18x234 strip.
            GuiDraw.blit(graphics, BLOOD_GAUGE, leftPos + 97, topPos + 16,
                    0F, frame * 18F, 18, 18, 18, 234);
        }
        GuiUtil.tank(graphics, fan.tank, leftPos + 35, topPos + 69, 34, 52);
    }

    @Override protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineTurbofan fan = menu.getFan();
        if (fan == null) return;
        if (GuiUtil.hover(mouseX, mouseY, leftPos + 35, topPos + 17, 34, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(fan.tank), mouseX, mouseY);
        } else if (fan.showBlood && GuiUtil.hover(mouseX, mouseY, leftPos + 98, topPos + 17, 16, 16)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.tankTooltip(fan.blood), mouseX, mouseY);
        } else if (GuiUtil.hover(mouseX, mouseY, leftPos + 143, topPos + 17, 16, 52)) {
            GuiUtil.tooltip(graphics, font, GuiUtil.energyTooltip(fan.power, fan.getMaxPower()), mouseX, mouseY);
        }
    }

    @Override public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() { return menu.getFan(); }
}
