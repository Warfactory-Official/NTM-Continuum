package com.hbm.inventory.gui;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
import dev.golem.core.gui.CompatContainerScreen;
import dev.golem.core.gui.GuiDraw;
import net.minecraft.ChatFormatting;
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
 * Shared chrome for the five particle-accelerator panels (1.7 {@code GUIPA*}). Every one of them is a 176 x 204 screen
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

    /** 1.7 {@code FluidTank.renderTank} (tinted, GL_REPEAT); {@code x, y} is the absolute top-left of the 16x52 gauge. */
    protected void drawTank(GuiGraphicsExtractor graphics, FluidTankNTM tank, int x, int y) {
        NuclearMiscGuiUtil.drawTank(graphics, tank, x, y, 16, 52);
    }

    /**
     * The two-part temperature readout: the target printed in place, and the live value right-aligned
     * above it, red once the machine is too warm to operate.
     */
    protected void drawTemperature(GuiGraphicsExtractor graphics, int targetX, int targetY, int valueRight, int valueY) {
        TileEntityCooledBase machine = machine();
        if (machine == null) return;

        Component target = Component.literal("/" + COOL_ENOUGH + "K").withStyle(ChatFormatting.AQUA);
        GuiDraw.text(graphics, font, target, targetX, targetY, 4210752, false);

        int heat = (int) Math.ceil(machine.temperature);
        Component value = Component.literal(heat + "K")
                .withStyle(heat > COOL_ENOUGH ? ChatFormatting.RED : ChatFormatting.AQUA);
        GuiDraw.text(graphics, font, value, valueRight - font.width(value), valueY, 4210752, false);
    }

    /**
     * The title and "Inventory" labels. The five 1.7 panels disagree on the title's offset, row and colour
     * (detector -8/5/white, source -9/4/white, dipole -9/6/grey, quadrupole -9/6/white, RFC none), so each
     * passes its own.
     */
    protected void drawTitleAndInventory(GuiGraphicsExtractor graphics, int titleXOffset, int titleY, int titleColor) {
        GuiDraw.text(graphics, font, title, imageWidth / 2 - font.width(title) / 2 + titleXOffset, titleY, titleColor, false);
        GuiDraw.text(graphics, font, playerInventoryTitle, 8, imageHeight - 96 + 2, 4210752, false);
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
