package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerBarrel;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * The 1.7 {@code GUIBarrel}: {@code gui_barrel.png} (176×166), the tank drawn 34×52 with its bottom at
 * (71, 69), and the mode button at (151, 34) whose 18×18 icon comes from the strip at u=176, v=mode*18.
 * Same layout as the fluid tank's screen, which the tank readout code is shared with.
 */
public class GUIBarrel extends dev.golem.core.gui.CompatContainerScreen<ContainerBarrel> {

    private static final Identifier TEXTURE = dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_barrel.png");
    private static final String[] MODE_NAMES = {"Input only", "Buffer (in+out)", "Output only", "Disabled"};

    private static final int BAR_X = 71, BAR_TOP = 17, BAR_W = 34, BAR_H = 52, BAR_BOTTOM = 69;
    private static final int BTN_X = 151, BTN_Y = 34, BTN_W = 18, BTN_H = 18;

    public GUIBarrel(ContainerBarrel menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        TileEntityBarrel barrel = this.menu.getBarrel();
        if (barrel == null) return;

        FluidTankNTM ft = barrel.tank;
        int max = ft.getMaxFill();
        int h = max <= 0 ? 0 : (int) ((long) ft.getFill() * BAR_H / max);
        if (h > 0) {
            Identifier fluidTex = ft.getTankType().getTexture();
            int x0 = this.leftPos + BAR_X;
            int yBottom = this.topPos + BAR_BOTTOM;
            int yTop = yBottom - h;
            if (Minecraft.getInstance().getResourceManager().getResource(fluidTex).isPresent()) {
                graphics.enableScissor(x0, yTop, x0 + BAR_W, yBottom);
                for (int ty = yBottom - 16; ty > yTop - 16; ty -= 16) {
                    for (int tx = 0; tx < BAR_W; tx += 16) {
                        int tw = Math.min(16, BAR_W - tx);
                        GuiDraw.blit(graphics, fluidTex, x0 + tx, ty, 0F, 0F, tw, 16, 16, 16);
                    }
                }
                graphics.disableScissor();
            } else {
                graphics.fill(x0, yTop, x0 + BAR_W, yBottom, 0xFF000000 | (ft.getTankType().getColor() & 0xFFFFFF));
            }
        }
        int mode = barrel.mode;
        GuiDraw.blit(graphics, TEXTURE, this.leftPos + BTN_X, this.topPos + BTN_Y, 176.0F, (float) (mode * 18), BTN_W, BTN_H, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityBarrel barrel = this.menu.getBarrel();
        if (barrel == null) return;
        if (this.isHovering(BAR_X, BAR_TOP, BAR_W, BAR_H, mouseX, mouseY)) {
            FluidTankNTM ft = barrel.tank;
            GuiDraw.tooltip(graphics, this.font, List.of(
                    ft.getTankType().getDisplayName(),
                    Component.literal(ft.getFill() + " / " + ft.getMaxFill() + " mB")
            ), Optional.empty(), mouseX, mouseY);
        } else if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, this.font, List.of(
                    Component.literal("Mode: " + MODE_NAMES[barrel.mode % MODE_NAMES.length])
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        if (this.isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            Minecraft mc = Minecraft.getInstance();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        }
        return false;
    }
}
