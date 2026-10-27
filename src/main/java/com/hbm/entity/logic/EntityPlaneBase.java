package com.hbm.entity.logic;

import dev.golem.core.data.Tags;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.util.ChunkUtil;
import com.hbm.client.ClientEffects;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.lib.ModDamageSources;
import com.hbm.particle.helper.ExplosionSmallCreator;
import com.hbm.sound.ModSounds;

import dev.golem.core.hook.Hook;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.Vec3;

/**
 * 1.7 entity/logic/EntityPlaneBase: a straight-flying, shoot-downable aircraft that keeps its own chunk loaded.
 */
public abstract class EntityPlaneBase extends CompatEntity {

    /** A listener on a shot-down plane hitting the ground (ADDON-API-INVENTORY H-73), after 1.7's blast and sound. */
    @FunctionalInterface
    public interface Crash {
        void crashed(EntityPlaneBase plane, double x, double y, double z);
    }

    /** Every listener runs; HBM registers none. NTM: Space adds hbm-space's large explosion effect. */
    public static final Hook<Crash> CRASH = Hook.notifying();


    private static final EntityDataAccessor<Float> DATA_HEALTH =
            SynchedEntityData.defineId(EntityPlaneBase.class, EntityDataSerializers.FLOAT);
    // 1.12.2: 1-chunk sliding window - setChunkForced. NONE = unforced
    private static final long NONE = Long.MIN_VALUE;
    private final EntityInterpolation interpolation = new EntityInterpolation(this);
    public float health = getMaxHealth();
    public int timer = getLifetime();
    // nose unclamped; setXRot pins to +/-90
    public float renderPitch = -90F;
    public float renderPitchO = -90F;
    public float renderYaw;
    public float renderYawO;
    private long forcedChunk = NONE;

    public EntityPlaneBase(EntityType<? extends EntityPlaneBase> type, Level level) {
        super(type, level);
    }

    public float getMaxHealth() {
        return 50F;
    }

    public int getLifetime() {
        return 200;
    }

    @Override
    public boolean isPickable() {
        return this.health > 0; // 1.7: canBeCollidedWith - shot until falling
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true; // 1.7: isInRangeToRenderDist always true
    }

    @Override
    protected void defineData(SynchedData builder) {
        builder.define(DATA_HEALTH, 50F);
    }

    @Override
    protected EntityInterpolation interpolation() {
        return this.interpolation;
    }

    @Override
    public void tick() {
        Vec3 prePos = position();
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) {
            this.health = this.entityData.get(DATA_HEALTH);
            Vec3 step = position().subtract(prePos);
            updateRenderRotation(step);
            // 1.7: death-spiral flames; client spawns, health synced
            if (this.health <= 0) {
                for (int i = 0; i < 10; i++) {
                    ClientEffects.spawnPlaneFlame(level(),
                            getX() + this.random.nextGaussian() * 0.5 - step.x * 2,
                            getY() + this.random.nextGaussian() * 0.5 - step.y * 2,
                            getZ() + this.random.nextGaussian() * 0.5 - step.z * 2);
                }
            }
            return;
        }

        this.entityData.set(DATA_HEALTH, this.health);

        Vec3 motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        rotation();

        if (this.health <= 0) {
            setDeltaMovement(motion.x, motion.y - 0.025, motion.z);

            if (!level().getBlockState(blockPosition()).isAir() || getY() < level().getMinY()) {
                double x = getX(), y = getY(), z = getZ();
                discard();
                new ExplosionVNT(level(), x, y, z, 15F).makeStandard().explode();
                level().playSound(null, x, y, z, ModSounds.ENTITY_PLANE_CRASH.get(), SoundSource.NEUTRAL, 25.0F, 1.0F);
                for (Crash listener : CRASH) listener.crashed(this, x, y, z);
                return;
            }
        } else {
            setDeltaMovement(motion.x, 0, motion.z);
        }

        if (this.tickCount > this.timer) {
            discard();
            return;
        }
        slideForcedChunk();
    }

    // 1.7: rotation() - pitch = atan2(vertical, horizontal) - 90
    protected void rotation() {
        Vec3 motion = getDeltaMovement();
        setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
    }

    private void updateRenderRotation(Vec3 step) {
        this.renderPitchO = this.renderPitch;
        this.renderYawO = this.renderYaw;
        double horiz = Math.sqrt(step.x * step.x + step.z * step.z);
        if (step.lengthSqr() > 1.0e-8) {
            this.renderPitch = (float) (Math.atan2(step.y, horiz) * 180.0D / Math.PI) - 90F;
        }
        if (horiz > 1.0e-3) {
            this.renderYaw = (float) (Math.atan2(step.x, step.z) * 180.0D / Math.PI);
        }
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        if (source.is(ModDamageSources.NUCLEAR_BLAST)) return false;
        if (isInvulnerableToBase(source)) return false;
        if (!isRemoved() && this.health > 0) {
            this.health -= amount;
            if (this.health <= 0) killPlane();
        }
        return true;
    }

    protected void killPlane() {
        ExplosionSmallCreator.composeEffect(level(), getX(), getY(), getZ(), 25, 3.5F, 2F);
        level().playSound(null, getX(), getY(), getZ(), ModSounds.ENTITY_PLANE_SHOT_DOWN.get(), SoundSource.NEUTRAL,
                25.0F, 1.0F);
    }

    @Override
    protected void loadData(DataIn input) {
        this.tickCount = input.getIntOr("ticksExisted", 0);
        this.health = input.getFloatOr("health", getMaxHealth());
        this.entityData.set(DATA_HEALTH, this.health);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("ticksExisted", this.tickCount);
        output.putFloat("health", this.health);
    }

    /**
     * Force-load the entity's current chunk before it is added - a plane spawns ~100 blocks out, usually in unloaded terrain.
     */
    public void forceSpawnChunk() {
        slideForcedChunk();
        if (level() instanceof ServerLevel server) {
            try {
                ChunkUtil.loadEntityTickingChunk(server, forcedChunk);
            } catch (RuntimeException | Error failure) {
                releaseForcedChunk();
                throw failure;
            }
        }
    }

    /** Adds a newly configured plane, releasing its start claim if the level refuses it or addition throws. */
    public final boolean addToLevel() {
        var added = false;
        try {
            added = level().addFreshEntity(this);
            return added;
        } finally {
            if (!added) discard();
        }
    }

    private void slideForcedChunk() {
        if (!(level() instanceof ServerLevel server)) return;
        int cx = getBlockX() >> 4;
        int cz = getBlockZ() >> 4;
        long key = ChunkPos.pack(cx, cz);
        if (key == forcedChunk) return;
        ChunkUtil.claimForced(server, key);
        if (forcedChunk != NONE) ChunkUtil.releaseForced(server, forcedChunk);
        forcedChunk = key;
    }

    private void releaseForcedChunk() {
        if (forcedChunk != NONE && level() instanceof ServerLevel server) {
            ChunkUtil.releaseForced(server, forcedChunk);
        }
        forcedChunk = NONE;
    }

    @Override
    public void remove(RemovalReason reason) {
        releaseForcedChunk();
        super.remove(reason);
    }
}
