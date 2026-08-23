package com.hbm.tileentity.machine.rbmk;

import java.util.Locale;

/**
 * The five manual-control-rod dye colours (1.7/1.12.2 RBMKColor); ordinal is the NBT/wire value, null = undyed.
 */
public enum RBMKColor {
    RED(0xFFE02020),
    YELLOW(0xFFE0E020),
    GREEN(0xFF20C020),
    BLUE(0xFF2040E0),
    PURPLE(0xFFA020E0);

    public static final RBMKColor[] VALUES = values();

    public final int argb;

    RBMKColor(int argb) {
        this.argb = argb;
    }

    public String texture() {
        return name().toLowerCase(Locale.ROOT);
    }
}
