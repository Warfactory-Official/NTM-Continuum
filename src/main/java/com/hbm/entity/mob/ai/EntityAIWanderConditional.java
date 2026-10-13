package com.hbm.entity.mob.ai;

import dev.golem.core.world.Navigations;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Predicate;

public class EntityAIWanderConditional extends Goal {

    private final PathfinderMob creature;
    private final double speed;
    private final Predicate<PathfinderMob> condition;
    private double xPosition;
    private double yPosition;
    private double zPosition;

    public EntityAIWanderConditional(PathfinderMob creature, double speed, Predicate<PathfinderMob> condition) {
        this.creature = creature;
        this.speed = speed;
        this.condition = condition;
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!condition.test(creature)) return false;
        if (creature.getNoActionTime() >= 100) return false;
        if (creature.getRandom().nextInt(120) != 0) return false;

        Vec3 target = DefaultRandomPos.getPos(creature, 10, 7);
        if (target == null) return false;

        xPosition = target.x;
        yPosition = target.y;
        zPosition = target.z;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !creature.getNavigation().isDone() && condition.test(creature);
    }

    @Override
    public void start() {
        Navigations.navigateTo(creature.getNavigation(), xPosition, yPosition, zPosition, speed);
    }
}
