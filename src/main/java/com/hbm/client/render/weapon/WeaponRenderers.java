package com.hbm.client.render.weapon;

import java.util.HashMap;
import java.util.Map;

/**
 * The weapon renderers, by name.
 *
 * <p>The item definition names one ({@code "weapon": "atlas"}) rather than describing the geometry,
 * because a Sedna weapon's first-person body is code — a hand-built transform tree reading animation
 * buses — not something a model JSON can express. What the JSON still owns is the {@code base} model
 * whose {@code display{}} block frames the non-first-person contexts.
 */
public final class WeaponRenderers {

    private static final Map<String, WeaponRenderer> BY_NAME = new HashMap<>();

    private WeaponRenderers() {}

    public static WeaponRenderer byName(String name) {
        WeaponRenderer renderer = BY_NAME.get(name);
        if (renderer == null) throw new IllegalArgumentException("Unknown weapon renderer: " + name);
        return renderer;
    }

    /**
     * Declares the renderers. Deliberately names no items: the item definition does that, so this can
     * run at client-init time on either loader without touching an unflushed registry.
     */
    public static void init() {
        BY_NAME.put("atlas", new AtlasWeaponRenderer());
    }
}
