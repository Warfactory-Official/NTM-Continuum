package com.hbm.client.render.armor;

import java.util.HashMap;
import java.util.Map;

/**
 * Name → worn-model lookup, the armour counterpart to {@code WeaponRenderers}.
 *
 * <p>1.12.2 needed one {@code ArmorX} item class per suit purely so it could override
 * {@code getArmorModel}. Here the item names its model with {@code ArmorFSB.setArmorModel("t51")} and
 * the renderer is found here, so adding a suit is a registration line plus a part tree — no item class
 * unless the suit also changes behaviour.
 */
public final class ArmorRenderers {

    private ArmorRenderers() {}

    private static final Map<String, ArmorRenderer> RENDERERS = new HashMap<>();

    public static void init() {
        RENDERERS.put("t51", new T51ArmorRenderer());
    }

    /** The renderer for {@code name}, or null if the suit has none (and so draws nothing). */
    public static ArmorRenderer get(String name) {
        return name == null ? null : RENDERERS.get(name);
    }

    /** How many worn models are registered — the count to read when checking this wired up. */
    public static int count() {
        return RENDERERS.size();
    }
}
