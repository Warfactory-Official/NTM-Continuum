package com.hbm.entity.missile;

import dev.golem.core.world.Protection;
import java.util.UUID;
import com.hbm.util.Owners;
import com.hbm.api.entity.IRadarDetectableNT;
import dev.golem.core.data.SynchedData;
import dev.golem.core.data.Tags;

import com.hbm.entity.logic.EntityChunkLoader;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import dev.golem.core.world.EntityInterpolation;
import org.jspecify.annotations.Nullable;

/**
 * The anti-ballistic missile, the only missile that steers. It climbs for 40 ticks, then picks a missile off the radar
 * system's list and flies at its predicted position. Stealth missiles are not on offer. The kill is its own blast
 * within 10 blocks, which also takes out anything flying alongside.
 */
public class EntityMissileAntiBallistic extends CompatEntity implements IChunkLoader, IMissilePitch,
        IRadarDetectableNT, Owners.Carrier {

    /** See {@link IMissilePitch}: the interceptor dives, so its pitch runs past −90 too. */
    public static final EntityDataAccessor<Float> PITCH =
            SynchedEntityData.defineId(EntityMissileAntiBallistic.class,
                    EntityDataSerializers.FLOAT);

    private float pitchO;
    private float pitchNow;

    /** The base step length. {@code velocity} scales it, ramping from 0 to 6 at 0.1 per tick. */
    private static final double BASE_SPEED = 1.5D;
    /** Ticks of straight climb before it steers or can be stopped by a block: about 123 blocks. */
    private static final int ACTIVATION_TICKS = 40;

    public @Nullable Entity tracking;
    public double velocity;
    private int activationTimer;

    public @Nullable LivingEntity thrower;
    private @Nullable UUID owner;

    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 1);
    private final EntityInterpolation interpolation = new EntityInterpolation(this);

    public EntityMissileAntiBallistic(EntityType<?> type, Level world) {
        super(type, world);
        this.setDeltaMovement(0, BASE_SPEED, 0);
    }

    public EntityMissileAntiBallistic(Level world) {
        this(ModMissileEntities.MISSILE_ANTI_BALLISTIC.get(), world);
    }

    @Override
    protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(PITCH, 0F);
    }

    @Override
    public float getMissilePitch(float partialTick) {
        return Mth.lerp(partialTick, this.pitchO, this.pitchNow);
    }

    @Override
    protected EntityInterpolation interpolation() {
        return interpolation;
    }

    @Override
    public void tick() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        if (level().isClientSide()) {
            interpolation.interpolate();
            this.pitchO = this.pitchNow;
            this.pitchNow = this.entityData.get(PITCH);
            return;
        }

        chunkLoader.tick();

        if (velocity < 6) velocity += 0.1;

        if (activationTimer < ACTIVATION_TICKS) {
            activationTimer++;
            setDeltaMovement(getDeltaMovement().x, BASE_SPEED, getDeltaMovement().z);
        } else {
            Entity prevTracking = this.tracking;

            if (this.tracking == null || !this.tracking.isAlive()) this.targetMissile();

            if (prevTracking == null && this.tracking != null) {
                ExplosionLarge.spawnShock(level(), getX(), getY(), getZ(), 24, 3F);
            }

            if (this.tracking != null && this.tracking.isAlive()) {
                this.aimAtTarget();
                if (isRemoved()) return;
            } else if (this.tickCount > 600) {
                discard();
                return;
            }
        }

        move();
        if (isRemoved()) return;

        if (getY() > 2000 && (this.tracking == null || !this.tracking.isAlive())) {
            discard();
            return;
        }

        Vec3 motion = getDeltaMovement();
        float horizontal = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
        float pitch = (float) (Math.atan2(motion.y, horizontal) * 180.0D / Math.PI) - 90;
        this.entityData.set(PITCH, pitch);
        this.setXRot(pitch);
    }

    /** 1.7 never updates {@code dist} in the loop, so the last missile under 1000 blocks wins, not the nearest. */
    private void targetMissile() {
        Entity closest = null;
        double dist = 1_000;

        for (Entity e : TileEntityMachineRadarNT.matchingEntities) {
            if (e.level() != level()) continue;
            if (!(e instanceof EntityMissileBaseNT)) continue;
            if (e instanceof EntityMissileStealth) continue;

            double length = new Vec3(e.getX() - getX(), e.getY() - getY(), e.getZ() - getZ()).length();
            if (length < dist) closest = e;
        }

        this.tracking = closest;
    }

    /** Lead the target by its last-tick displacement over the closing time. */
    private void aimAtTarget() {
        Entity target = this.tracking;
        if (target == null) return;

        Vec3 delta = new Vec3(target.getX() - getX(), target.getY() - getY(), target.getZ() - getZ());
        double intercept = delta.length() / (BASE_SPEED * this.velocity);
        Vec3 predicted = new Vec3(
                target.getX() + (target.getX() - target.xOld) * intercept,
                target.getY() + (target.getY() - target.yOld) * intercept,
                target.getZ() + (target.getZ() - target.zOld) * intercept);
        Vec3 heading = new Vec3(predicted.x - getX(), predicted.y - getY(), predicted.z - getZ()).normalize();

        if (delta.length() < 10) {
            discard();
            ExplosionLarge.explode(level(), thrower, Protection.Source.of(this), getX(), getY(), getZ(), 15F, true, false, false);
            return;
        }

        this.setDeltaMovement(heading.scale(BASE_SPEED));
    }

    /** Same swept step as a ballistic missile: no gravity, no drag, blocks end the flight. */
    private void move() {
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement().scale(velocity));

        BlockHitResult hit = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() != HitResult.Type.MISS) {
            if (this.activationTimer >= ACTIVATION_TICKS) {
                discard();
                ExplosionLarge.explode(level(), thrower, Protection.Source.of(this), getX(), getY(), getZ(), 20F, true, false, false);
            }
            return;
        }

        setPos(to);
    }

    public void setThrower(@Nullable LivingEntity thrower) {
        this.thrower = thrower;
        this.owner = Owners.of(thrower);
    }

    @Override
    public @Nullable UUID protectionOwner() {
        return owner;
    }

    @Override
    public void setProtectionOwner(@Nullable UUID owner) {
        this.owner = owner;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    // --- IChunkLoader ---

    @Override public void initChunkLoading() { chunkLoader.initChunkLoading(); }
    @Override public void loadNeighboringChunks(int chunkX, int chunkZ) { chunkLoader.loadNeighboringChunks(chunkX, chunkZ); }
    @Override public void clearChunkLoader() { chunkLoader.clearChunkLoader(); }

    @Override
    public void remove(RemovalReason reason) {
        chunkLoader.clearChunkLoader();
        super.remove(reason);
    }

    @Override
    protected void loadData(DataIn input) {
        chunkLoader.markRestoredFromNbt();
        this.velocity = input.getDoubleOr("veloc", 0);
        owner = Owners.load(input);
    }

    @Override
    protected void saveData(DataOut output) {
        output.putDouble("veloc", this.velocity);
        Owners.save(output, owner);
    }

    // --- 1.7 IRadarDetectableNT ---

    @Override public String getRadarName() { return "radar.target.abm"; }
    @Override public int getBlipLevel() { return TIER_AB; }
    @Override public boolean canBeSeenBy(Object radar) { return true; }
    @Override public boolean paramsApplicable(RadarScanParams params) { return params.scanMissiles; }
    @Override public boolean suppliesRedstone(RadarScanParams params) { return false; }
}
