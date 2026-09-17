package com.hbm.entity.mob.ai;

import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * 1.7 {@code mob/ai/EntityAINearestAttackableTargetNT}: its {@code resetTask} re-asserts the last target
 * instead of clearing it, so aggro survives the goal stopping. 26.2's {@code TargetGoal.stop} nulls it.
 */
public class EntityAINearestAttackableTargetNT<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {

    /** The filter is a plain predicate; Golem spells it in whichever type this version's goal takes. */
    public EntityAINearestAttackableTargetNT(Mob mob, Class<T> targetType, int randomInterval, boolean mustSee,
                                             boolean mustReach, java.util.function.@Nullable Predicate<LivingEntity> selector) {
        super(mob, targetType, randomInterval, mustSee, mustReach, dev.golem.core.world.Targeting.selector(selector));
    }

    @Override
    public void stop() {
        this.mob.setTarget(this.target);
    }
}
