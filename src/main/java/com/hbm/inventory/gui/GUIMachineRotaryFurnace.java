package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.container.ContainerMachineRotaryFurnace;
import com.hbm.tileentity.machine.TileEntityMachineRotaryFurnace;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 1.7 {@code GUIMachineRotaryFurnace}: {@code gui_rotary_furnace.png}, 176x186. */
public final class GUIMachineRotaryFurnace extends CompatContainerScreen<ContainerMachineRotaryFurnace> {

    private static final Identifier TEXTURE = Ids.of("hbm", "textures/gui/processing/gui_rotary_furnace.png");

    public GUIMachineRotaryFurnace(ContainerMachineRotaryFurnace menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 186);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GuiDraw.blit(graphics, TEXTURE, leftPos, topPos, 0F, 0F, imageWidth, imageHeight, 256, 256);
        TileEntityMachineRotaryFurnace furnace = menu.getFurnace();
        if (furnace == null) return;

        int p = (int) Math.ceil(furnace.progress * 33);
        if (p > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 63, topPos + 30, 176F, 0F, p, 10, 256, 256);

        if (furnace.maxBurnTime > 0) {
            int b = furnace.burnTime * 14 / furnace.maxBurnTime;
            if (b > 0) GuiDraw.blit(graphics, TEXTURE, leftPos + 26, topPos + 69 - b, 176F, 24F - b, 14, b, 256, 256);
        }

        if (furnace.output != null) {
            int hex = furnace.output.material.moltenColor;
            int amount = furnace.output.amount * 52 / TileEntityMachineRotaryFurnace.maxOutput;
            if (amount > 0) {
                // The old pass drew the bar tinted with the molten colour, then a 30% white additive wash.
                GuiDraw.blit(graphics, TEXTURE, leftPos + 98, topPos + 70 - amount, 176F, 76F - amount, 16, amount, 256, 256,
                        0xFF000000 | (hex & 0xFFFFFF));
                GuiDraw.blit(graphics, TEXTURE, leftPos + 98, topPos + 70 - amount, 176F, 76F - amount, 16, amount, 256, 256,
                        0x4DFFFFFF);
            }
        }

        drawTankHorizontal(graphics, furnace.tanks[0], 8, 36, 52, 16);
        drawTank(graphics, furnace.tanks[1], 134, 18);
        drawTank(graphics, furnace.tanks[2], 152, 18);
    }

    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        int height = tank.getMaxFill() <= 0 ? 0 : tank.getFill() * 52 / tank.getMaxFill();
        if (height <= 0) return;
        int x0 = leftPos + x;
        int bottom = topPos + y + 52;
        int top = bottom - height;
        Identifier texture = tank.getTankType().clientMetadata().getTexture();
        int tint = 0xFF000000 | (tank.getTankType().clientMetadata().getTint() & 0xFFFFFF);
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x0, top, x0 + 16, bottom);
            for (int tileY = bottom - 16; tileY > top - 16; tileY -= 16) {
                GuiDraw.blit(graphics, texture, x0, tileY, 0F, 0F, 16, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, top, x0 + 16, bottom, tint);
        }
    }

    /** The old {@code renderTank(x, y, z, width, height, 1)}: a horizontal bar filling left to right. */
    private void drawTankHorizontal(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        int fill = tank.getMaxFill() <= 0 ? 0 : tank.getFill() * width / tank.getMaxFill();
        if (fill <= 0) return;
        int x0 = leftPos + x;
        int y0 = topPos + y;
        Identifier texture = tank.getTankType().clientMetadata().getTexture();
        int tint = 0xFF000000 | (tank.getTankType().clientMetadata().getTint() & 0xFFFFFF);
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            graphics.enableScissor(x0, y0, x0 + fill, y0 + height);
            for (int tileX = x0; tileX < x0 + fill; tileX += 16) {
                GuiDraw.blit(graphics, texture, tileX, y0, 0F, 0F, 16, 16, 16, 16, tint);
            }
            graphics.disableScissor();
        } else {
            graphics.fill(x0, y0, x0 + fill, y0 + height, tint);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        GuiDraw.text(graphics, font, title, (imageWidth - 54) / 2 - font.width(title) / 2, 6, 0x404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        TileEntityMachineRotaryFurnace furnace = menu.getFurnace();
        if (furnace == null) return;

        if (isHovering(8, 36, 52, 16, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(furnace.tanks[0]), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(134, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(furnace.tanks[1]), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(152, 18, 16, 52, mouseX, mouseY)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(furnace.tanks[2]), Optional.empty(), mouseX, mouseY);
            return;
        }
        if (isHovering(44, 54, 16, 16, mouseX, mouseY) && !menu.getSlot(4).hasItem()) {
            List<String> bonuses = TileEntityMachineRotaryFurnace.burnModule.getDesc();
            if (!bonuses.isEmpty()) {
                List<Component> lines = new ArrayList<>();
                for (String line : bonuses) lines.add(Component.literal(line));
                GuiDraw.tooltip(graphics, font, lines, Optional.empty(), mouseX, mouseY);
            }
            return;
        }
        if (isHovering(98, 18, 16, 52, mouseX, mouseY)) {
            if (furnace.output == null) {
                GuiDraw.tooltip(graphics, font, List.of(Component.literal("Empty").withStyle(ChatFormatting.RED)),
                        Optional.empty(), mouseX, mouseY);
            } else {
                boolean shift = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isShiftKeyDown();
                GuiDraw.tooltip(graphics, font, List.of(furnace.output.material.getLocalizedName().copy().append(": "
                        + Mats.formatAmount(furnace.output.amount, shift)).withStyle(ChatFormatting.YELLOW)),
                        Optional.empty(), mouseX, mouseY);
            }
        }
    }
}
