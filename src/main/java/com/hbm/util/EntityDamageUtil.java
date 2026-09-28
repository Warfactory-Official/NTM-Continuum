package com.hbm.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import dev.golem.core.world.Compat;

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


    /**
     * ntm-next {@code attackEntityFromNT}: one server-side hurt call carrying threshold/resistance piercing.
     * This port has no {@code NtmDamageContext} knockback-multiplier / early-cancel duck on LivingEntity, so in the
     * default mode {@code knockbackMultiplier} and {@code allowSpecialCancel} are accepted for call-site fidelity and
     * not applied (documented gap in entity-port-manifest.md). With {@code /ntmserver DAMAGE_COMPATIBILITY_MODE} on
     * it is 1.7's "super compatibility" path, which does apply the multiplier.
     */
    public static boolean attackEntityFromNT(net.minecraft.world.entity.LivingEntity living, DamageSource source,
                                             float amount, boolean ignoreIFrame, boolean allowSpecialCancel,
                                             double knockbackMultiplier, float pierceDT, float pierce) {
        if (!(living.level() instanceof ServerLevel level)) return false;
        if (ignoreIFrame) living.invulnerableTime = 0;
        if (!com.hbm.config.ServerConfig.DAMAGE_COMPATIBILITY_MODE.get()) {
            return DamageResistanceHandler.hurt(level, living, source, amount, pierceDT, pierce);
        }
        // 1.7 /ntmserver DAMAGE_COMPATIBILITY_MODE: "attackEntitySuperCompatibility" - vanilla's own hurt, then the
        // velocity it had put back and HBM's knockback (scaled by knockbackMultiplier) instead of vanilla's.
        // 1.7 odd thing kept: its velocity cache read motionX three times, so Y and Z come back as the old X.
        if (ignoreIFrame) living.hurtTime = 0;
        net.minecraft.world.phys.Vec3 motion = living.getDeltaMovement();
        boolean ret = DamageResistanceHandler.hurt(level, living, source, amount, pierceDT, pierce);
        living.setDeltaMovement(motion.x, motion.x, motion.x);
        Entity entity = source.getEntity();
        if (entity != null) {
            double deltaX = entity.getX() - living.getX();
            double deltaZ;
            for (deltaZ = entity.getZ() - living.getZ(); deltaX * deltaX + deltaZ * deltaZ < 1.0E-4D;
                 deltaZ = (Math.random() - Math.random()) * 0.01D) {
                deltaX = (Math.random() - Math.random()) * 0.01D;
            }
            if (knockbackMultiplier > 0) knockBack(living, deltaX, deltaZ, knockbackMultiplier);
        }
        return ret;
    }

    /** 1.7 {@code EntityDamageUtil.knockBack}: vanilla's 1.7 knockback with the strength scaled by {@code multiplier}. */
    public static void knockBack(net.minecraft.world.entity.LivingEntity living, double motionX, double motionZ, double multiplier) {
        if (living.getRandom().nextDouble() >= living.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)) {
            living.hurtMarked = true; // 1.7 isAirBorne: send the new velocity
            double horizontal = Math.sqrt(motionX * motionX + motionZ * motionZ);
            double magnitude = 0.4D * multiplier;
            net.minecraft.world.phys.Vec3 m = living.getDeltaMovement();
            double mx = m.x / 2.0D - motionX / horizontal * magnitude;
            double my = m.y / 2.0D + magnitude;
            double mz = m.z / 2.0D - motionZ / horizontal * magnitude;
            if (my > 0.2D) my = 0.2D * multiplier;
            living.setDeltaMovement(mx, my, mz);
        }
    }

    public static HitResult getMouseOver(Player player, double reach) {
        return getMouseOver(player, reach, 0D);
    }

    /** Source ray semantics: blocks and entities are traced over the complete requested reach. */
    public static HitResult getMouseOver(Player attacker, double reach, double threshold) {
        Vec3 from = attacker.position().add(0D, attacker.getEyeHeight(), 0D);
        Vec3 look = attacker.getViewVector(1F);
        Vec3 end = from.add(look.scale(reach));
        net.minecraft.world.phys.BlockHitResult blockHit = Compat.clip(attacker.level(), from, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE);
        HitResult block = blockHit.getType() == HitResult.Type.MISS ? null : blockHit;
        Entity pointedEntity = null;
        Vec3 hitVec = null;
        double closest = reach;
        AABB search = attacker.getBoundingBox().expandTowards(look.scale(reach)).inflate(1D);

        for (Entity entity : attacker.level().getEntities(attacker, search,
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && (candidate.isPickable() || candidate instanceof net.minecraft.world.entity.LivingEntity))) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius() + threshold);
            Vec3 location = box.clip(from, end).orElse(null);
            if (box.contains(from)) {
                if (0D <= closest) {
                    pointedEntity = entity;
                    hitVec = location == null ? from : location;
                    closest = 0D;
                }
            } else if (location != null) {
                double distance = from.distanceTo(location);
                if (distance < closest || closest == 0D) {
                    // Rider-interaction capability is not exposed consistently across supported eras;
                    // never select the attacker's own vehicle as a gun/drill target.
                    if (entity == attacker.getVehicle()) {
                        if (closest == 0D) {
                            pointedEntity = entity;
                            hitVec = location;
                        }
                    } else {
                        pointedEntity = entity;
                        hitVec = location;
                        closest = distance;
                    }
                }
            }
        }

        if (pointedEntity != null && (closest <= reach || block == null)) {
            return new EntityHitResult(pointedEntity, hitVec);
        }
        return block;
    }
}
