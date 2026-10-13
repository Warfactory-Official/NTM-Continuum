package com.hbm.config;

import java.util.Locale;

/**
 * 1.7 {@code PotionConfig}, category {@code 08_potion_effects} of {@code hbm.cfg}. 1.7 read "NORMAL" as 1, "TERRARIA"
 * as 2 and anything else - including its default "OFF" - as 0, so potion sickness is off out of the box.
 *
 * <p>The twelve {@code 8.00}..{@code 8.11} potion-ID keys are not ported: effects are registered by name, and the port
 * persists no numeric ids.
 */
public final class PotionConfig {

    private PotionConfig() {}

    /** 0 off (1.7 default), 1 normal, 2 "terraria" (twelve times as long). */
    public static int potionSickness = 0;

    public static void loadFromConfig(Configuration config) {

        final String CATEGORY_POTION = CommonConfig.CATEGORY_POTION;

        String s = CommonConfig.createConfigString(config, CATEGORY_POTION, "8.S0_potionSickness", "Valid configs include \"NORMAL\" and \"TERRARIA\", otherwise potion sickness is turned off", "OFF");

        // 1.7 only ever raised the field from its 0 default; a reload that turns sickness back OFF must lower it too
        potionSickness = 0;
        if ("normal".equals(s.toLowerCase(Locale.US)))
            potionSickness = 1;
        if ("terraria".equals(s.toLowerCase(Locale.US)))
            potionSickness = 2;
    }
}
