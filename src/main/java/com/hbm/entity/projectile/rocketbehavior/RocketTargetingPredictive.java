package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.EntityArtilleryRocket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Leads the target: keeps the last second of its velocity and aims at where the average carries it by
 * the time the rocket arrives. Inside one tick of impact the lead is dropped and the current position
 * is used, so the last correction cannot overshoot.
 */
public class RocketTargetingPredictive implements IRocketTargetingBehavior {

    private static final int BUFFER = 20;

    private final double[][] targetMotion = new double[BUFFER][3];

    @Override
    public void recalculateTargetPosition(EntityArtilleryRocket rocket, Entity target) {
        Vec3 delta = new Vec3(target.getX() - rocket.getX(), target.getY() - rocket.getY(),
                target.getZ() - rocket.getZ());
        double eta = delta.length() - rocket.getDeltaMovement().length();

        double motionX = target.getDeltaMovement().x;
        double motionY = target.getDeltaMovement().y;
        double motionZ = target.getDeltaMovement().z;

        for (int i = 1; i < BUFFER; i++) {
            targetMotion[i - 1] = targetMotion[i];
            motionX += targetMotion[i][0];
            motionY += targetMotion[i][1];
            motionZ += targetMotion[i][2];
        }

        targetMotion[BUFFER - 1] = new double[]{target.getDeltaMovement().x, target.getDeltaMovement().y,
                target.getDeltaMovement().z};

        if (eta <= 1D) {
            rocket.setTarget(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            return;
        }

        rocket.setTarget(target.getX() + motionX / BUFFER * eta,
                target.getY() + target.getBbHeight() * 0.5D + motionY / BUFFER * eta,
                target.getZ() + motionZ / BUFFER * eta);
    }
}
