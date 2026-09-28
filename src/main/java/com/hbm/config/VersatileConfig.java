package com.hbm.config;

import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;
import net.minecraft.world.entity.LivingEntity;

/** 1.7 {@code VersatileConfig}: the config-dependent potion sickness applied by consumables. */
public final class VersatileConfig {

    private VersatileConfig() {}

    /** Applies {@code duration} seconds of potion sickness, or nothing when {@link PotionConfig} turns it off. */
    public static void applyPotionSickness(LivingEntity entity, int duration) {
        if (PotionConfig.potionSickness == 0) return;
        if (PotionConfig.potionSickness == 2) duration *= 12;
        Effects.addRegistered(entity, ModEffects.POTION_SICKNESS.get(), duration * 20, 0);
    }

    public static boolean hasPotionSickness(LivingEntity entity) {
        return Effects.hasRegistered(entity, ModEffects.POTION_SICKNESS.get());
    }
}
