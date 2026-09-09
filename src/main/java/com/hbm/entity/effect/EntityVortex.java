package com.hbm.entity.effect;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** A normal vortex, shrinking at the legacy fixed rate. */
public class EntityVortex extends EntityBlackHole {
    protected float shrinkRate = 0.0025F;

    public EntityVortex(EntityType<?> type, Level level) {
        super(type, level);
    }

    public EntityVortex(EntityType<?> type, Level level, float size) {
        super(type, level, size);
    }

    public EntityVortex setShrinkRate(float shrinkRate) {
        this.shrinkRate = shrinkRate;
        return this;
    }

    @Override
    public void tick() {
        setSize(getSize() - 0.0025F);
        if (getSize() <= 0F) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        shrinkRate = input.getFloatOr("shrinkRate", 0.0025F);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putFloat("shrinkRate", shrinkRate);
    }
}
