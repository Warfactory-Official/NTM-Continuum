package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.EntityArtilleryRocket;

/**
 * Modifies a rocket's motion vector so it chases the position set by its
 * {@link IRocketTargetingBehavior}.
 */
public interface IRocketSteeringBehavior {

    void adjustCourse(EntityArtilleryRocket rocket, double speed, double turnSpeed);
}
