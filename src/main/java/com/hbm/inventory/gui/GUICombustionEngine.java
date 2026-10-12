package com.hbm.inventory.gui;

import dev.golem.core.gui.GuiDraw;
import com.hbm.inventory.container.ContainerCombustionEngine;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.items.machine.ItemPistons;
import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/** Screen for the combustion engine — 1.7.10 {@code GUICombustionEngine} ({@code generators/gui_combustion.png},
 * 176×203 on 256×256). Ignition button (89,13) 16×14, throttle drag x79-114 y38-46, piston icon (80,51),
 * power bar x=143, tank bar x=35 (bottom y=69). */
public class GUICombustionEngine extends dev.golem.core.gui.CompatContainerScreen<ContainerCombustionEngine> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/generators/gui_combustion.png");

    public GUICombustionEngine(ContainerCombustionEngine menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 203);
    }

    /** 1.7 {@code drawGuiContainerForegroundLayer}: only the inventory line at (8, ySize-96+2), 4210752 — no title. */
    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        GuiDraw.text(graphics, this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);

        // 1.7: the locked slider re-reads mouseX every frame in drawScreen (so holding still off-axis still tracks).
        if (this.isMouseLocked) applyThrottle(mouseX);

        TileEntityMachineCombustionEngine te = this.menu.getTile();
        if (te == null) return;

        ItemStack piston = te.inventory.getStackInSlot(2);
        if (!piston.isEmpty() && piston.getItem() instanceof ItemPistons p) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 80, this.topPos + 51, 176.0F, (float) (52 + p.getVariant().ordinal() * 12), 25, 12, 256, 256);
        }

        GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79 + (te.setting * 32 / 30), this.topPos + 38, 192.0F, 15.0F, 4, 8, 256, 256);

        if (te.isOn) {
            GuiDraw.blit(graphics, TEXTURE, this.leftPos + 79, this.topPos + 13, 192.0F, 0.0F, 35, 15, 256, 256);
        }

        // 1.7: i = power * 53 / maxPower (53, not 52: a full bar reaches one pixel past the 52-tall window, as in 1.7).
        int i = (int) (te.power * 53 / TileEntityMachineCombustionEngine.maxPower);
        if (i > 0) GuiDraw.blit(graphics, TEXTURE, this.leftPos + 143, this.topPos + 69 - i, 176.0F, (float) (52 - i), 16, i, 256, 256);

        // 1.7: engine.tank.renderTank(guiLeft + 35, guiTop + 69, z, 16, 52) — y is the bottom edge, tinted.
        GuiUtil.tank(graphics, te.tank, this.leftPos + 35, this.topPos + 69, 16, 52);
    }

    /** 1.7 {@code drawScreen} tooltips, same regions, order and strings. */
    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineCombustionEngine te = this.menu.getTile();
        if (te == null) return;
        int gl = this.leftPos, gt = this.topPos;

        if (!this.isMouseLocked) {
            if (GuiUtil.hover(mouseX, mouseY, gl + 143, gt + 17, 16, 52)) {
                GuiUtil.tooltip(graphics, this.font, GuiUtil.energyTooltip(te.power, TileEntityMachineCombustionEngine.maxPower), mouseX, mouseY);
                return;
            }
            if (GuiUtil.hover(mouseX, mouseY, gl + 35, gt + 17, 16, 52)) {
                GuiUtil.tooltip(graphics, this.font, GuiUtil.tankTooltip(te.tank), mouseX, mouseY);
                return;
            }
        }

        // 1.7: throttle readout while dragging or hovering x [80,114), y (38,46]; drawn at the clamped mouse.
        if (this.isMouseLocked || (gl + 80 <= mouseX && gl + 80 + 34 > mouseX && gt + 38 < mouseY && gt + 38 + 8 >= mouseY)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines(((te.setting * 2) / 10D) + "mB/t"),
                    Mth.clamp(mouseX, gl + 80, gl + 114), Mth.clamp(mouseY, gt + 38, gt + 46));
            return;
        }

        ItemStack piston = te.inventory.getStackInSlot(2);
        if (!piston.isEmpty() && piston.getItem() instanceof ItemPistons p && GuiUtil.hover(mouseX, mouseY, gl + 79, gt + 50, 35, 14)) {
            double power = 0;
            FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(te.tank.getTankType());
            if (fuel != null) {
                int g = fuel.grade().ordinal();
                double eff = g < p.efficiency().length ? p.efficiency()[g] : 0;
                power = te.setting * 0.2 * fuel.energyPerBucket() / 1_000D * eff;
            }
            String c = ChatFormatting.YELLOW + "";
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines(
                    c + String.format(Locale.US, "%,d", (int) power) + " HE/t",
                    c + String.format(Locale.US, "%,d", (int) (power * 20)) + " HE/s"), mouseX, mouseY);
            return;
        }

        if (GuiUtil.hover(mouseX, mouseY, gl + 79, gt + 13, 35, 15)) {
            GuiUtil.tooltip(graphics, this.font, GuiUtil.lines("Ignition"), mouseX, mouseY);
        }
    }

    /**
     * Old {@code isMouseLocked}: the throttle is a GRABBED slider — the hit test runs only when the drag
     * starts (region x [79,115), y (38,46]); once locked the value follows mouseX alone until release.
     */
    private boolean isMouseLocked = false;

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        Minecraft mc = Minecraft.getInstance();
        double mx = event.x(), my = event.y();

        // Ignition button — 1.7 hit box (89,13) 16×14 (NOT the 35×15 "Ignition" tooltip area at x=79).
        if (mc.gameMode != null
                && this.leftPos + 89 <= mx && mx < this.leftPos + 89 + 16
                && this.topPos + 13 < my && my <= this.topPos + 13 + 14) {
            GuiUtil.playClickSound();
            mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            return true;
        }

        // Throttle grab — 1.7 region x [79, 79+36), y (38, 38+8].
        if (this.leftPos + 79 <= mx && mx < this.leftPos + 79 + 36
                && this.topPos + 38 < my && my <= this.topPos + 38 + 8) {
            GuiUtil.playClickSound();
            this.isMouseLocked = true;
            applyThrottle(mx);
            return true;
        }

        return false;
    }

    @Override
    protected boolean mouseReleased(dev.golem.core.gui.MouseInput event) {
        // 1.7 mouseMovedOrUp: any left/right release drops the lock.
        if (this.isMouseLocked && (event.button() == 0 || event.button() == 1)) {
            this.isMouseLocked = false;
        }
        return false;
    }

    /** While locked the setting tracks mouseX only — no Y test (1.7: the drag runs in the render loop). */
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
}
