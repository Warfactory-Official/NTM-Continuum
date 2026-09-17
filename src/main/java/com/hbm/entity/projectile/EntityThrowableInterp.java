package com.hbm.entity.projectile;

import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.level.Level;

/**
 * 26.2: 1.7's hand-rolled tracker lerping is exactly vanilla's EntityInterpolation now, so the body collapses to the override.
 */
public abstract class EntityThrowableInterp extends EntityThrowableNT {

    private final EntityInterpolation interpolation = new EntityInterpolation(this, 3 + approachNum());

    public EntityThrowableInterp(EntityType<? extends EntityThrowableInterp> type, Level level) {
        super(type, level);
    }

    @Override
    protected EntityInterpolation interpolation() {
        return this.interpolation;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide()) {
            super.tick();
        } else {
            // 1.7: interpolate() not auto-ticked for non-living
            this.interpolation.interpolate();
            this.baseTick();
        }
    }

    /**
     * A number added to the basic "3" of the approach progress: larger makes the approach smoother, but lagging more.
     */
    public int approachNum() {
        return 0;
    }
}
