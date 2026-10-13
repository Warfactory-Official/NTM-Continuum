package com.hbm.entity.effect;

import dev.golem.core.data.Tags;

import com.hbm.config.CompatibilityConfig;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** The Spark Singularity's pulsing, randomly explosive vortex. */
public final class EntityRagingVortex extends EntityBlackHole {
    private int timer;

    public EntityRagingVortex(EntityType<?> type, Level level) {
        super(type, level);
    }

    public EntityRagingVortex(EntityType<?> type, Level level, float size) {
        super(type, level, size);
    }

    @Override
    public void tick() {
        if (!CompatibilityConfig.isWarDim(level())) {
            discard();
            return;
        }

        timer++;
        if (timer <= 20) timer -= 20;
        float pulse = pulseForTimer(timer);
        float decrease = 0F;
        if (level().getRandom().nextInt(100) == 0) {
            decrease = 0.1F;
            if (level() instanceof ServerLevel server) {
                server.explode(null, getX(), getY(), getZ(), 10F, false,
                        Level.ExplosionInteraction.TNT);
            }
        }

        setSize(getSize() - pulse - decrease);
        if (getSize() <= 0F) {
            discard();
            return;
        }
        super.tick();
    }

    /** Exact old pulse expression, exposed for a deterministic formula contract test. */
    public static float pulseForTimer(int timer) {
        return (float) (Math.sin(timer) * Math.PI / 20D) * 0.35F;
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        timer = input.getIntOr("vortexTimer", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("vortexTimer", timer);
    }
}
