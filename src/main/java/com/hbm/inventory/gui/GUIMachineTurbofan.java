package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineTurbofan;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/** Screen for the legacy turbofan. */
public class GUIMachineTurbofan extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineTurbofan>
        implements IUpgradeInfoProviderSource {
    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/generators/gui_turbofan.png");
    private static final Identifier BLOOD_GAUGE = dev.golem.core.Ids.of("hbm", "textures/gui/gauges/small_round.png");

    public GUIMachineTurbofan(ContainerMachineTurbofan menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
    }

    @Override protected void init() {
        super.init();
        titleLabelX = 43 - font.width(title) / 2;
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
            GuiDraw.blit(graphics, BLOOD_GAUGE, leftPos + 97, topPos + 16,
                    frame * 18F, 0F, 18, 18, 234, 18);
        }
        drawTank(graphics, fan.tank, 35, 17, 34, 52);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int max = tank.getMaxFill();
        int fill = max <= 0 ? 0 : tank.getFill() * height / max;
        if (fill <= 0) return;
        int x0 = leftPos + x;
        int bottom = topPos + y + height;
        int top = bottom - fill;
        Identifier texture = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + width, bottom);
            for (int ty = bottom - 16; ty > top - 16; ty -= 16)
                GuiDraw.blit(graphics, texture, x0, ty, 0F, 0F, width, 16, 16, 16);
            graphics.disableScissor();
        } else GuiDraw.fill(graphics, x0, top, x0 + width, bottom,
                0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
    }

    @Override protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineTurbofan fan = menu.getFan();
        if (fan == null) return;
        if (isHovering(143, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(fan.power, fan.getMaxPower()), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(35, 17, 34, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(fan.tank.getTankType().getDisplayName(),
                    Component.literal(fan.tank.getFill() + " / " + fan.tank.getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        } else if (isHovering(98, 17, 16, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(fan.blood.getTankType().getDisplayName(),
                    Component.literal(fan.blood.getFill() + " / " + fan.blood.getMaxFill() + " mB")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() { return menu.getFan(); }
}
