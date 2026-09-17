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
     * This port has no {@code NtmDamageContext} knockback-multiplier / early-cancel duck on LivingEntity, so
     * {@code knockbackMultiplier} and {@code allowSpecialCancel} are accepted for call-site fidelity and not
     * applied (documented gap in entity-port-manifest.md).
     */
    public static boolean attackEntityFromNT(net.minecraft.world.entity.LivingEntity living, DamageSource source,
                                             float amount, boolean ignoreIFrame, boolean allowSpecialCancel,
                                             double knockbackMultiplier, float pierceDT, float pierce) {
        if (!(living.level() instanceof ServerLevel level)) return false;
        if (ignoreIFrame) living.invulnerableTime = 0;
        return DamageResistanceHandler.hurt(level, living, source, amount, pierceDT, pierce);
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
