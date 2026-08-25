package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerCompressor;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityMachineCompressorBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Screen for the compressor — 1.12.2 {@code GUICompressor} ({@code gui_compressor.png}, 176×204 on a
 * 256×256 texture). Tanks at x=17/107 (bottom y=70); power bar x=152; progress bar (42,26) 55 wide ×17;
 * the 5-way compression selector at (43 + j·11, 46) 8×14 — its highlight blits from (193,18).
 * The old title is drawn centred on <b>x=70</b> (not the panel centre) at y=6.
 */
public class GUICompressor extends dev.golem.core.gui.CompatContainerScreen<ContainerCompressor> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/gui_compressor.png");

    public GUICompressor(ContainerCompressor menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 70 - this.font.width(this.title) / 2; // old: centred on x=70, not xSize/2
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityMachineCompressorBase te = this.menu.getTile();
        if (te == null) return;

        // "powered" indicator lamp
        if (te.power >= te.powerRequirement) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 156, this.topPos + 4, 176.0F, 52.0F, 9, 12, 256, 256);
        }

        // compression selector highlight over the chosen level
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 43 + te.tanks[0].getPressure() * 11, this.topPos + 46, 193.0F, 18.0F, 8, 124, 256, 256);

        int i = te.getProgressScaled(55);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 42, this.topPos + 26, 192.0F, 0.0F, i, 17, 256, 256);

        int j = (int) te.getPowerScaled(52);
        if (j > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 152, this.topPos + 70 - j, 176.0F, (float) (52 - j), 16, j, 256, 256);

        drawTank(graphics, te.tanks[0], 17, 18, 16, 52);
        drawTank(graphics, te.tanks[1], 107, 18, 16, 52);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop, int w, int h) {
        int maxF = tank.getMaxFill();
        int fh = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * h / maxF);
        if (fh <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + h;
        int yTop = yBottom - fh;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + w, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, w, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + w, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCompressorBase te = this.menu.getTile();
        if (te == null) return;

        if (this.isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineCompressorBase.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        // Compression selector: old "<j> PU -> <j+1> PU".
        for (int j = 0; j <= TileEntityMachineCompressorBase.MAX_COMPRESSION; j++) {
            if (this.isHovering(43 + j * 11, 46, 8, 14, mouseX, mouseY)) {
                GuiDraw.tooltip(graphics, this.font, List.of(Component.literal(j + " PU -> " + (j + 1) + " PU")), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
        int[] xs = {17, 107};
        for (int k = 0; k < 2; k++) {
            if (this.isHovering(xs[k], 18, 16, 52, mouseX, mouseY)) {
                FluidTankNTM tank = te.tanks[k];
                GuiDraw.tooltip(graphics, this.font, List.of(
                        tank.getTankType().getDisplayName(),
                        Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB"),
                        Component.literal(tank.getPressure() + " PU")), Optional.empty(), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        double mx = event.x(), my = event.y();

        // Old bounds verbatim: x [43+j*11, 43+8+j*11), y (46, 46+14].
        for (int j = 0; j <= TileEntityMachineCompressorBase.MAX_COMPRESSION; j++) {
            if (mc.gameMode != null
                    && this.leftPos + 43 + j * 11 <= mx && mx < this.leftPos + 43 + 8 + j * 11
                    && this.topPos + 46 < my && my <= this.topPos + 46 + 14) {
                mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, j);
                return true;
            }
        }
        return false;
    }
}
