package com.hbm.entity.mob.ai;

import dev.golem.core.world.CompatMeleeAttackGoal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.AABB;

/**
 * The melee rule of a 1.7 {@code EntityMob} without AI tasks ({@code EntityMob.attackEntity}): it hits when the target
 * is less than 2 blocks away, centre to centre, and the two boxes overlap in height, whatever the mob's width.
 */
public class EntityAIMobAttack extends CompatMeleeAttackGoal {

    public EntityAIMobAttack(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
    }

    @Override
    protected boolean inReach(LivingEntity target) {
        AABB own = mob.getBoundingBox(), other = target.getBoundingBox();
        return mob.distanceTo(target) < 2.0F && other.maxY > own.minY && other.minY < own.maxY;
    }
}
