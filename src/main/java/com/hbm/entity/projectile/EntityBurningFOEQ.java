package com.hbm.entity.projectile;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatThrowableProjectile;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeGeneric;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// 26.2: 1.7's ignoreFrustumCheck and full-bright overrides are renderer concerns - RenderFOEQ's job.
public class EntityBurningFOEQ extends CompatThrowableProjectile {

    // 26.2: Entity.setXRot clamps to +-90; 1.7:64 writes rotationPitch raw and a steep re-entry needs -180..-90.
    public float renderPitch;
    public float renderPitchO;

    public EntityBurningFOEQ(EntityType<? extends EntityBurningFOEQ> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    @Override
    public void tick() {

        this.renderPitchO = this.renderPitch;
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.xOld = this.xo;
        this.yOld = this.yo;
        this.zOld = this.zo;

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        // 1.7: gravity after the step, capped at a terminal fall of -4
        if (motion.y > -4D) {
            this.setDeltaMovement(motion.x, motion.y - 0.1D, motion.z);
        }

        this.rotation();

        // 1.7: (int) casts kept - truncation, not floor
        BlockPos pos = new BlockPos((int) getX(), (int) getY(), (int) getZ());

        if (!level().getBlockState(pos).isAir()) {
            if (!level().isClientSide()) {
                for (int i = 0; i < 25; i++)
                    ExplosionLarge.explode(level(), getX() + 0.5F + random.nextGaussian() * 5,
                            getY() + 0.5F + random.nextGaussian() * 5, getZ() + 0.5F + random.nextGaussian() * 5,
                            10.0F, random.nextBoolean(), false, false);
                ExplosionNukeGeneric.transformWaste(level(), pos, 35); // 1.7 waste(): this port's own terrain sweep
            }
            this.discard();
        }
    }

    public void rotation() {
        Vec3 motion = this.getDeltaMovement();
        float f2 = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));

        this.renderPitch = (float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90F;

        for (; this.renderPitch - this.renderPitchO < -180.0F; this.renderPitchO -= 360.0F) ;
        while (this.renderPitch - this.renderPitchO >= 180.0F) this.renderPitchO += 360.0F;

        while (this.getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (this.getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
    }

    // 1.7: isInRangeToRenderDist(distance < 100000); parameter was squared distance
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 100000;
    }
}
