package com.hbm.config;

/**
 * 1.7 {@code PotionConfig}'s {@code 8.S0_potionSickness} switch, as a plain constant like the port's other
 * configs (no file-backed config yet). 1.7 read "NORMAL" as 1, "TERRARIA" as 2 and anything else - including
 * its default "OFF" - as 0, so potion sickness is off out of the box.
 */
public final class PotionConfig {

    private PotionConfig() {}

    /** 0 off (1.7 default), 1 normal, 2 "terraria" (twelve times as long). */
    public static int potionSickness = 0;
}
