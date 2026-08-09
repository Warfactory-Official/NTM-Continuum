package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerCombustionEngine;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemPistons;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Screen for the combustion engine — 1.12.2 {@code GUICombustionEngine} ({@code gui_combustion.png},
 * 176×203 on 256×256). Ignition button (79,13), throttle drag x79-114 y38-46, piston icon (80,51),
 * power bar x=143, tank bar x=35 (bottom y=69). */
public class GUICombustionEngine extends AbstractContainerScreen<ContainerCombustionEngine> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("hbm", "textures/gui/gui_combustion.png");

    public GUICombustionEngine(ContainerCombustionEngine menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
    }

    @Override
    protected void init() {
        super.init();
        // The old GUI's foreground draws ONLY the inventory label at (8, ySize−96+2) — no machine title.
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    /** Old {@code drawGuiContainerForegroundLayer} emits only the inventory line (no machine title). */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // Old: the locked slider re-reads mouseX every frame (so holding still off-axis still tracks).
        if (this.isMouseLocked) applyThrottle(mouseX);

        TileEntityMachineCombustionEngine te = this.menu.getTile();
        if (te == null) return;

        ItemStack piston = te.inventory.getStackInSlot(2);
        if (!piston.isEmpty() && piston.getItem() instanceof ItemPistons p) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 80, this.topPos + 51, 176.0F, (float) (52 + p.getVariant().ordinal() * 12), 25, 12, 256, 256);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 79 + (te.setting * 32 / 30), this.topPos + 38, 192.0F, 15.0F, 4, 8, 256, 256);

        if (te.isOn) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 79, this.topPos + 13, 192.0F, 0.0F, 35, 15, 256, 256);
        }

        int i = (int) te.getPowerScaled(53);
        if (i > 0) graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + 143, this.topPos + 69 - i, 176.0F, (float) (52 - i), 16, i, 256, 256);

        drawTank(graphics, te.tank, 35, 17, 16, 52);
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
                graphics.blit(RenderPipelines.GUI_TEXTURED, fluidTex, x0, ty, 0F, 0F, w, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, yTop, x0 + w, yBottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        TileEntityMachineCombustionEngine te = this.menu.getTile();
        if (te == null) return;

        if (this.isHovering(143, 17, 16, 52, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineCombustionEngine.maxPower), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(79, 13, 35, 15, mouseX, mouseY)) {
            // Old drawCustomInfoStat over the 35×15 area (wider than the 16×14 button itself).
            graphics.setTooltipForNextFrame(this.font, List.of(Component.literal("Ignition")), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(79, 38, 36, 8, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(Component.literal(((te.setting * 2) / 10D) + " mB/t")), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (this.isHovering(35, 17, 16, 52, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(this.font, List.of(
                    Component.literal(te.tank.getTankType().getName()),
                    Component.literal(te.tank.getFill() + " / " + te.tank.getMaxFill() + " mB")), Optional.empty(), mouseX, mouseY);
        }
    }

    /**
     * Old {@code isMouseLocked}: the throttle is a GRABBED slider — the hit test runs only when the drag
     * starts (region x [79,115), y (38,46]); once locked the value follows mouseX alone until release.
     * Without the latch, any click sharing the slider's x range (over the slots, the tank, …) would move
     * the throttle.
     */
    private boolean isMouseLocked = false;

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        double mx = event.x(), my = event.y();

        // Ignition button — old hit box (89,13) 16×14 (NOT the 35×15 "Ignition" tooltip area at x=79).
        if (mc.gameMode != null
                && this.leftPos + 89 <= mx && mx < this.leftPos + 89 + 16
                && this.topPos + 13 < my && my <= this.topPos + 13 + 14) {
            playClickSound();
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }

        // Throttle grab — old region x [79, 79+36), y (38, 38+8].
        if (this.leftPos + 79 <= mx && mx < this.leftPos + 79 + 36
                && this.topPos + 38 < my && my <= this.topPos + 38 + 8) {
            playClickSound();
            this.isMouseLocked = true;
            applyThrottle(mx);
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // Old mouseReleased: any left/right release drops the lock.
        if (this.isMouseLocked && (event.button() == 0 || event.button() == 1)) {
            this.isMouseLocked = false;
        }
        return super.mouseReleased(event);
    }

    /** While locked the setting tracks mouseX only — no Y test (old: the drag runs in the render loop). */
    private void applyThrottle(double mouseX) {
        TileEntityMachineCombustionEngine te = this.menu.getTile();
        Minecraft mc = Minecraft.getInstance();
        if (te == null || mc.gameMode == null) return;
        int setting = Mth.clamp((int) ((mouseX - this.leftPos - 81) * 30 / 32), 0, 30);
        if (te.setting != setting) {
            te.setting = setting; // optimistic client update; the server echoes it back on the BufPacket
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 1000 + setting);
        }
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
