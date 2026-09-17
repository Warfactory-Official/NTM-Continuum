package com.hbm.entity.projectile;

import dev.golem.core.world.Compat;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.sound.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1.7 entity/projectile/EntityBuilding: the .50 BMG equestrian round's falling building prop.
 */
public class EntityBuilding extends CompatEntity {

    private final EntityInterpolation interpolation = new EntityInterpolation(this);

    public EntityBuilding(EntityType<? extends EntityBuilding> type, Level level) {
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
            for (int i = 0; i < 100; i++) {
                FlameCreator.composeEffect(level(),
                        getX() + (random.nextDouble() - 0.5) * 15,
                        getY() + (random.nextDouble() - 0.5) * 15,
                        getZ() + (random.nextDouble() - 0.5) * 15,
                        FlameCreator.META_BALEFIRE);
            }
        }

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        double mY = motion.y - 0.03;
        if (mY < -1.5) mY = -1.5;
        setDeltaMovement(motion.x, mY, motion.z);

        if (!level().getBlockState(blockPosition()).isAir()) {
            level().playSound(null, getX(), getY(), getZ(), ModSounds.OLD_EXPLOSION.get(), SoundSource.AMBIENT,
                    10000.0F, 0.5F + this.random.nextFloat() * 0.1F);
            discard();
            ExplosionLarge.spawnParticles(level(), getX(), getY() + 3, getZ(), 150);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 6);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 5);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 4);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);
            ExplosionLarge.spawnShock(level(), getX(), getY() + 1, getZ(), 24, 3);

            List<Entity> list = level().getEntities(null,
                    new AABB(getX() - 8, getY() - 8, getZ() - 8, getX() + 8, getY() + 8, getZ() + 8));
            if (level() instanceof ServerLevel server) {
                for (Entity e : list) Compat.hurt(e, server, ModDamageSources.source(level(), ModDamageSources.BUILDING), 1000);
            }

            for (int i = 0; i < 250; i++) {
                // 1.7: (1,0,0) rot Z -rand*PI/2, then Y rand*2PI
                double pitch = this.random.nextFloat() * Math.PI / 2;
                double yaw = this.random.nextFloat() * Math.PI * 2;

                EntityRubble rubble = new EntityRubble(level(), getX(), getY() + 3, getZ());
                rubble.setBlockState(Blocks.BRICKS.defaultBlockState());
                rubble.setDeltaMovement(Math.cos(pitch) * Math.cos(yaw), Math.sin(pitch),
                        -Math.cos(pitch) * Math.sin(yaw));
                level().addFreshEntity(rubble);
            }
        }
    }
}
