package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerBarrel;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.TileEntityBarrel;
import dev.golem.core.gui.GuiDraw;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Client screen for the fluid drum. Same geometry as {@link GUIMachineFluidTank} — the fork draws both
 * from the same layout — over the drum's own background.
 */
public class GUIBarrel extends dev.golem.core.gui.CompatContainerScreen<ContainerBarrel> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/storage/gui_barrel.png");
    private static final String[] MODE_NAMES = {"Input only", "Buffer (in+out)", "Output only", "Disabled"};

    private static final int BAR_X = 71, BAR_TOP = 17, BAR_W = 34, BAR_H = 52, BAR_BOTTOM = 69;
    private static final int BTN_X = 151, BTN_Y = 34, BTN_W = 18, BTN_H = 18;

    public GUIBarrel(ContainerBarrel menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);

        TileEntityBarrel barrel = menu.getBarrel();
        if (barrel == null) return;

        FluidTankNTM ft = barrel.tank;
        int max = ft.getMaxFill();
        int h = max <= 0 ? 0 : (int) ((long) ft.getFill() * BAR_H / max);
        if (h > 0) {
            Identifier fluidTex = ft.getTankType().getTexture();
            int x0 = leftPos + BAR_X;
            int yBottom = topPos + BAR_BOTTOM;
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
                graphics.fill(x0, yTop, x0 + BAR_W, yBottom,
                        0xFF000000 | (ft.getTankType().getColor() & 0xFFFFFF));
            }
        }

        GuiDraw.blit(graphics, TEXTURE, leftPos + BTN_X, topPos + BTN_Y, 176.0F,
                (float) (barrel.mode * 18), BTN_W, BTN_H, 256, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityBarrel barrel = menu.getBarrel();
        if (barrel == null) return;

        if (isHovering(BAR_X, BAR_TOP, BAR_W, BAR_H, mouseX, mouseY)) {
            FluidTankNTM ft = barrel.tank;
            GuiDraw.tooltip(graphics, font, List.of(
                    ft.getTankType().getDisplayName(),
                    Component.literal(ft.getFill() + " / " + ft.getMaxFill() + " mB")
            ), Optional.empty(), mouseX, mouseY);
        } else if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, List.of(
                    Component.literal("Mode: " + MODE_NAMES[barrel.mode % MODE_NAMES.length])
            ), Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean mouseClicked(dev.golem.core.gui.MouseInput event, boolean doubleClick) {
        if (isHovering(BTN_X, BTN_Y, BTN_W, BTN_H, event.x(), event.y())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
        }
        return false;
    }
}
