package com.hbm.particle.helper;

import net.minecraft.world.level.Level;

/**
 * Server-side effect broadcasts for explosion presets.
 *
 * <p>1.12.2's standard and large presets are their own particle families. This port has neither, so
 * both are approximated with the small-blast fireball cluster at the counts and scales that read
 * closest — the same effect, sized up, rather than a new one invented for the occasion.
 */
public final class ExplosionCreator {

    private ExplosionCreator() {}

    /** The RBMK meltdown mushroom cloud uses the upstream fixed scale-nine preset. */
    public static void composeEffectRBMKMush(Level level, double x, double y, double z) {
        composeEffectRBMKMush(level, x, y, z, 9F);
    }

    /**
     * The mushroom cloud at the scale the caller states: every 1.12.2 sender picks its own — 35 and 20
     * for the two thermobaric HIMARS rockets, 15 for the white-phosphorus one, 9 for a meltdown.
     */
    public static void composeEffectRBMKMush(Level level, double x, double y, double z, float scale) {
        ParticleEffects.rbmkMush(level, x, y, z, scale);
    }

    public static void composeEffectStandard(Level level, double x, double y, double z) {
        ParticleEffects.explosionSmall(level, x, y, z, 15, 4F, 1F);
    }

    public static void composeEffectSmall(Level level, double x, double y, double z) {
        ParticleEffects.explosionSmall(level, x, y, z, 10, 2.5F, 1F);
    }

    public static void composeEffectLarge(Level level, double x, double y, double z) {
        ParticleEffects.explosionSmall(level, x, y, z, 25, 8F, 1F);
    }
}
