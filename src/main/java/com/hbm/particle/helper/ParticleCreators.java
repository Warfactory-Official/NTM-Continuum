package com.hbm.particle.helper;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** The ntm-next particle dispatch names used by the copied entities, routed onto {@link ParticleEffects}. */
public final class ParticleCreators {

    private ParticleCreators() {
    }

    /** {@code countDivisor} 0 stands in for 1.7's absent {@code cDiv} key and keeps the full count. */
    public static void giblets(Level level, Entity entity, int gibType, int countDivisor) {
        ParticleEffects.giblets(level, entity, gibType);
    }
}
