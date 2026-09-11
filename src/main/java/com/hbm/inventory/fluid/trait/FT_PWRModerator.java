package com.hbm.inventory.fluid.trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * How a coolant bends the pressurised water reactor's flux. The multiplier scales both the flux the
 * core reports and the heat its fuel gives off, so a good moderator makes the reactor hotter and
 * faster at once. Carried as a plain object in the type's trait map, like the other traits here.
 */
public final class FT_PWRModerator {

    private final double multiplier;

    public FT_PWRModerator(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void addInfo(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("trait.pwrflux")).append("]")
                .withStyle(ChatFormatting.BLUE));
        int percent = (int) (multiplier * 100 - 100);
        info.add(Component.translatable("trait.pwrflux.desc", (percent >= 0 ? "+" : "") + percent + "%")
                .withStyle(ChatFormatting.BLUE));
    }
}
