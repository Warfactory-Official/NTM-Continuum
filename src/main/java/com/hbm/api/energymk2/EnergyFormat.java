package com.hbm.api.energymk2;

import com.hbm.inventory.gui.GuiUtil;
import java.util.List;
import net.minecraft.network.chat.Component;

/** HE shown the way NTM's own screens show it. */
public final class EnergyFormat {

    private EnergyFormat() {}

    /** {@code <power>HE/<max>HE}, short-number formatted. */
    public static List<Component> tooltip(long power, long max) {
        return GuiUtil.energyTooltip(power, max);
    }

    /** Plus the {@code ±<delta>HE/s} line. */
    public static List<Component> tooltip(long power, long max, long delta) {
        return GuiUtil.energyTooltip(power, max, delta);
    }
}
