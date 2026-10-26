package com.hbm.inventory.gui;

import com.hbm.blocks.StandardInfo;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.util.BobMathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * How a fluid tank presents itself: the rectangle it fills, the tint it fills it with, and the
 * lines it says on hover. None of that needs a client.
 *
 * <p>It lives here rather than on the screens, which draw it, so a dedicated-server GameTest can reach
 * it. None of it calls a client method, but resolving the owning class pulls in its superclass, and
 * NeoForge answers a client superclass on a server with
 * {@code Attempted to load class … for invalid dist DEDICATED_SERVER}. {@code GUIMachineTurbineGas}
 * extends {@code AbstractContainerScreen}; {@code GuiUtil} imports {@code Minecraft} and
 * {@code SimpleSoundInstance}. Here nothing in the signatures or the imports is client-only, and the
 * screens call in.
 */
public final class TankDisplay {

    private TankDisplay() {}

    /** The rectangle to draw the fluid in, in screen-relative coordinates, with its tint. */
    public record Fill(int x, int top, int width, int bottom, int tint) {}

    /** The visible fill, or {@code null} when the tank holds too little to occupy a single pixel. */
    public static Fill visibleFill(FluidTankNTM tank, int x, int top, int width, int height) {
        int max = tank.getMaxFill();
        int fill = max <= 0 ? 0 : tank.getFill() * height / max;
        if (fill <= 0) return null;
        return new Fill(x, top + height - fill, width, top + height, tint(tank));
    }

    /** The tank fluid's tint, forced fully opaque. */
    public static int tint(FluidTankNTM tank) {
        return 0xFF000000 | (tank.getTankType().clientMetadata().getTint() & 0xFFFFFF);
    }

    /** The hover lines: name, fill over capacity, pressure when it is not ambient, then fluid traits. */
    public static List<Component> tooltip(FluidTankNTM tank) {
        List<Component> lines = new ArrayList<>();
        lines.add(tank.getTankType().clientMetadata().getDisplayName());
        lines.add(Component.literal(tank.getFill() + "/" + tank.getMaxFill() + "mB"));
        if (tank.getPressure() != 0) {
            lines.add(Component.literal("Pressure: " + tank.getPressure() + " PU")
                    .withStyle(ChatFormatting.RED));
            // 1.7 FluidTank.renderTankInfo: the blinking compressor warning under the pressure line.
            lines.add(Component.literal("Pressurized, use compressor!").withStyle(
                    BobMathUtil.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_RED));
        }
        tank.getFluid().clientMetadata().addInfo(lines, StandardInfo.shiftHeld());
        return lines;
    }
}
