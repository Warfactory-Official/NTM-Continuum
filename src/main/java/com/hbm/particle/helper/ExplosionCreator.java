package com.hbm.particle.helper;

import net.minecraft.world.level.Level;

/** Server-side effect broadcasts for explosion presets. */
public final class ExplosionCreator {

    private ExplosionCreator() {}

    /** The RBMK meltdown mushroom cloud uses the upstream fixed scale-nine preset. */
    public static void composeEffectRBMKMush(Level level, double x, double y, double z) {
        ParticleEffects.rbmkMush(level, x, y, z, 9F);
    }
}
