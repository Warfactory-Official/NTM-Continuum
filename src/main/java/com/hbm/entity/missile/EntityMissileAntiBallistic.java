package com.hbm.entity.missile;

import com.hbm.entity.logic.EntityChunkLoader;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.explosion.ExplosionLarge;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The anti-ballistic missile — 1.12.2's {@code EntityMissileAntiBallistic}, the only missile that
 * steers.
 *
 * <p>It launches straight up for ten ticks (so it clears the pad), then hunts: every tick without a
 * target it re-scans for the nearest {@link EntityMissileBaseNT} within radar range, and with one it
 * aims at a <em>predicted</em> position — the target's own last-tick displacement extrapolated over the
 * closing time, which is what lets a 1.5-blocks-per-tick interceptor meet a missile moving four times
 * that fast.
 *
 * <p>The kill is a proximity burst, not a collision: inside 15 blocks it takes 51 health off every
 * missile in a 30-block cube, which is one more than a missile's 50 and so guarantees the kill even
 * against several at once.
 *
 * <p>Stealth missiles are skipped by the scan — that is the coating's whole purpose.
 *
 * <p>Radius 1: unlike a ballistic missile, an ABM turns, so it claims the 3×3 around itself rather than
 * just the chunk it is over.
 *
 * <p>Deferred: the radar interfaces, the {@code particleControl} exhaust trail, and
 * {@code ExplosionLarge.spawnShock}'s lock-on ring (its particle family is unported).
 */
public class EntityMissileAntiBallistic extends CompatEntity implements IChunkLoader, IMissilePitch {

    /** See {@link IMissilePitch}: the interceptor dives, so its pitch runs past −90 too. */
    public static final net.minecraft.network.syncher.EntityDataAccessor<Float> PITCH =
            net.minecraft.network.syncher.SynchedEntityData.defineId(EntityMissileAntiBallistic.class,
                    net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    private float pitchO;
    private float pitchNow;

    /** The base step length. {@code velocity} scales it, ramping 0 → 6 at 0.1 per tick. */
    private static final double BASE_SPEED = 1.5D;
    /** Upstream reads this off the radar block; the radar is unported, so its value is inlined. */
    private static final int RADAR_RANGE = 1000;

    public @Nullable Entity tracking;
    public double velocity;
    private int activationTimer;

    public @Nullable LivingEntity thrower;

    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 1);

    public EntityMissileAntiBallistic(EntityType<?> type, Level world) {
        super(type, world);
        this.setDeltaMovement(0, BASE_SPEED, 0);
    }

    public EntityMissileAntiBallistic(Level world) {
        this(ModMissileEntities.MISSILE_ANTI_BALLISTIC.get(), world);
    }

    @Override
    protected void defineData(dev.golem.core.data.SynchedData data) {
        super.defineData(data);
        data.define(PITCH, 0F);
    }

    @Override
    public float getMissilePitch(float partialTick) {
        return net.minecraft.util.Mth.lerp(partialTick, this.pitchO, this.pitchNow);
    }

    @Override
    public void tick() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        if (level().isClientSide()) {
            this.pitchO = this.pitchNow;
            this.pitchNow = this.entityData.get(PITCH);
            return;
        }

        chunkLoader.tick();

        if (velocity < 6) velocity += 0.1;

        if (activationTimer < 10) {
            activationTimer++;
            setDeltaMovement(getDeltaMovement().x, BASE_SPEED, getDeltaMovement().z);
        } else {
            Entity prevTracking = this.tracking;

            if (this.tracking == null || !this.tracking.isAlive()) this.targetMissile();

            if (prevTracking == null && this.tracking != null) {
                ExplosionLarge.spawnShock(level(), getX(), getY(), getZ(), 24, 3F);
            }

            if (this.tracking != null) {
                double distance = Math.sqrt(this.tracking.distanceToSqr(getX(), getY(), getZ()));

                if (distance < 15) {
                    // 51 damage: one more than a missile's 50 health, applied to everything in the
                    // burst radius, so a salvo flying in formation goes down together.
                    List<Entity> caught = level().getEntities(this,
                            new AABB(getX() - 15, getY() - 15, getZ() - 15, getX() + 15, getY() + 15, getZ() + 15));
                    for (Entity entity : caught) {
                        if (entity instanceof EntityMissileBaseNT target) target.health -= 51;
                    }

                    discard();
                    ExplosionLarge.explode(level(), thrower, getX(), getY(), getZ(), 20F, true, false, false);
                    return;
                }

                this.aimAtTarget();
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

    /**
     * Nearest live missile within radar range, ignoring stealth airframes.
     *
     * <p>Upstream never updates {@code dist} inside the loop, so this actually picks the <em>last</em>
     * candidate under 1000 blocks rather than the nearest. Kept as-is: the ABM's own 15-block proximity
     * burst makes the choice matter far less than it looks, and "fix" here would change interception
     * behaviour that players have tuned batteries around.
     */
    private void targetMissile() {
        Entity closest = null;
        double dist = 1_000;

        AABB box = new AABB(getX() - RADAR_RANGE, getY(), getZ() - RADAR_RANGE,
                getX() + RADAR_RANGE, getY() + RADAR_RANGE, getZ() + RADAR_RANGE);

        for (Entity e : level().getEntities(this, box)) {
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

        if (delta.length() < 10 && activationTimer >= 40) {
            discard();
            ExplosionLarge.explode(level(), thrower, getX(), getY(), getZ(), 15F, true, false, false);
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
            if (this.activationTimer >= 10) {
                discard();
                ExplosionLarge.explode(level(), thrower, getX(), getY(), getZ(), 20F, true, false, false);
            }
            return;
        }

        setPos(to);
    }

    public void setThrower(@Nullable LivingEntity thrower) {
        this.thrower = thrower;
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
    }

    @Override
    protected void saveData(DataOut output) {
        output.putDouble("veloc", this.velocity);
    }
}
