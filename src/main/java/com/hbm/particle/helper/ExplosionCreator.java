package com.hbm.particle.helper;

import net.minecraft.world.level.Level;

/**
 * Server-side effect broadcasts for explosion presets.
 *
 * <p>The three sizes are upstream's, parameter for parameter. Each is one call to
 * {@link ParticleEffects#explosionLarge}, which is a plume plus a spread of real ground thrown into
 * the air; the numbers are what separates a satchel charge from an ATACMS.
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

    /** Downscaled, for small bombs. */
    public static void composeEffectSmall(Level level, double x, double y, double z) {
        ParticleEffects.explosionLarge(level, x, y, z, 10, 2F, 0.5F, 25F, 5, 8, 20, 0.75F, 1F, -2F, 150F);
    }

    public static void composeEffectStandard(Level level, double x, double y, double z) {
        ParticleEffects.explosionLarge(level, x, y, z, 15, 5F, 1F, 45F, 10, 16, 50, 1F, 3F, -2F, 200F);
    }

    /** Upscaled. */
    public static void composeEffectLarge(Level level, double x, double y, double z) {
        ParticleEffects.explosionLarge(level, x, y, z, 30, 6.5F, 2F, 65F, 25, 16, 50, 1.25F, 3F, -2F, 350F);
    }
}
