package com.hbm.entity.projectile;

import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.7 entity/projectile/EntityDuchessGambit: the 12ga equestrian round's falling boat prop.
 */
public class EntityDuchessGambit extends CompatEntity {

    private final EntityInterpolation interpolation = new EntityInterpolation(this);

    public EntityDuchessGambit(EntityType<? extends EntityDuchessGambit> type, Level level) {
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
            for (int i = 0; i < 50; i++) {
                FlameCreator.composeEffect(level(),
                        getX() + (random.nextDouble() - 0.5) * 5,
                        getY() + (random.nextDouble() - 0.5) * 7,
                        getZ() + (random.nextDouble() - 0.5) * 20,
                        FlameCreator.META_BALEFIRE);
            }
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        double mY = motion.y - 0.03;
        if (mY < -1.5) mY = -1.5;
        setDeltaMovement(motion.x, mY, motion.z);

        if (!level().getBlockState(blockPosition()).isAir()) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.ALARM_GAMBIT.get(), SoundSource.RECORDS, 10000.0F,
                    1F);
            discard();

            List<Entity> list = level().getEntities(null,
                    new AABB(getX() - 5, getY() - 2, getZ() - 9, getX() + 5, getY() + 2, getZ() + 9));
            if (level() instanceof ServerLevel server) {
                for (Entity e : list) Compat.hurt(e, server, ModDamageSources.source(level(), ModDamageSources.BOAT), 1000);
            }

            ExplosionLarge.explode(level(), getX(), getY(), getZ() - 6, 2, true, false, false);
            ExplosionLarge.explode(level(), getX(), getY(), getZ() - 3, 2, true, false, false);
            ExplosionLarge.explode(level(), getX(), getY(), getZ(), 2, true, false, false);
            ExplosionLarge.explode(level(), getX(), getY(), getZ() + 3, 2, true, false, false);
            ExplosionLarge.explode(level(), getX(), getY(), getZ() + 6, 2, true, false, false);

            level().setBlockAndUpdate(new BlockPos((int) (getX() - 0.5), (int) (getY() + 0.5), (int) (getZ() - 0.5)),
                    com.hbm.blocks.ModDecoBlocks.boat.get().defaultBlockState());

            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 2.5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 2);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 1.5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 1);
        }
    }
}
