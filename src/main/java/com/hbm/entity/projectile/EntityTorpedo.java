package com.hbm.entity.projectile;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.particle.helper.FlameCreator;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 entity/projectile/EntityTorpedo: the .44 equestrian round's falling torpedo prop.
 */
public class EntityTorpedo extends CompatEntity {

    private final EntityInterpolation interpolation = new EntityInterpolation(this);

    public EntityTorpedo(EntityType<? extends EntityTorpedo> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    @Override
    protected void loadData(DataIn input) {
    }

    @Override
    protected void saveData(DataOut output) {
    }

    @Override
    protected EntityInterpolation interpolation() {
        return this.interpolation;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) return;

        if (this.tickCount == 1) {
            for (int i = 0; i < 15; i++) {
                FlameCreator.composeEffect(level(),
                        getX() + (random.nextDouble() - 0.5) * 2,
                        getY() + (random.nextDouble() - 0.5) * 1,
                        getZ() + (random.nextDouble() - 0.5) * 2,
                        FlameCreator.META_BALEFIRE);
            }
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        double mY = motion.y - 0.04;
        if (mY < -2.5) mY = -2.5;
        setDeltaMovement(motion.x, mY, motion.z);

        if (!level().getBlockState(blockPosition()).isAir()) {
            discard();
            ExplosionCreator.composeEffectStandard(level(), getX(), getY() + 1, getZ());
            ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), 20F);
            vnt.makeStandard();
            vnt.explode();
        }
    }
}
