package com.hbm.entity.mob.ai;

import dev.golem.core.world.Navigations;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;

/**
 * 1.7 {@code EntityAIMaskmanCasualApproach}: the old melee pursuit with its attack commented out, aimed ten blocks
 * short of the target so the boss circles at gun range. Ported line for line, quirks included:
 * <ul>
 * <li>{@code shouldExecute} only builds a fresh path once its own timer runs out and otherwise answers true with
 * whatever path it built last;</li>
 * <li>{@code failedPathFindingPenalty} grows by 10 on every repath whose end is not within one block of the target -
 * and the end is the approach point, ten blocks short, so it practically never resets: the boss repaths more and
 * more lazily the longer a fight lasts (until the entity is reloaded).</li>
 * </ul>
 */
public class EntityAIMaskmanCasualApproach extends Goal {

    private final PathfinderMob attacker;
    private final double speedTowardsTarget;
    private final boolean longMemory;
    private final @Nullable Class<? extends LivingEntity> classTarget;
    private @Nullable Path entityPathEntity;
    private int pathTimer;
    private double lastX;
    private double lastY;
    private double lastZ;
    private int failedPathFindingPenalty;

    public EntityAIMaskmanCasualApproach(PathfinderMob owner, Class<? extends LivingEntity> target, double speed,
                                         boolean longMemory) {
        this(owner, speed, longMemory, target);
    }

    public EntityAIMaskmanCasualApproach(PathfinderMob owner, double speed, boolean longMemory) {
        this(owner, speed, longMemory, null);
    }

    private EntityAIMaskmanCasualApproach(PathfinderMob owner, double speed, boolean longMemory,
                                          @Nullable Class<? extends LivingEntity> target) {
        this.attacker = owner;
        this.speedTowardsTarget = speed;
        this.longMemory = longMemory;
        this.classTarget = target;
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK)); // 1.7 mutex 3
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.attacker.getTarget();

        if (target == null || !target.isAlive()) return false;
        if (this.classTarget != null && !this.classTarget.isInstance(target)) return false;

        if (--this.pathTimer <= 0) {
            Vec3 pos = getApproachPos(target);
            this.entityPathEntity = this.attacker.getNavigation().createPath(pos.x, pos.y, pos.z, 0);
            this.pathTimer = 4 + this.attacker.getRandom().nextInt(7);
            return this.entityPathEntity != null;
        }

        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.attacker.getTarget();
        if (target == null || !target.isAlive()) return false;
        if (!this.longMemory) return !this.attacker.getNavigation().isDone();
        // 1.7 asked isWithinHomeDistance here. No HBM mob builds this goal with longMemory, and the home check is
        // spelled differently on every node, so the unused branch only keeps the target.
        return true;
    }

    @Override
    public void start() {
        Navigations.navigateTo(this.attacker.getNavigation(), this.entityPathEntity,
                this.speedTowardsTarget);
        this.pathTimer = 0;
    }

    @Override
    public void stop() {
        this.attacker.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.attacker.getTarget();
        if (target == null) return;

        this.attacker.getLookControl().setLookAt(target, 30.0F, 30.0F);
        double d0 = this.attacker.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());

        this.pathTimer--;

        if ((this.longMemory || this.attacker.getSensing().hasLineOfSight(target)) && this.pathTimer <= 0
                && (this.lastX == 0.0D && this.lastY == 0.0D && this.lastZ == 0.0D
                || target.distanceToSqr(this.lastX, this.lastY, this.lastZ) >= 1.0D
                || this.attacker.getRandom().nextFloat() < 0.05F)) {

            this.lastX = target.getX();
            this.lastY = target.getBoundingBox().minY;
            this.lastZ = target.getZ();
            this.pathTimer = this.failedPathFindingPenalty + 4 + this.attacker.getRandom().nextInt(7);

            Path current = this.attacker.getNavigation().getPath();
            Node end = current != null ? current.getEndNode() : null;

            if (end != null && target.distanceToSqr(end.x, end.y, end.z) < 1) {
                this.failedPathFindingPenalty = 0;
            } else {
                this.failedPathFindingPenalty += 10;
            }

            if (d0 > 1024.0D) {
                this.pathTimer += 10;
            } else if (d0 > 256.0D) {
                this.pathTimer += 5;
            }

            Vec3 pos = getApproachPos(target);
            if (!Navigations.navigateTo(this.attacker.getNavigation(), pos.x, pos.y, pos.z,
                    this.speedTowardsTarget)) {
                this.pathTimer += 15;
            }
        }
    }

    private Vec3 getApproachPos(LivingEntity target) {
        Vec3 vec = new Vec3(this.attacker.getX() - target.getX(), this.attacker.getY() - target.getY(),
                this.attacker.getZ() - target.getZ());

        double range = Math.min(vec.length(), 20) - 10;

        vec = vec.normalize();

        double x = this.attacker.getX() + vec.x * range + this.attacker.getRandom().nextGaussian() * 2;
        double y = this.attacker.getY() + vec.y - 5 + this.attacker.getRandom().nextInt(11);
        double z = this.attacker.getZ() + vec.z * range + this.attacker.getRandom().nextGaussian() * 2;

        return new Vec3(x, y, z);
    }
}
