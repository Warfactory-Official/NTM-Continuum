package com.hbm.entity.projectile.rocketbehavior;

import com.hbm.entity.projectile.EntityArtilleryRocket;
import net.minecraft.world.phys.Vec3;

/**
 * Flies an arc: the turn rate is capped by how many steps are left to the target, so a rocket with a
 * long way to go corrects gently and one about to arrive is allowed to snap. The velocity is rebuilt
 * from the speed and the adjusted angles rather than blended, which is what keeps the speed constant.
 */
public class RocketSteeringBallisticArc implements IRocketSteeringBehavior {

    private static final double TURN_SPEED = 45D;

    @Override
    public void adjustCourse(EntityArtilleryRocket rocket, double speed, double maxTurn) {
        Vec3 motion = rocket.getDeltaMovement();
        Vec3 direction = motion.normalize();
        double horizontalMomentum = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        if (horizontalMomentum == 0D) return;

        Vec3 targetPos = rocket.getLastTarget();
        if (targetPos == null) return;

        double deltaX = targetPos.x - rocket.getX();
        double deltaZ = targetPos.z - rocket.getZ();
        double stepsRequired = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) / horizontalMomentum;
        Vec3 target = new Vec3(deltaX, targetPos.y - rocket.getY(), deltaZ).normalize();

        double rocketYaw = yaw(direction);
        double rocketPitch = pitch(direction);
        double turnSpeed = stepsRequired <= 1D ? 180D : Math.min(maxTurn, TURN_SPEED / stepsRequired);

        double deltaYaw = ((yaw(target) - rocketYaw) + 180D) % 360D - 180D;
        double deltaPitch = ((pitch(target) - rocketPitch) + 180D) % 360D - 180D;
        double turnYaw = Math.min(Math.abs(deltaYaw), turnSpeed) * Math.signum(deltaYaw);
        double turnPitch = Math.min(Math.abs(deltaPitch), turnSpeed) * Math.signum(deltaPitch);

        rocket.setDeltaMovement(new Vec3(speed, 0D, 0D)
                .zRot((float) -Math.toRadians(rocketPitch + turnPitch))
                .yRot((float) Math.toRadians(rocketYaw + turnYaw + 90D)));
    }

    private static double yaw(Vec3 vec) {
        return Math.toDegrees(Math.atan(vec.x / vec.z)) + (vec.z >= 0D ? 180D : 0D);
    }

    private static double pitch(Vec3 vec) {
        return Math.toDegrees(Math.atan(vec.y / Math.sqrt(vec.x * vec.x + vec.z * vec.z)));
    }
}
