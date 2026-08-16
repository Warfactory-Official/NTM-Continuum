package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.oil.TileEntityOilDrillBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.Optional;

/**
 * Screen shared by the oil derrick and pumpjack — faithful port of the 1.12.2 {@code GUIMachineOilWell}
 * ({@code gui_well.png}, 176×222): power bar 16×34 at (8,17) (fill sampled from u=176), the drill
 * indicator lamp 16×16 at (35,17) (u=176+(state-1)·16, v=52; 0 = drilling shows nothing extra),
 * the "no third tank" cover 18×34 at (34,36) (u=192 — the fracking tower variant exposes a solution
 * tank there), and the two 16×52 tank bars at (62,17) / (107,17). Upgrade info glyph at (156,3).
 */
public class GUIMachineOilWell extends dev.golem.core.gui.CompatContainerScreen<ContainerMachineOilWell> implements IUpgradeInfoProviderSource {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/machine/gui_well.png");
    private static final int TANK_TOP = 17, TANK_W = 16, TANK_H = 52;

    public GUIMachineOilWell(ContainerMachineOilWell menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 222);
    }

    @Override
    protected void init() {
        super.init();
        // The player inventory sits at y=84 (not the vanilla bottom‑anchored position), so the default
        // inventoryLabelY (imageHeight−94) lands in the middle of it — put it just above, like the old GUI
        // (ySize−151+2 = 73). Centre the machine title (the old drawGuiContainerForegroundLayer centred it).
        this.inventoryLabelY = this.imageHeight - 149;
        this.titleLabelX = this.imageWidth / 2 - this.font.width(this.title) / 2;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        TileEntityOilDrillBase te = this.menu.getDrill();
        if (te == null) return;

        // Power bar (16×34 at (8,17), fills upward; fill texture at u=176 v=0).
        int i = te.getMaxPower() <= 0 ? 0 : (int) (te.power * 34 / te.getMaxPower());
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 8, this.topPos + 51 - i, 176.0F, (float) (34 - i), 16, i, 256, 256);

        // Drill indicator lamp: 1 = bottomed out, 2 = power-starved / tanks full.
        int k = te.indicator;
        if (k != 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 35, this.topPos + 17, (float) (176 + (k - 1) * 16), 52.0F, 16, 16, 256, 256);

        if (te.tanks.length >= 3) {
            // Fracking tower: draw the FRACKSOL solution tank (6×32 at (40,69) bottom).
            drawTankCustom(graphics, te.tanks[2], 40, 37, 6, 32);
        } else {
            // The derrick/pumpjack have no third tank — draw the cover plate.
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 34, this.topPos + 36, 192.0F, 0.0F, 18, 34, 256, 256);
        }

        drawTank(graphics, te.tanks[0], 62);
        drawTank(graphics, te.tanks[1], 107);

        GuiUtil.drawUpgradeInfoPanel(graphics, this.leftPos + 156, this.topPos + 3);
    }

    /** Tank fill bar (16×52): the fluid's own 16×16 sprite tiled bottom-up, or a solid colour fallback. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX) {
        int maxF = tank.getMaxFill();
        int h = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * TANK_H / maxF);
        if (h <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + TANK_TOP + TANK_H;
        int yTop = yBottom - h;
        Identifier fluidTex = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
            graphics.enableScissor(x0, yTop, x0 + TANK_W, yBottom);
            for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                GuiDraw.blit(graphics, fluidTex, x0, ty, 0F, 0F, TANK_W, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + TANK_W, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    /** Fill bar with explicit width/height (the fracking tower's narrow solution tank). */
    private void drawTankCustom(GuiGraphicsExtractor graphics, FluidTankNTM tank, int barX, int barTop, int w, int h) {
        int maxF = tank.getMaxFill();
        int fh = maxF <= 0 ? 0 : (int) ((long) tank.getFill() * h / maxF);
        if (fh <= 0) return;
        int x0 = this.leftPos + barX;
        int yBottom = this.topPos + barTop + h;
        int yTop = yBottom - fh;
        graphics.fill(x0, yTop, x0 + w, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityOilDrillBase te = this.menu.getDrill();
        if (te == null) return;

        if (this.isHovering(8, 17, 16, 34, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, te.getMaxPower()), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (te.tanks.length >= 3 && this.isHovering(40, 37, 6, 32, mouseX, mouseY)) { tankTooltip(graphics, te.tanks[2], mouseX, mouseY); return; }
        if (this.isHovering(62, TANK_TOP, TANK_W, TANK_H, mouseX, mouseY)) { tankTooltip(graphics, te.tanks[0], mouseX, mouseY); return; }
        if (this.isHovering(107, TANK_TOP, TANK_W, TANK_H, mouseX, mouseY)) { tankTooltip(graphics, te.tanks[1], mouseX, mouseY); return; }
        if (this.isHovering(156, 3, GuiUtil.INFO_PANEL_SIZE, GuiUtil.INFO_PANEL_SIZE, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, GuiUtil.upgradeInfo(te), Optional.empty(), mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, this.font, java.util.List.of(
                Component.literal(tank.getTankType().getName()),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")
        ), Optional.empty(), mouseX, mouseY);
    }

    @Override
    public com.hbm.tileentity.IUpgradeInfoProvider getUpgradeInfoProvider() {
        return this.menu.getDrill();
    }
}
