package com.hbm.inventory.gui;

import com.hbm.inventory.container.ContainerMachineCustom;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import dev.golem.core.Ids;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 1.7 {@code GUIMachineCustom}: {@code gui_custom.png}, 176x256. Unused tank, input, filter and output cells are
 * covered with the blank tiles from the right of the sheet, so one texture serves every configuration. Power bar
 * 16x52 at (150,18), tanks 16x34 from (8,18) and (78,18), progress arrow (78,119) in two halves, heat gauge at
 * (61,53) and the green flux readout at (83,57) when the machine uses them.
 */
public final class GUIMachineCustom extends CompatContainerScreen<ContainerMachineCustom> {

    private static final Identifier texture = Ids.of("hbm", "textures/gui/processing/gui_custom.png");

    public GUIMachineCustom(ContainerMachineCustom menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 256);
    }

    @Override
    protected void extractOwnTooltip(GuiGraphicsExtractor graphics, int x, int y) {
        TileEntityCustomMachine custom = menu.getCustom();
        if (custom == null || custom.config == null) return;

        if (isHovering(150, 18, 16, 52, x, y)) {
            GuiDraw.tooltip(graphics, font, GuiUtil.energyTooltip(custom.power, custom.config.maxPower), Optional.empty(), x, y);
            return;
        }
        if (custom.config.maxHeat > 0 && isHovering(61, 53, 18, 18, x, y)) {
            GuiDraw.tooltip(graphics, font, List.of(Component.literal("Heat:" + String.format(Locale.US, "%,d", custom.heat)
                    + " / " + String.format(Locale.US, "%,d", custom.config.maxHeat))), Optional.empty(), x, y);
            return;
        }
        if (menu.getCarried().isEmpty()) {
            for (Slot slot : menu.slots) {
                int tileIndex = slot.getContainerSlot();
                if (slot instanceof ContainerMachineCustom.SlotPattern && isHovering(slot.x, slot.y, 16, 16, x, y)
                        && tileIndex - 10 < custom.matcher.modes.length && custom.matcher.modes[tileIndex - 10] != null) {
                    GuiDraw.tooltip(graphics, font, List.of(
                            Component.literal("Right click to change").withStyle(ChatFormatting.RED),
                            Component.literal(ModulePatternMatcher.getLabel(custom.matcher.modes[tileIndex - 10]))),
                            Optional.empty(), x, y - 30);
                    return;
                }
            }
        }

        for (int i = 0; i < custom.inputTanks.length; i++) {
            if (isHovering(8 + 18 * i, 18, 16, 34, x, y)) {
                GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(custom.inputTanks[i]), Optional.empty(), x, y);
                return;
            }
        }

        for (int i = 0; i < custom.outputTanks.length; i++) {
            if (isHovering(78 + 18 * i, 18, 16, 34, x, y)) {
                GuiDraw.tooltip(graphics, font, GuiUtil.tankTooltip(custom.outputTanks[i]), Optional.empty(), x, y);
                return;
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int i, int j) {
        TileEntityCustomMachine custom = menu.getCustom();
        // 1.7 drew the config's name for the client language, not the name the server sent
        String name = custom != null && custom.config != null ? custom.config.displayName() : title.getString();
        GuiDraw.text(graphics, font, name, 68 - font.width(name) / 2, 6, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0xFF404040, false);
        if (custom != null && custom.config != null && custom.config.fluxMode) {
            GuiDraw.text(graphics, font, "Flux:" + custom.flux, 83, 57, 0xFF08FF00, false);
        }
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        if (w <= 0 || h <= 0) return;
        GuiDraw.blit(graphics, texture, x, y, (float) u, (float) v, w, h, 256, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float interp) {
        blit(graphics, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        TileEntityCustomMachine custom = menu.getCustom();
        if (custom == null || custom.config == null) return;

        if (custom.config.fluxMode) {
            blit(graphics, leftPos + 78, topPos + 54, 192, 122, 51, 15);
        }
        if (custom.maxHeat > 0) {
            blit(graphics, leftPos + 61, topPos + 53, 236, 0, 18, 18);
            GuiUtil.drawSmoothGauge(graphics, leftPos + 70, topPos + 62, (double) custom.heat / (double) custom.config.maxHeat, 5, 2, 1, 0x7F0000);
        }
        int p = custom.progress * 90 / Math.max(custom.maxProgress, 1);
        blit(graphics, leftPos + 78, topPos + 119, 192, 0, Math.min(p, 44), 16);
        if (p > 44) {
            p -= 44;
            blit(graphics, leftPos + 78 + 44, topPos + 119, 192, 16, p, 16);
        }

        int e = (int) (custom.power * 52 / Math.max(custom.config.maxPower, 1));
        blit(graphics, leftPos + 150, topPos + 70 - e, 176, 52 - e, 16, e);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                if (custom.config.itemInCount <= index) {
                    blit(graphics, leftPos + 7 + j * 18, topPos + 71 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                    blit(graphics, leftPos + 7 + j * 18, topPos + 107 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                }
                if (custom.config.itemOutCount <= index) {
                    blit(graphics, leftPos + 77 + j * 18, topPos + 71 + i * 18, 192 + j * 18, 86 + i * 18, 18, 18);
                }
            }
        }

        for (int i = 0; i < 3; i++) {
            if (custom.config.fluidInCount <= i) {
                blit(graphics, leftPos + 7 + i * 18, topPos + 17, 192 + i * 18, 32, 18, 54);
            }
            if (custom.config.fluidOutCount <= i) {
                blit(graphics, leftPos + 77 + i * 18, topPos + 17, 192 + i * 18, 32, 18, 36);
            }
        }

        for (int i = 0; i < custom.inputTanks.length; i++) {
            drawTank(graphics, custom.inputTanks[i], 8 + 18 * i, 18, 16, 34);
        }

        for (int i = 0; i < custom.outputTanks.length; i++) {
            drawTank(graphics, custom.outputTanks[i], 78 + 18 * i, 18, 16, 34);
        }
    }

    /** 1.7 {@code FluidTank.renderTank(x, bottom, z, width, height)}: the fluid texture tiled up from the bottom. */
    private void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y, int width, int height) {
        TankDisplay.Fill visible = TankDisplay.visibleFill(tank, x, y, width, height);
        if (visible == null) return;
        int x0 = leftPos + visible.x(), top = topPos + visible.top(), bottom = topPos + visible.bottom();
        Identifier fluid = tank.getTankType().getTexture();
        if (Minecraft.getInstance().getResourceManager().getResource(fluid).isPresent()) {
            graphics.enableScissor(x0, top, x0 + visible.width(), bottom);
            for (int ty = bottom - 16; ty > top - 16; ty -= 16)
                GuiDraw.blit(graphics, fluid, x0, ty, 0F, 0F, visible.width(), 16, 16, 16, visible.tint());
            graphics.disableScissor();
        } else {
            GuiDraw.fill(graphics, x0, top, x0 + width, bottom, visible.tint());
        }
    }
}
