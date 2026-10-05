package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.entity.logic.EntityChunkLoader;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.entity.projectile.rocketbehavior.IRocketSteeringBehavior;
import com.hbm.entity.projectile.rocketbehavior.IRocketTargetingBehavior;
import com.hbm.entity.projectile.rocketbehavior.RocketSteeringBallisticArc;
import com.hbm.entity.projectile.rocketbehavior.RocketTargetingPredictive;
import com.hbm.items.weapon.ItemAmmoHIMARS;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The HIMARS rocket: predictive targeting onto a live entity or a fixed column, flown along a ballistic
 * arc, with guidance dropped once a fixed target is within fifteen blocks.
 *
 * <p>A fixed target drops guidance once the rocket is within fifteen blocks, which is also what makes
 * gravity switch on (see {@link #getGravityVelocity}). Upstream leaves a {@code TODO} about entities that
 * unload and reload mid-flight; the chunk loader under the rocket is what keeps that from mattering here.
 */
public class EntityArtilleryRocket extends EntityThrowableNT implements IChunkLoader, IRadarDetectableNT {

    private static final EntityDataAccessor<Integer> ROCKET_TYPE =
            SynchedEntityData.defineId(EntityArtilleryRocket.class, EntityDataSerializers.INT);

    /**
     * Radius 1, not upstream's single chunk: at twenty-five blocks a tick the rocket crosses a border
     * every other tick, and modern tickets are asynchronous — the claim has to already cover the chunk
     * being entered. This is the same reason {@code EntityMissileBaseNT} uses 1.
     */
    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 1);

    public @Nullable Entity targetEntity;
    public @Nullable Vec3 lastTargetPos;

    public @Nullable IRocketTargetingBehavior targeting = new RocketTargetingPredictive();
    public @Nullable IRocketSteeringBehavior steering = new RocketSteeringBallisticArc();

    public EntityArtilleryRocket(EntityType<? extends EntityArtilleryRocket> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(ROCKET_TYPE, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public EntityArtilleryRocket setType(int type) {
        this.entityData.set(ROCKET_TYPE, type);
        return this;
    }

    /** Renamed from upstream's {@code getType()}, which now clashes with {@code Entity.getType()}. */
    public int getTypeIndex() {
        return this.entityData.get(ROCKET_TYPE);
    }

    public ItemAmmoHIMARS.HIMARSRocketType getRocket() {
        return ItemAmmoHIMARS.byIndex(getTypeIndex());
    }

    public EntityArtilleryRocket setTarget(Entity target) {
        this.targetEntity = target;
        return setTarget(target.getX(), target.getY() + target.getBbHeight() * 0.5D, target.getZ());
    }

    public EntityArtilleryRocket setTarget(double x, double y, double z) {
        this.lastTargetPos = new Vec3(x, y, z);
        return this;
    }

    public @Nullable Vec3 getLastTarget() {
        return this.lastTargetPos;
    }

    @Override
    public void tick() {
        if (!level().isClientSide()) chunkLoader.tick();
        super.tick();

        if (isRemoved()) return;

        if (level().isClientSide()) {
            spawnContrail();
            return;
        }

        if (this.targeting == null) this.targeting = new RocketTargetingPredictive();
        if (this.steering == null) this.steering = new RocketSteeringBallisticArc();

        if (this.lastTargetPos != null && this.targetEntity == null) {
            Vec3 aim = this.lastTargetPos;
            Vec3 delta = new Vec3(aim.x - getX(), aim.y - getY(), aim.z - getZ());
            if (delta.length() <= 15D) {
                this.targeting = null;
                this.steering = null;
            }
        }

        if (this.targeting != null && this.targetEntity != null) {
            this.targeting.recalculateTargetPosition(this, this.targetEntity);
        }
        if (this.steering != null) this.steering.adjustCourse(this, 25D, 15D);

        getRocket().onUpdate(this);
    }

    /**
     * The exhaust trail, drawn along the step just taken. Upstream puts the puffs <em>behind</em> the
     * nose — six blocks back and further — and emits nothing at all for a step of one block or less,
     * which is what keeps a rocket at rest from smoking.
     */
    private void spawnContrail() {
        Vec3 back = new Vec3(this.xOld - getX(), this.yOld - getY(), this.zOld - getZ());
        double velocity = back.length();
        if (velocity <= 1D) return;

        Vec3 unit = back.normalize();
        for (int i = 6; i < velocity + 6; i++) {
            CompoundTag data = new CompoundTag();
            data.putFloat("scale", 0.5F);
            data.putDouble("moX", unit.x);
            data.putDouble("moY", unit.y);
            data.putDouble("moZ", unit.z);
            data.putInt("maxAge", 60 + this.random.nextInt(20));
            HbmEffectNT.MissileContrail.summonParticle(level(), getX() + unit.x * i, getY() + unit.y * i,
                    getZ() + unit.z * i, data);
        }
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (!level().isClientSide()) getRocket().onImpact(this, hit);
    }

    @Override
    public void initChunkLoading() {
        chunkLoader.initChunkLoading();
    }

    @Override
    public void loadNeighboringChunks(int chunkX, int chunkZ) {
        chunkLoader.loadNeighboringChunks(chunkX, chunkZ);
    }

    @Override
    public void clearChunkLoader() {
        chunkLoader.clearChunkLoader();
    }

    /** What a warhead calls instead of {@code discard()}: the claim has to go with the rocket. */
    public void killAndClear() {
        discard();
        clearChunkLoader();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkLoader();
        super.remove(reason);
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    /**
     * A rocket under guidance holds its arc by rebuilding the velocity every tick, so gravity would
     * only fight the steering. Once the behaviours are dropped it becomes a falling shell.
     */
    @Override
    public double getGravityVelocity() {
        return this.steering != null ? 0D : 0.01D;
    }

    @Override
    public String getRadarName() {
        return "entity.hbm.radar.artillery";
    }

    @Override
    public int getBlipLevel() {
        return ARTY;
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return true;
    }

    @Override
    public boolean paramsApplicable(RadarScanParams params) {
        return params.scanShells;
    }

    @Override
    public boolean suppliesRedstone(RadarScanParams params) {
        return params.scanShells;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        Vec3 aim = this.lastTargetPos != null ? this.lastTargetPos : position();
        output.putDouble("targetX", aim.x);
        output.putDouble("targetY", aim.y);
        output.putDouble("targetZ", aim.z);
        // by enum name, never the ordinal (History section 19ek-small)
        com.hbm.util.SavedIds.putEnum(output, "type", ItemAmmoHIMARS.byIndex(getTypeIndex()));
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.lastTargetPos = new Vec3(input.getDoubleOr("targetX", 0D), input.getDoubleOr("targetY", 0D),
                input.getDoubleOr("targetZ", 0D));
        this.entityData.set(ROCKET_TYPE, com.hbm.util.SavedIds.getEnum(input, "type", ItemAmmoHIMARS.HIMARSRocketType.values()[0]).ordinal());
        chunkLoader.markRestoredFromNbt();
    }
}
