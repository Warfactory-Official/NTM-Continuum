package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineGasFlare;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.oil.TileEntityMachineGasFlare;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the flare stack — 1.12.2 {@code GUIMachineGasFlare} ({@code gui_flare_stack.png}, 176×203):
 * power bar 16×94 at (143,-25..69)? — actually 16-wide filling to y=69 from u=176,v=94; the valve
 * indicator 35×10 at (79,15) (u=176,v=0) when on, the ignition indicator 35×14 at (79,49) (u=176,v=10)
 * when burning, the burning flame icon 18×18 at (88,29) (u=176,v=24), tank bar 16×52 at (35,17).
 */
public class GUIMachineGasFlare extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineGasFlare> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/generators/gui_flare_stack.png");

    public GUIMachineGasFlare(ContainerMachineGasFlare menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
    }

    /** The 1.12.2 GUI drew no machine title (only the inventory label) — the texture carries the art. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }


    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineGasFlare te = this.menu.getFlare();
        if (te == null) return;

        int j = (int) te.getPowerScaled(94);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 143, this.topPos + 69 - j, 176.0F, (float) (94 - j), 16, j, 256, 256);

        if (te.isOn) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 15, 176.0F, 0.0F, 35, 10, 256, 256);
        if (te.doesBurn) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 49, 176.0F, 10.0F, 35, 14, 256, 256);
        if (te.isOn && te.doesBurn && te.tank.getFill() > 0) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 88, this.topPos + 29, 176.0F, 24.0F, 18, 18, 256, 256);
        }

        drawTank(graphics, te.tank, 35, 17);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * 52 / maxF);
        if (h <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + 52;
        int yTop = yBottom - h;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + 16, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, 16, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + 16, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineGasFlare te = this.menu.getFlare();
        if (te == null) return;

        if (this.isHovering(143, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineGasFlare.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(35, 17, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    te.tank.getTankType().getDisplayName(),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(79, 16, 35, 10, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.translatable("flare.valve")), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(79, 50, 35, 14, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(Component.translatable("flare.ignition")), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null) {
            if (this.isHovering(89, 16, 16, 10, event.x(), event.y())) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0); // valve
                return true;
            }
            if (this.isHovering(89, 50, 16, 14, event.x(), event.y())) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 1); // ignition
                return true;
            }
        }
        return false;
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getFlare();
    }
}
