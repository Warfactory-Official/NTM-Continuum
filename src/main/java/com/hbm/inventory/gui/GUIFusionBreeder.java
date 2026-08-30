package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerFusionBreeder;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Fusion breeder control panel, retaining the legacy 176 x 200 layout. */
public class GUIFusionBreeder extends dev.golem.core.gui.CompatContainerScreen<ContainerFusionBreeder> {

    private static final Identifier TEXTURE =
            dev.golem.core.Ids.of("hbm", "textures/gui/reactors/gui_fusion_breeder.png");

    public GUIFusionBreeder(ContainerFusionBreeder menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 200);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        GuiDraw.blit(graphics, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);

        TileEntityFusionBreeder breeder = menu.getBreeder();
        if (breeder == null) return;

        int progress = Math.min(42, (int) Math.ceil(breeder.progress * 42D / TileEntityFusionBreeder.CAPACITY));
        if (progress > 0) GuiDraw.blit(graphics, TEXTURE, x + 67, y + 48, 176F, 0F, progress, 10, 256, 256);

        double gauge = 1D - Math.exp(-breeder.neutronEnergy * 10D / TileEntityFusionBreeder.CAPACITY);
        drawNeedle(graphics, x + 88, y + 32, gauge);

        drawTank(graphics, breeder.tanks[0], x + 26, y + 18);
        drawTank(graphics, breeder.tanks[1], x + 134, y + 18);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        int max = tank.getMaxFill();
        int height = max <= 0 ? 0 : (int) ((long) tank.getFill() * 52 / max);
        if (height <= 0) return;

        int bottom = y + 52;
        int top = bottom - height;
        Identifier fluidTexture = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluidTexture).isPresent()) {
            graphics.enableScissor(x, top, x + 16, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, fluidTexture, x, tileY, 0F, 0F, 16, 16, 16, 16);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x, top, x + 16, bottom, 0xFF000000 | (tank.getTankType().getColor() & 0xFFFFFF));
        }
    }

    private static void drawNeedle(GuiGraphicsExtractor graphics, int x, int y, double progress) {
        progress = Math.max(0D, Math.min(1D, progress));
        GuiDraw.pushMatrix(graphics);
        GuiDraw.translate(graphics, x, y);
        GuiDraw.rotate(graphics, (float) Math.toRadians(progress * 270D + 45D));
        GuiDraw.scale(graphics, 1F / 8F, 1F / 8F);
        fillNeedle(graphics, 1.5D, 0xFF000000);
        fillNeedle(graphics, 1D, 0xFFA00000);
        GuiDraw.popMatrix(graphics);
    }

    private static void fillNeedle(GuiGraphicsExtractor graphics, double multiplier, int color) {
        double tip = 5D * multiplier * 8D;
        double back = 2D * multiplier * 8D;
        double side = multiplier * 8D;
        double total = tip + back;
        for (int row = (int) Math.round(-back); row < (int) Math.round(tip); row++) {
            int halfWidth = (int) Math.round(side * (tip - row) / total);
            if (halfWidth > 0) graphics.fill(-halfWidth, row, halfWidth, row + 1, color);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, 88 - font.width(title) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 35, imageHeight - 93, 0xFF404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityFusionBreeder breeder = menu.getBreeder();
        if (breeder == null) return;

        if (isHovering(79, 23, 18, 18, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal("-> "
                    + String.format(Locale.US, "%,.0f", breeder.neutronEnergy) + " flux/t")
                    .withStyle(ChatFormatting.GREEN)), mouseX, mouseY);
        } else if (isHovering(67, 46, 42, 14, mouseX, mouseY)) {
            tooltip(graphics, List.of(Component.literal(String.format(Locale.US, "%,.0f / %,.0f flux",
                    breeder.progress, TileEntityFusionBreeder.CAPACITY))), mouseX, mouseY);
        } else if (isHovering(26, 18, 16, 52, mouseX, mouseY)) {
            tankTooltip(graphics, breeder.tanks[0], mouseX, mouseY);
        } else if (isHovering(134, 18, 16, 52, mouseX, mouseY)) {
            tankTooltip(graphics, breeder.tanks[1], mouseX, mouseY);
        }
    }

    private void tankTooltip(GuiGraphicsExtractor graphics, FluidTankNTM tank, int mouseX, int mouseY) {
        tooltip(graphics, List.of(tank.getTankType().getDisplayName(),
                Component.literal(tank.getFill() + " / " + tank.getMaxFill() + " mB")), mouseX, mouseY);
    }

    private void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, text, Optional.empty(), mouseX, mouseY);
    }
}
