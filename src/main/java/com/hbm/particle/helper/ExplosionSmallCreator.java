package com.hbm.particle.helper;

import net.minecraft.world.level.Level;

/** 1.7 ExplosionSmallCreator: fireball clouds, delayed crack, block-dust debris. */
public final class ExplosionSmallCreator {

    private ExplosionSmallCreator() {
    }

    public static void composeEffect(Level level, double x, double y, double z, int cloudCount, float cloudScale,
                                     float cloudSpeedMult) {
        ParticleEffects.explosionSmall(level, x, y, z, cloudCount, cloudScale, cloudSpeedMult);
    }
}
