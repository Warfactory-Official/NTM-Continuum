package com.hbm.items.weapon.sedna;

import com.hbm.Constants;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.lib.ModDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The two standard impact behaviours every ordinary round uses, ported from the lambdas at the top of
 * 1.12.2's {@code BulletConfig}. They live in their own class because upstream's versions are large
 * inline lambdas and because both need real helper methods.
 *
 * <p>Deferred with their subsystems, all skipped rather than approximated: the {@code red_barrel} and
 * {@code BlockDetonatable} shoot triggers and the {@code deco_crt} screen-cracking state swap (none of
 * those blocks are ported), and {@code ConfettiUtil.decideConfetti} on a kill (the particle system is
 * not ported).
 */
public final class BulletImpact {

    /**
     * Glass a bullet punches straight through. Upstream tests {@code material == GLASS} plus an
     * explosion resistance under 0.6 — materials are gone, so the set is a tag and the resistance guard
     * is kept, which is what actually excludes reinforced glass.
     */
    public static final TagKey<Block> SHATTERS_WHEN_SHOT =
            TagKey.create(Registries.BLOCK, dev.golem.core.Ids.of(Constants.MOD_ID, "shatters_when_shot"));

    private static final float SHATTER_RESISTANCE_LIMIT = 0.6F;

    private BulletImpact() {}

    /**
     * Block impact: shatter glass and carry on, bounce off shallow angles, otherwise stop dead.
     *
     * <p>The angle test is upstream's: the angle between the round's heading and the face normal,
     * measured away from perpendicular, must be within the round's {@code ricochetAngle} for it to skip
     * off. Anything steeper buries the round in the block.
     */
    public static void standardRicochet(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;

        Level level = bullet.level();
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(SHATTERS_WHEN_SHOT)
                && state.getBlock().getExplosionResistance() < SHATTER_RESISTANCE_LIMIT) {
            level.destroyBlock(pos, false);
            bullet.setPos(hit.getLocation());
            return;
        }

        Direction face = blockHit.getDirection();
        Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
        Vec3 heading = bullet.getDeltaMovement().normalize();
        double angle = Math.abs(crossAngle(heading, normal) - 90D);

        if (angle > bullet.config.ricochetAngle) {
            bullet.setPos(hit.getLocation());
            bullet.discard();
            return;
        }

        bullet.ricochets++;
        if (bullet.ricochets > bullet.config.maxRicochetCount) {
            bullet.setPos(hit.getLocation());
            bullet.discard();
            return;
        }

        Vec3 motion = bullet.getDeltaMovement();
        bullet.setDeltaMovement(switch (face.getAxis()) {
            case X -> new Vec3(-motion.x, motion.y, motion.z);
            case Y -> new Vec3(motion.x, -motion.y, motion.z);
            case Z -> new Vec3(motion.x, motion.y, -motion.z);
        });
        level.playSound(null, bullet.getX(), bullet.getY(), bullet.getZ(),
                SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.BLOCKS, 0.25F, 1.0F);
        bullet.setPos(hit.getLocation());
    }

    /**
     * Entity impact: damage, optional headshot multiplier, then either stop or keep going with the
     * damage spent on that victim deducted.
     */
    public static void standardEntityHit(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (!(hit instanceof EntityHitResult entityHit) || hit.getType() != HitResult.Type.ENTITY) return;
        if (!(bullet.level() instanceof ServerLevel level)) return;

        Entity victim = entityHit.getEntity();
        BulletConfig config = bullet.config;

        if (victim == bullet.getThrower() && bullet.tickCount < config.selfDamageDelay) return;
        if (victim instanceof LivingEntity living && living.getHealth() <= 0F) return;

        if (!(victim instanceof LivingEntity living)) {
            hurt(level, victim, source(level, config.dmgClass.type(), bullet), bullet.damage);
            return;
        }

        float intendedDamage = bullet.damage;
        if (config.headshotMult > 1F) {
            // "Head" is the band above eye level, mirrored to the same thickness above the eyes as
            // below the top of the box — upstream's own approximation of a head hitbox.
            double head = living.getBbHeight() - living.getEyeHeight();
            if (living.isAlive() && hit.getLocation().y > living.getY() + living.getBbHeight() - head * 2D) {
                intendedDamage *= config.headshotMult;
            }
        }

        float previousHealth = living.getHealth();
        attack(level, living, bullet, intendedDamage);
        float dealt = Math.max(previousHealth - living.getHealth(), 0F);

        if (config.damageFalloffByPen) bullet.damage -= dealt * 0.5F;
        if (!config.doesPenetrate || bullet.damage < 0) {
            bullet.setPos(hit.getLocation());
            bullet.discard();
        }
    }

    /**
     * Applies one bullet's damage, honouring its armour piercing and knockback multiplier.
     *
     * <p>Armour piercing is delivered by splitting the hit: {@code armorPiercingPercent} of the damage
     * goes through {@code hbm:bullet_piercing}, which carries the vanilla {@code bypasses_armor} tag,
     * and the rest through the round's own damage class. With no damage-resistance layer in this port
     * that split <em>is</em> what "this fraction ignores armour" means. Upstream instead configures its
     * {@code DamageResistanceHandler}; when that system lands, this is the seam to revisit.
     *
     * <p>{@code armorThresholdNegation} has no counterpart at all — vanilla has no damage threshold —
     * so it is carried on the config and ignored here.
     */
    private static void attack(ServerLevel level, LivingEntity victim, EntityBulletBaseMK4 bullet, float amount) {
        BulletConfig config = bullet.config;
        Vec3 before = victim.getDeltaMovement();

        float piercing = amount * config.armorPiercingPercent;
        float normal = amount - piercing;

        if (normal > 0F) {
            hurt(level, victim, source(level, config.dmgClass.type(), bullet), normal);
        }
        if (piercing > 0F) {
            hurt(level, victim, source(level, ModDamageSources.BULLET_PIERCING, bullet), piercing);
        }

        // Scale whatever knockback the hits produced rather than applying our own: that keeps knockback
        // resistance, shields and enchantments in play, which reimplementing it would quietly discard.
        Vec3 knockback = victim.getDeltaMovement().subtract(before);
        victim.setDeltaMovement(before.add(knockback.scale(config.knockbackMult)));
        victim.hurtMarked = true;
    }

    /**
     * Damages {@code victim} ignoring invulnerability frames. A burst can land several rounds in one
     * tick and every one of them has to count, which is upstream's {@code ignoreIFrame} behaviour.
     */
    private static void hurt(ServerLevel level, Entity victim, DamageSource source, float amount) {
        victim.invulnerableTime = 0;
        dev.golem.core.world.Compat.hurt(victim, level, source, amount);
    }

    private static DamageSource source(Level level, ResourceKey<DamageType> type, EntityBulletBaseMK4 bullet) {
        return ModDamageSources.bullet(level, type, bullet, bullet.getThrower());
    }

    /** The angle between two vectors, in degrees. */
    private static double crossAngle(Vec3 a, Vec3 b) {
        double lengths = a.length() * b.length();
        if (lengths == 0D) return 0D;
        return Math.toDegrees(Math.acos(Math.max(-1D, Math.min(1D, a.dot(b) / lengths))));
    }
}
