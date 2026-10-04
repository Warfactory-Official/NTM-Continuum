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
                // 1.7 HbmPotion.performEffect (every second tick, isReady): 1 in 40, never the tainted creeper or crab
                if (!(entity instanceof com.hbm.entity.mob.EntityCreeperTainted)
                        && !(entity instanceof com.hbm.entity.mob.EntityTaintCrab)
                        && level.getRandom().nextInt(40) == 0) {
                    Compat.hurt(entity, level, ModDamageSources.taint(level), amplifier + 1);
                }
                // 1.7 ServerConfig.TAINT_TRAILS: the tainted leave young taint (age 14) in the full block below them
                if (com.hbm.config.ServerConfig.TAINT_TRAILS.get()) {
                    net.minecraft.core.BlockPos below = net.minecraft.core.BlockPos.containing(
                            Math.floor(entity.getX()), Math.floor(entity.getY()) - 1, Math.floor(entity.getZ()));
                    net.minecraft.world.level.block.state.BlockState b = level.getBlockState(below);
                    if (below.getY() + 1 > level.getMinY() + 1 && Compat.isSolidRender(b) && !b.isAir()) { // 1.7 y > 1: the world floor
                        level.setBlock(below, com.hbm.blocks.bomb.ModBombBlocks.taint.get().defaultBlockState()
                                .setValue(com.hbm.blocks.bomb.BlockTaint.TAINTAGE, 14), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
                    }
                }
            }
            // 1.7 HbmPotion.performEffect: level + 1 RAD removed every tick (RadAway I: 1 RAD/t = 20 RAD/s)
            case RADAWAY -> HbmLivingProps.incrementRadiation(entity, -(amplifier + 1));
            case BANG -> {
                // 1.7: 1000 bang damage, health forced to 0, any non-player removed outright; a cow drops cheese
                // (10 for a calf, 3 for an adult) on its way out.
                Compat.hurt(entity, level, ModDamageSources.bang(level), 1000F);
                entity.setHealth(0.0F);
                level.playSound(null, entity.blockPosition(), ModSounds.LASER_BANG.get(),
                        SoundSource.AMBIENT, 100.0F, 1.0F);
                ExplosionLarge.spawnParticles(level, entity.getX(), entity.getY(), entity.getZ(), 10);
                if (entity instanceof net.minecraft.world.entity.animal.cow.Cow cow) {
                    // 1.7 entityDropItem(stack, 1.0F): one block up, the default pickup delay
                    net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(level,
                            cow.getX(), cow.getY() + 1.0D, cow.getZ(), new net.minecraft.world.item.ItemStack(
                            com.hbm.items.ModFoodItems.cheese.get(), cow.isBaby() ? 10 : 3));
                    drop.setDefaultPickUpDelay();
                    level.addFreshEntity(drop);
                }
                if (!(entity instanceof Player)) entity.discard();
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
            // 1.7: setFire(1) every tick, whatever the level
            case PHOSPHORUS -> Effects.ignite(entity, 1);
            // 1.7 potionsickness has no tick action at all (isReady false): it only blocks pills and syringes.
            case POTION_SICKNESS -> { }
        }
        return true;
    }

    @Override
    protected boolean shouldApplyTick(int duration, int amplifier) {
        return switch (kind) {
            case TAINT -> duration % 2 == 0;
            case POTION_SICKNESS -> false;
            case RADAWAY, TELEKINESIS, PHOSPHORUS -> true;
            case BANG -> duration <= 10;
            case LEAD -> duration % 60 == 0;
        };
    }
}
