package com.hbm.entity.missile;

import com.hbm.client.ClientEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1.7 {@code EntityRocketLambda}, the Lambda-98: a single-satellite rocket with a broad flame and contrail puffs. */
public class EntityRocketLambda extends EntityRocketBase {

    public EntityRocketLambda(EntityType<? extends EntityRocketLambda> type, Level level) {
        super(type, level, 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) spawnExhaust(getX(), getY() + 1, getZ());
    }

    // 1.7 effectNT "exhaust"/"lambda", then every ten ticks a "missileContrail" puff thrown out on one of eight headings
    private void spawnExhaust(double x, double y, double z) {
        ClientEffects.spawnRocketFlame(level(), x, y, z, 1.5F, 0.0D, -1D + random.nextDouble() * 0.25D, 0.0D,
                300 + random.nextInt(50));

        if (this.tickCount % 10 == 0) {
            Vec3 vec = new Vec3(1, 0, 0).yRot((float) Math.toRadians(45 * this.random.nextInt(8)));
            double j = 0.5;
            ClientEffects.spawnRocketFlame(level(), getX() - vec.x * j, getY() - vec.y * j, getZ() - vec.z * j, 1F,
                    vec.x, getDeltaMovement().y - 0.5, vec.z, 60 + random.nextInt(20));
        }
    }
}
