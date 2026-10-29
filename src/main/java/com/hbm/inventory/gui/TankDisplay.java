package com.hbm.inventory.gui;

import com.hbm.blocks.StandardInfo;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Tank fill geometry and hover text over explicit server or client fluid metadata.
 */
public final class TankDisplay {

    private TankDisplay() {
    }

    /**
     * The rectangle to draw the fluid in, in screen-relative coordinates, with its tint.
     */
    public record Fill(int x, int top, int width, int bottom, int tint) {
    }

    /**
     * The visible fill, or {@code null} when the tank holds too little to occupy a single pixel.
     */
    public static Fill visibleFill(FluidTankNTM tank, int x, int top, int width, int height, boolean client) {
        int max = tank.getMaxFill();
        int fill = max <= 0 ? 0 : tank.getFill() * height / max;
        if (fill <= 0) return null;
        return new Fill(x, top + height - fill, width, top + height, tint(tank, client));
    }

    /**
     * The tank fluid's tint, forced fully opaque.
     */
    public static int tint(FluidTankNTM tank, boolean client) {
        var type = tank.getTankType();
        int color = client ? type.clientMetadata().getTint() : type.getTint();
        return 0xFF000000 | (color & 0xFFFFFF);
    }

    /**
     * The hover lines: name, fill over capacity, pressure when it is not ambient, then fluid traits.
     */
    public static List<Component> tooltip(FluidTankNTM tank, boolean client) {
        List<Component> lines = new ArrayList<>();
        var type = tank.getTankType();
        lines.add(client ? type.clientMetadata().getDisplayName() : type.getDisplayName());
        lines.add(Component.literal(tank.getFill() + "/" + tank.getMaxFill() + "mB"));
        if (tank.getPressure() != 0) {
            lines.add(Component.literal("Pressure: " + tank.getPressure() + " PU")
                               .withStyle(ChatFormatting.RED));
            // 1.7 FluidTank.renderTankInfo: the blinking compressor warning under the pressure line.
            lines.add(Component.literal("Pressurized, use compressor!").withStyle(
                    BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_RED));
        }
        if (client) tank.getFluid().clientMetadata().addInfo(lines, StandardInfo.shiftHeld());
        else tank.getFluid().addInfo(lines, StandardInfo.shiftHeld());
        return lines;
    }
}
