package com.hbm.entity.missile;

import dev.golem.core.data.Tags;

import com.hbm.entity.logic.EntityChunkLoader;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A ballistic missile in flight — 1.12.2's {@code EntityMissileBaseNT}.
 *
 * <p><b>The flight model, which is not vanilla projectile physics.</b> A missile is launched straight
 * up at {@code motionY = 2} and never steers: it is aimed once, at launch, by a pair of constants
 * derived from the distance to the target. With {@code d} the horizontal distance,
 * {@code accelXZ = 1/d} and {@code decelY = 2/d}; every tick gravity-substitute {@code decelY·velocity}
 * is taken off the vertical motion, and {@code accelXZ·velocity} is added to the horizontal motion
 * <em>towards</em> the target while climbing and <em>away</em> from it while falling. The result is a
 * symmetric arc that lands on the target regardless of range — a far target simply gets a flatter,
 * longer arc — and it is why the missile carries no guidance at all.
 *
 * <p>{@code velocity} is a per-tick step multiplier that ramps from 0 to 4 over the first minute of
 * flight ({@code ticksExisted/60 · 0.05}, capped at 0.05 per tick), so the whole trajectory is traced
 * at increasing speed. That makes it the missile's {@code motionMult}: the entity is swept from its
 * position to {@code position + motion · velocity} each tick and impacts on the first block in that
 * span, which is what keeps a 4-blocks-per-tick missile from tunnelling through the ground.
 *
 * <p>Missiles are chunkloaders for the obvious reason: an ICBM outruns the player who fired it long
 * before it lands. The modern loading ticket is asynchronous, so the missile holds the 3×3 around its
 * current chunk; that gives the next border chunk time to become resident before the entity crosses.
 *
 * <p><b>One deviation.</b> Upstream ran the flight simulation on both sides and reconciled with its own
 * {@code EntityThrowableInterp} interpolation, because 1.12's tracker only sent a position every few
 * ticks and dropped rotation updates (hence its {@code EntityTrackerEntry.encodedRotationYaw += 100}
 * hack). Here the simulation is server-only and the entity types are registered with
 * {@code updateInterval(1)}, so the client is told where the missile is every tick and vanilla's own
 * interpolation draws the path — no second copy of the physics, and no rotation hack.
 *
 * <p><b>Pitch does not live in {@code xRot}.</b> Upstream's convention is 0° pointing straight up, −90°
 * horizontal and −180° straight down, and from 26.1 {@code Entity.setXRot} clamps to [−90, 90] — so a
 * missile stored in the vanilla field flattens out at horizontal and never noses over there. Below
 * 26.1 the field is unclamped and would hold it, but the value is carried the same way on every
 * target rather than by two different mechanisms: {@link #PITCH} holds it, and the renderer and the
 * contrail read it through {@link #getMissilePitch}.
 *
 * <p><b>Deferred vs 1.12.2:</b> the radar interfaces, which arrive with the radar itself.
 */
public abstract class EntityMissileBaseNT extends CompatEntity implements IChunkLoader, IMissilePitch {

    public int startX;
    public int startZ;
    public int targetX;
    public int targetZ;
    public double velocity;
    public double decelY;
    public double accelXZ;
    public boolean isCluster = false;
    public int health = 50;

    public @Nullable LivingEntity thrower;

    /**
     * The missile's pitch in upstream's convention: 0° straight up, −90° horizontal, −180° straight
     * down. Synchronized rather than derived, because the client does not run the flight simulation
     * and vanilla's {@code xRot} cannot hold a value outside [−90, 90].
     */
    public static final EntityDataAccessor<Float> PITCH =
            SynchedEntityData.defineId(EntityMissileBaseNT.class, EntityDataSerializers.FLOAT);

    /** Client-side interpolation of {@link #PITCH}: last tick's value and this tick's. */
    private float pitchO;
    private float pitchNow;

    /** Client-side: where the missile was when the contrail last ran, for the trail's direction. */
    private double contrailX;
    private double contrailY;
    private double contrailZ;
    private boolean contrailStarted;

    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 1);

    public EntityMissileBaseNT(EntityType<?> type, Level world) {
        super(type, world);
        startX = (int) getX();
        startZ = (int) getZ();
        targetX = (int) getX();
        targetZ = (int) getZ();
    }

    /** The launch constructor. {@code (a, b)} is the target, in world X/Z. */
    public EntityMissileBaseNT(EntityType<?> type, Level world, float x, float y, float z, int a, int b) {
        this(type, world);
        this.setPos(x, y, z);
        startX = (int) x;
        startZ = (int) z;
        targetX = a;
        targetZ = b;
        this.setDeltaMovement(0, 2, 0);

        Vec3 vector = new Vec3(targetX - startX, 0, targetZ - startZ);
        accelXZ = decelY = 1 / vector.length();
        decelY *= 2;
        velocity = 0;

        this.setYRot((float) (Math.atan2(targetX - getX(), targetZ - getZ()) * 180.0D / Math.PI));
    }

    @Override
    protected void defineData(SynchedData data) {
        super.defineData(data);
        data.define(PITCH, 0F);
    }

    /** This missile's pitch for rendering, interpolated across the frame. See {@link #PITCH}. */
    @Override
    public float getMissilePitch(float partialTick) {
        return Mth.lerp(partialTick, this.pitchO, this.pitchNow);
    }

    /** The item this missile came from. Everything a radar wants to say about it is read off that. */
    public abstract ItemStack getMissileItemForInfo();

    public abstract void onMissileImpact(HitResult hit);

    /** The parts scattered when this missile is destroyed in flight. */
    public abstract List<ItemStack> getDebris();

    /** The warhead, dropped one time in ten. */
    public abstract @Nullable ItemStack getDebrisRareDrop();

    /** Cluster missiles split instead of landing; everything else keeps this empty. */
    public void cluster() {}

    public boolean hasPropulsion() {
        return true;
    }

    /** Contrail thickness. Tier 0 and 1 halve it. */
    protected float getContrailScale() {
        return 1F;
    }

    @Override
    public void tick() {
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        if (level().isClientSide()) {
            this.pitchO = this.pitchNow;
            this.pitchNow = this.entityData.get(PITCH);

            // The trail is drawn along the step the missile just took, so it needs the position it
            // held when this ran last — not xo, which vanilla resets to the current position at the
            // top of every client tick. The first tick has no such step and only records one.
            if (this.contrailStarted) this.spawnContrail();
            this.contrailX = getX();
            this.contrailY = getY();
            this.contrailZ = getZ();
            this.contrailStarted = true;
            return;
        }

        chunkLoader.tick();
        move();

        if (isRemoved()) return;

        if (this.health <= 0) { // shot down
            this.killMissile();
            return;
        }

        if (velocity < 4) {
            velocity += Math.max(0, Math.min(this.tickCount / 60D * 0.05D, 0.05));
        }

        {
            Vec3 motion = getDeltaMovement();

            if (hasPropulsion()) {
                double motionY = motion.y - decelY * velocity;

                Vec3 heading = new Vec3(targetX - startX, 0, targetZ - startZ).normalize().scale(accelXZ);

                double motionX = motion.x;
                double motionZ = motion.z;
                // Towards the target on the way up, away from it on the way down: the arc is symmetric
                // about its apex, so the second half undoes exactly the horizontal speed the first half
                // built up and the missile lands on the aim point.
                if (motionY > 0) {
                    motionX += heading.x * velocity;
                    motionZ += heading.z * velocity;
                }
                if (motionY < 0) {
                    motionX -= heading.x * velocity;
                    motionZ -= heading.z * velocity;
                }

                setDeltaMovement(motionX, motionY, motionZ);
            } else {
                double motionY = motion.y;
                if (motionY > -1.5) motionY -= 0.05;
                setDeltaMovement(motion.x * 0.99, motionY, motion.z * 0.99);
            }

            if (getDeltaMovement().y < -1.5 && this.isCluster) {
                cluster();
                discard();
                return;
            }

            this.setYRot((float) (Math.atan2(targetX - getX(), targetZ - getZ()) * 180.0D / Math.PI));
            Vec3 now = getDeltaMovement();
            float horizontal = (float) Math.sqrt(now.x * now.x + now.z * now.z);
            float pitch = (float) (Math.atan2(now.y, horizontal) * 180.0D / Math.PI) - 90;
            this.entityData.set(PITCH, pitch);
            // Kept in step for anything that reads vanilla rotation, but it is not the value that is
            // rendered: from 26.1 setXRot clamps this to −90 on the way down.
            this.setXRot(pitch);
        }
    }

    /**
     * The movement half of upstream's {@code EntityThrowableNT}, reduced to what a missile uses: no
     * gravity, no drag, no entity collision ({@code doesImpactEntities} is false for every missile —
     * they are hit by ABMs through {@code health}, not by flying into things).
     */
    private void move() {
        Vec3 from = position();
        Vec3 to = from.add(getDeltaMovement().scale(velocity));

        BlockHitResult hit = level().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (hit.getType() != HitResult.Type.MISS) {
            this.onMissileImpact(hit);
            discard();
            return;
        }

        setPos(to);
    }

    /** The exhaust trail. Client-side only, once per client tick. */
    protected void spawnContrail() {
        this.spawnContrailWithOffset(0, 0, 0);
    }

    /**
     * One trail, offset from the missile's axis; missiles with several engines call this per nozzle.
     *
     * <p>Puffs are dropped along the step just taken, one per block, but thrown backwards along the
     * missile's <em>own axis</em> rather than its path — which is what makes the trail flare sideways
     * when it turns over at apogee.
     */
    protected void spawnContrailWithOffset(double offsetX, double offsetY, double offsetZ) {
        Vec3 step = new Vec3(this.contrailX - getX(), this.contrailY - getY(), this.contrailZ - getZ());
        double len = step.length();
        Vec3 dir = step.normalize();

        // The body axis: (0,1,0) rolled by the pitch, then yawed onto the missile's heading.
        Vec3 thrust = rotateYaw(rotateRoll(new Vec3(0, 1, 0), contrailPitchRadians()), contrailYawRadians());

        int puffs = (int) Math.max(Math.min(len, 10), 1);
        for (int i = 0; i < puffs; i++) {
            double j = i - len;
            CompoundTag data = new CompoundTag();
            data.putFloat("scale", this.getContrailScale());
            data.putDouble("moX", -thrust.x);
            data.putDouble("moY", -thrust.y);
            data.putDouble("moZ", -thrust.z);
            data.putInt("maxAge", 60 + this.random.nextInt(20));
            HbmEffectNT.MissileContrail.summonParticle(level(),
                    getX() - dir.x * j + offsetX,
                    getY() - dir.y * j + offsetY,
                    getZ() - dir.z * j + offsetZ, data);
        }
    }

    /** @see #spawnContrailWithOffset */
    protected float contrailPitchRadians() {
        return getMissilePitch(1F) * (float) Math.PI / 180F;
    }

    /** The yaw the contrail is built from, in radians — the body axis is 90° off the heading. */
    protected float contrailYawRadians() {
        return (getYRot() + 90F) * (float) Math.PI / 180F;
    }

    /**
     * 1.12's {@code Vec3.rotateAroundY}. These three are copied out rather than expressed with
     * {@code Axis.*.rotation} because 1.12's sign convention is not the same one.
     */
    protected static Vec3 rotateYaw(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(v.x * cos + v.z * sin, v.y, v.z * cos - v.x * sin);
    }

    /** 1.12's {@code Vec3.rotateAroundX}. */
    protected static Vec3 rotatePitch(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(v.x, v.y * cos + v.z * sin, v.z * cos - v.y * sin);
    }

    /** 1.12's {@code Vec3.rotateAroundZ}. */
    protected static Vec3 rotateRoll(Vec3 v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(v.x * cos + v.y * sin, v.y * cos - v.x * sin, v.z);
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        if (this.health > 0 && !level().isClientSide()) {
            health -= amount;
            if (this.health <= 0) this.killMissile();
        }
        return true;
    }

    /** Destroyed in flight: a modest blast, a shower of shrapnel, and the missile's parts. */
    protected void killMissile() {
        if (isRemoved()) return;
        discard();
        ExplosionLarge.explode(level(), thrower, getX(), getY(), getZ(), 5, true, false, true);
        Vec3 motion = getDeltaMovement();
        ExplosionLarge.spawnShrapnelShower(level(), getX(), getY(), getZ(), motion.x, motion.y, motion.z, 15, 0.075);
        ExplosionLarge.spawnMissileDebris(level(), getX(), getY(), getZ(), motion.x, motion.y, motion.z, 0.25,
                getDebris(), getDebrisRareDrop());
    }

    /**
     * The standard warhead: a VNT blast at the given resolution, optionally leaving fire, with the
     * cross entity processor at node distance 7.5 and double entity range.
     */
    public void explodeStandard(float strength, int resolution, boolean fire) {
        ExplosionVNT xnt = new ExplosionVNT(level(), getX(), getY(), getZ(), strength);
        xnt.setBlockAllocator(new BlockAllocatorStandard(resolution));
        xnt.setBlockProcessor(new BlockProcessorStandard().setNoDrop().withBlockEffect(fire ? new BlockMutatorFire() : null));
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(2));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
    }

    public void setThrower(@Nullable LivingEntity thrower) {
        this.thrower = thrower;
    }

    @Override
    public boolean isPickable() {
        return true; // a missile must be shootable, which is the whole point of the ABM
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

    // --- persistence ---

    @Override
    protected void loadData(DataIn input) {
        chunkLoader.markRestoredFromNbt();
        setDeltaMovement(input.getDoubleOr("moX", 0), input.getDoubleOr("moY", 0), input.getDoubleOr("moZ", 0));
        setPos(input.getDoubleOr("poX", getX()), input.getDoubleOr("poY", getY()), input.getDoubleOr("poZ", getZ()));
        decelY = input.getDoubleOr("decel", 0);
        accelXZ = input.getDoubleOr("accel", 0);
        targetX = input.getIntOr("tX", 0);
        targetZ = input.getIntOr("tZ", 0);
        startX = input.getIntOr("sX", 0);
        startZ = input.getIntOr("sZ", 0);
        velocity = input.getDoubleOr("veloc", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        Vec3 motion = getDeltaMovement();
        output.putDouble("moX", motion.x);
        output.putDouble("moY", motion.y);
        output.putDouble("moZ", motion.z);
        output.putDouble("poX", getX());
        output.putDouble("poY", getY());
        output.putDouble("poZ", getZ());
        output.putDouble("decel", decelY);
        output.putDouble("accel", accelXZ);
        output.putInt("tX", targetX);
        output.putInt("tZ", targetZ);
        output.putInt("sX", startX);
        output.putInt("sZ", startZ);
        output.putDouble("veloc", velocity);
    }
}
