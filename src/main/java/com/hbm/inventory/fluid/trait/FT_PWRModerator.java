package com.hbm.inventory.fluid.trait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The 1.7 {@code FT_PWRModerator}: a coolant that also moderates the PWR core, scaling both the flux
 * and the heat the loaded fuel emits by {@link #getMultiplier()} while the cold tank holds any of it.
 * MUG 1.15, heavy water 1.25, lead 0.75, thorium salt 2.5.
 */
public final class FT_PWRModerator {

    private final double multiplier;

    public FT_PWRModerator(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    /** 1.7 {@code addInfo}: the blue bracketed trait tag. */
    public void addInfo(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.pwrFluxMultiplier")).append("]")
                .withStyle(ChatFormatting.BLUE));
    }

    /** 1.7 {@code addInfoHidden}: the shifted-tooltip percentage line. */
    public void addInfoHidden(List<Component> info) {
        int mult = (int) (multiplier * 100 - 100);
        info.add(Component.translatable("hbmfluid.trait.pwrFluxCore").append(" " + (mult >= 0 ? "+" : "") + mult + "%")
                .withStyle(ChatFormatting.BLUE));
    }
}
