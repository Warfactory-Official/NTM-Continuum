package com.hbm.entity.mob.ai;

import dev.golem.core.world.Targeting;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * 1.7 {@code mob/ai/EntityAINearestAttackableTargetNT}: its {@code resetTask} re-asserts the last target
 * instead of clearing it, so aggro survives the goal stopping. 26.1's {@code TargetGoal.stop} nulls it.
 */
public class EntityAINearestAttackableTargetNT<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    private final double range;

    /** The filter is a plain predicate; Golem spells it in whichever type this version's goal takes. */
    public EntityAINearestAttackableTargetNT(Mob mob, Class<T> targetType, int randomInterval, boolean mustSee,
                                             boolean mustReach, java.util.function.@Nullable Predicate<LivingEntity> selector) {
        this(mob, targetType, randomInterval, mustSee, mustReach, selector, -1D);
    }

    /** {@code range} replaces the follow range attribute as the search distance, as 1.7's last argument did. */
    public EntityAINearestAttackableTargetNT(Mob mob, Class<T> targetType, int randomInterval, boolean mustSee,
                                             boolean mustReach, java.util.function.@Nullable Predicate<LivingEntity> selector,
                                             double range) {
        super(mob, targetType, randomInterval, mustSee, mustReach, Targeting.selector(selector));
        this.range = range;
    }

    @Override
    protected double getFollowDistance() {
        return this.range > 0 ? this.range : super.getFollowDistance();
    }

    @Override
    public void stop() {
        this.mob.setTarget(this.target);
    }
}
