package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Shared chrome for the five particle-accelerator panels. Every one of them is a 176 x 204 screen
 * carrying the same three readouts off {@link TileEntityCooledBase} — a power bar, the cold and warm
 * perfluoromethyl tanks, and the temperature the machine has to reach before it will run — so they are
 * drawn here once rather than in each subclass, the way {@code GUITurretBase} serves all fourteen
 * turrets.
 */
public abstract class GUIPABase<T extends AbstractContainerMenu> extends CompatContainerScreen<T> {

    /** The temperature at or below which the machine is cool enough to run, in whole kelvin. */
    protected static final int COOL_ENOUGH = (int) TileEntityCooledBase.temperature_target;

    protected GUIPABase(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 176, 204);
    }

    protected abstract Identifier texture();

    protected abstract @Nullable TileEntityCooledBase machine();

    protected void drawPowerBar(GuiGraphicsExtractor graphics, int x, int y, long power, long maxPower) {
        int height = (int) (power * 52 / Math.max(1L, maxPower));
        if (height <= 0) return;
        GuiDraw.blit(graphics, texture(), x, y + 52 - height, 184F, (float) (52 - height), 16, height, 256, 256);
    }

    /** The 8 x 8 "condition met" lamp the panels line up under their status row. */
    protected void drawLamp(GuiGraphicsExtractor graphics, int x, int y) {
        GuiDraw.blit(graphics, texture(), x, y, 176F, 8F, 8, 8, 256, 256);
    }

    protected void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
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

    /**
     * The two-part temperature readout: the target printed in place, and the live value right-aligned
     * above it, red once the machine is too warm to operate.
     */
    protected void drawTemperature(GuiGraphicsExtractor graphics, int targetX, int targetY, int valueRight, int valueY) {
        TileEntityCooledBase machine = machine();
        if (machine == null) return;

        Component target = Component.literal("/" + COOL_ENOUGH + "K").withStyle(ChatFormatting.AQUA);
        GuiDraw.text(graphics, font, target, targetX, targetY, 0xFF404040, false);

        int heat = (int) Math.ceil(machine.temperature);
        Component value = Component.literal(heat + "K")
                .withStyle(heat > COOL_ENOUGH ? ChatFormatting.RED : ChatFormatting.AQUA);
        GuiDraw.text(graphics, font, value, valueRight - font.width(value), valueY, 0xFF404040, false);
    }

    protected void drawTitleAndInventory(GuiGraphicsExtractor graphics, int titleY) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2 - 9, titleY, 0xFF404040, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 0xFF404040, false);
    }

    /** Tooltips for the power bar and the two coolant tanks, at the coordinates the panel drew them. */
    protected boolean commonTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                    int powerX, int powerY, int tankX, int tankY) {
        TileEntityCooledBase machine = machine();
        if (machine == null) return false;

        if (isHovering(powerX, powerY, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.energyTooltip(machine.power, machine.getMaxPower()), mouseX, mouseY);
            return true;
        }
        if (isHovering(tankX, tankY, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(machine.coolantTanks[0]), mouseX, mouseY);
            return true;
        }
        if (isHovering(tankX + 18, tankY, 16, 52, mouseX, mouseY)) {
            tooltip(graphics, GuiUtil.tankTooltip(machine.coolantTanks[1]), mouseX, mouseY);
            return true;
        }
        return false;
    }

    protected void tooltip(GuiGraphicsExtractor graphics, List<Component> text, int mouseX, int mouseY) {
        GuiDraw.tooltip(graphics, font, text, Optional.empty(), mouseX, mouseY);
    }

    protected static String comma(long value) {
        return String.format(Locale.US, "%,d", value);
    }
}
