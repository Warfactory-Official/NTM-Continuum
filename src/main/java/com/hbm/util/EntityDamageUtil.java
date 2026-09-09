package com.hbm.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/** Damage helpers that bypass rules vanilla applies but 1.12.2's weapons do not. */
public final class EntityDamageUtil {

    private EntityDamageUtil() {}

    /**
     * Hurts {@code victim} ignoring invulnerability frames. A burst weapon lands several hits in one
     * tick and every one has to count, which is upstream's {@code ignoreIFrame} behaviour.
     */
    public static void attackEntityFromIgnoreIFrame(Entity victim, DamageSource source, float amount) {
        if (!(victim.level() instanceof ServerLevel level)) return;
        victim.invulnerableTime = 0;
        dev.golem.core.world.Compat.hurt(victim, level, source, amount);
    }
}
