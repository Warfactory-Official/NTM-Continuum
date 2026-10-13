package com.hbm.items.weapon.sedna.factory;

import com.hbm.lib.ModDamageSources.DamageClass;
import com.hbm.particle.helper.ParticleEffects;
import com.hbm.sound.ModGunSounds;
import java.util.Locale;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** The 1.7 death-only target effects used by Sedna ammunition. */
public final class ConfettiUtil {
    private ConfettiUtil() {}

    /** Chooses the source-authored disintegration, cremation, or gib effect after a confirmed kill. */
    public static void decideConfetti(LivingEntity entity, DamageSource source) {
        if (entity.isAlive()) return;

        // Modern damage types are named bullet_laser/etc.; accept the old message id as well because
        // this is the same source gate after the registry API adaptation.
        if (isBulletClass(source, DamageClass.LASER) || isBulletClass(source, DamageClass.ELECTRIC)) {
            pulverize(entity);
        }
        if (isBulletClass(source, DamageClass.PLASMA)
                || source.is(DamageTypeTags.IS_FIRE)) {
            cremate(entity);
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            gib(entity);
        }
    }

    public static void pulverize(LivingEntity entity) {
        int amount = amount(entity);
        ParticleEffects.ashes(entity.level(), entity, amount, 0.125F);
        ParticleEffects.skeleton(entity.level(), entity, 1F);
        playDisintegrationSound(entity);
    }

    public static void cremate(LivingEntity entity) {
        int amount = amount(entity);
        ParticleEffects.ashes(entity.level(), entity, amount, 0.125F);
        ParticleEffects.skeleton(entity.level(), entity, 0.25F);
        playDisintegrationSound(entity);
    }

    public static void gib(LivingEntity entity) {
        // This is the source's explicit ocelot exception, not a generic animal rule.
        if (entity.getType() == EntityType.OCELOT) return;

        EntityType<?> entityType = entity.getType();
        int type = entityType == EntityType.SLIME || entityType == EntityType.MAGMA_CUBE
                || entityType == EntityType.CREEPER ? 1
                : entityType == EntityType.IRON_GOLEM || entityType == EntityType.SNOW_GOLEM
                || entityType == EntityType.BLAZE ? 2 : 0;

        ParticleEffects.skeletonGib(entity.level(), entity, 0.25F);
        // The original skeleton receives only the skeleton-piece effect, not a second giblet cloud.
        if (entity.getType() == EntityType.SKELETON) return;

        ParticleEffects.giblets(entity.level(), entity, type);
        Level level = entity.level();
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                SoundSource.HOSTILE, 2F,
                0.95F + entity.getRandom().nextFloat() * 0.2F);
    }

    private static int amount(LivingEntity entity) {
        return Math.max(5, Math.min(50,
                (int) (entity.getBbWidth() * entity.getBbHeight() * entity.getBbWidth() * 25F)));
    }

    private static void playDisintegrationSound(LivingEntity entity) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                ModGunSounds.DISINTEGRATION.get(), SoundSource.PLAYERS, 2F,
                0.9F + entity.getRandom().nextFloat() * 0.2F);
    }

    private static boolean isBulletClass(DamageSource source, DamageClass damageClass) {
        String id = source.getMsgId().toLowerCase(Locale.ROOT);
        String name = damageClass.name().toLowerCase(Locale.ROOT);
        return id.equals(name) || id.equals("bullet_" + name);
    }
}
