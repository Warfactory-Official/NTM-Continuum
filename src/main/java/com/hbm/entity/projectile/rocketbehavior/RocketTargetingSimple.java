package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.EntityArtilleryRocket;
import net.minecraft.world.entity.Entity;

/** Aims straight at where the target is now. */
public class RocketTargetingSimple implements IRocketTargetingBehavior {

    @Override
    public void recalculateTargetPosition(EntityArtilleryRocket rocket, Entity target) {
        rocket.setTarget(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }
}
