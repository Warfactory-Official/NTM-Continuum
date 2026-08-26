package com.hbm.potion;

import com.hbm.capability.HbmLivingProps;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.ModDamageSources;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatMobEffect;
import dev.golem.core.world.Effects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** The non-radiation effects from the old {@code HbmPotion} registry. */
public final class HbmEffect extends CompatMobEffect {

    public enum Kind {
        TAINT, BANG, LEAD, RADAWAY, TELEKINESIS, PHOSPHORUS, POTION_SICKNESS
    }

    private final Kind kind;

    public HbmEffect(MobEffectCategory category, int color, Kind kind) {
        super(category, color);
        this.kind = kind;
    }

    @Override
    protected boolean applyCompatEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity.level() instanceof ServerLevel level)) return true;

        switch (kind) {
            case TAINT -> {
                // The tainted-creeper exception belongs to the mob wave; ordinary entities retain the
                // old one-in-eighty absolute damage pulse here.
                if (level.getRandom().nextInt(80) == 0) {
                    Compat.hurt(entity, level, ModDamageSources.taint(level), amplifier + 1);
                }
            }
            case RADAWAY -> HbmLivingProps.incrementRadiation(entity, -(amplifier + 1) * 0.05F);
            case BANG -> {
                Compat.hurt(entity, level, ModDamageSources.bang(level), 10_000F * (amplifier + 1));
                if (!(entity instanceof Player)) {
                    entity.die(ModDamageSources.bang(level));
                    entity.setHealth(0);
                }
                level.playSound(null, entity.blockPosition(), ModSounds.LASER_BANG.get(),
                        SoundSource.AMBIENT, 100.0F, 1.0F);
                ExplosionLarge.spawnParticles(level, entity.getX(), entity.getY(), entity.getZ(), 10);
            }
            case LEAD -> Compat.hurt(entity, level, ModDamageSources.lead(level), amplifier + 1);
            case TELEKINESIS -> {
                var effect = Effects.getRegistered(entity, ModEffects.TELEKINESIS.get());
                if (effect != null && effect.getDuration() > 1) {
                    Vec3 motion = entity.getDeltaMovement();
                    entity.setDeltaMovement(motion.x + (entity.getRandom().nextFloat() - 0.5D) * (amplifier + 1) * 0.5D,
                            motion.y + (entity.getRandom().nextFloat() - 0.5D) * (amplifier + 1) * 0.5D,
                            motion.z + (entity.getRandom().nextFloat() - 0.5D) * (amplifier + 1) * 0.5D);
                }
            }
            case PHOSPHORUS -> Effects.ignite(entity, amplifier + 1);
            case POTION_SICKNESS -> {
                if (level.getRandom().nextInt(128) == 0) {
                    Effects.add(entity, net.minecraft.world.effect.MobEffects.NAUSEA, 8 * 20, 0);
                }
            }
        }
        return true;
    }

    @Override
    protected boolean shouldApplyTick(int duration, int amplifier) {
        return switch (kind) {
            case TAINT, POTION_SICKNESS -> duration % 2 == 0;
            case RADAWAY, TELEKINESIS, PHOSPHORUS -> true;
            case BANG -> duration <= 10;
            case LEAD -> duration % 60 == 0;
        };
    }
}
