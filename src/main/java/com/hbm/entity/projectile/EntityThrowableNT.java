package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The hand-rolled projectile 1.12.2 uses in place of vanilla's throwable: a straight motion step with a
 * ray cast across it, an entity sweep over the same span, and an {@link #onImpact} driven from the tick
 * rather than from vanilla's hit callback.
 *
 * <p>This is the base {@link EntityBulletBaseMK4}'s javadoc anticipated. The bullet is
 * <em>not</em> built on it: its flight model differs (the config's velocity is a per-tick step length).
 * New projectiles use this base.
 *
 * <p>Two upstream pieces are absent. There is no interpolation handler, {@code EntityThrowableInterp}
 * exists only on 26.1 and nothing in this port interpolates a non-living entity, so a rocket rides the
 * entity tracker like every other projectile here. And {@link #getStuck} does not resend the position:
 * upstream's teleport packet has no pre-26.1 spelling, and a stuck projectile is already where the
 * client last saw it.
 */
public abstract class EntityThrowableNT extends CompatEntity {

    private static final EntityDataAccessor<Byte> STUCK_IN =
            SynchedEntityData.defineId(EntityThrowableNT.class, EntityDataSerializers.BYTE);

    public int throwableShake;
    public int ticksInGround;
    public int ticksInAir;
    public @Nullable LivingEntity thrower;

    protected boolean inGround;
    private BlockPos stuckBlockPos = BlockPos.ZERO;
    /** The block, not its state: a stair rotating or a redstone level changing must not unstick this. */
    private @Nullable Block stuckBlock;

    protected EntityThrowableNT(EntityType<? extends EntityThrowableNT> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(STUCK_IN, (byte) 0);
    }

    public int getStuckIn() {
        return this.entityData.get(STUCK_IN);
    }

    public void setStuckIn(int side) {
        this.entityData.set(STUCK_IN, (byte) side);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        double perimeter = getBoundingBox().getSize() * 4.0D * 64.0D;
        return dist < perimeter * perimeter;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    protected float throwForce() {
        return 1.5F;
    }

    protected double headingForceMult() {
        return 0.0075D;
    }

    protected double motionMult() {
        return 1.0D;
    }

    protected float throwAngle() {
        return 0.0F;
    }

    /**
     * 1.7 EntityThrowableNT(World, EntityLivingBase): eye-height spawn, 0.16 side offset off the yaw
     * (ntm-next's spelling, used by every thrown entity copied from it).
     */
    public void initThrower(LivingEntity thrower) {
        setThrower(thrower);
        snapTo(thrower.getX(), thrower.getY() + thrower.getEyeHeight(), thrower.getZ(), thrower.getYRot(),
                thrower.getXRot());
        double px = getX() - Math.cos(getYRot() / 180.0F * Math.PI) * 0.16F;
        double py = getY() - 0.1D;
        double pz = getZ() - Math.sin(getYRot() / 180.0F * Math.PI) * 0.16F;
        setPos(px, py, pz);
        float velocity = 0.4F;
        double motionX = -Math.sin(getYRot() / 180.0F * Math.PI) * Math.cos(getXRot() / 180.0F * Math.PI) * velocity;
        double motionZ = Math.cos(getYRot() / 180.0F * Math.PI) * Math.cos(getXRot() / 180.0F * Math.PI) * velocity;
        double motionY = -Math.sin((getXRot() + this.throwAngle()) / 180.0F * Math.PI) * velocity;
        this.setThrowableHeading(motionX, motionY, motionZ, this.throwForce(), 1.0F);
    }

    public @Nullable LivingEntity getThrower() {
        return this.thrower;
    }

    public void setThrower(@Nullable LivingEntity thrower) {
        this.thrower = thrower;
    }

    /** ntm-next's owner spelling of the same field. */
    public net.minecraft.world.entity.@Nullable Entity getOwner() {
        return this.thrower;
    }

    /**
     * Aims this projectile, with a <em>gaussian</em> per-axis spread scaled by {@link #headingForceMult}
     * rather than vanilla {@code shoot}'s triangular one: every upstream velocity and inaccuracy figure
     * is calibrated against this.
     */
    public void setThrowableHeading(double motionX, double motionY, double motionZ, float velocity,
            float inaccuracy) {
        double length = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (length == 0D) return;

        double x = (motionX / length + random.nextGaussian() * headingForceMult() * inaccuracy) * velocity;
        double y = (motionY / length + random.nextGaussian() * headingForceMult() * inaccuracy) * velocity;
        double z = (motionZ / length + random.nextGaussian() * headingForceMult() * inaccuracy) * velocity;
        setDeltaMovement(x, y, z);

        float yaw = (float) (Math.atan2(x, z) * 180D / Math.PI);
        float pitch = (float) (Math.atan2(y, Math.sqrt(x * x + z * z)) * 180D / Math.PI);
        setYRot(yaw);
        this.yRotO = yaw;
        setXRot(pitch);
        this.xRotO = pitch;
        this.ticksInGround = 0;
    }

    @Override
    public void tick() {
        baseTick();

        if (this.throwableShake > 0) this.throwableShake--;

        if (this.inGround) {
            if (level().getBlockState(this.stuckBlockPos).getBlock() == this.stuckBlock) {
                this.ticksInGround++;
                if (groundDespawn() > 0 && this.ticksInGround == groundDespawn()) discard();
                return;
            }

            this.inGround = false;
            this.ticksInGround = 0;
            this.ticksInAir = 0;
        }

        this.ticksInAir++;

        Vec3 motion = getDeltaMovement().scale(motionMult());
        Vec3 from = position();
        Vec3 to = from.add(motion);

        HitResult hit = null;
        if (!isSpectral()) {
            BlockHitResult blockHit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                hit = blockHit;
                to = blockHit.getLocation();
            }
        }

        if (!level().isClientSide() && doesImpactEntities()) {
            if (doesPenetrate()) {
                for (EntityHitResult entityHit : entitiesAlong(from, to)) {
                    onImpact(entityHit);
                    if (isRemoved()) return;
                }
            } else {
                EntityHitResult nearest = nearestEntityAlong(from, to);
                if (nearest != null) hit = nearest;
            }
        }

        if (hit != null) onImpact(hit);
        if (isRemoved()) return;

        if (!onGround()) alignRotationWithMotion();

        float drag = getAirDrag();
        double gravity = getGravityVelocity();

        if (fullBlockCollisions()) {
            move(MoverType.SELF, motion);
        } else {
            setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);
        }

        if (isInWater()) {
            Vec3 velocity = getDeltaMovement();
            for (int i = 0; i < 4; i++) {
                level().addParticle(ParticleTypes.BUBBLE, getX() - velocity.x * 0.25D, getY() - velocity.y * 0.25D,
                        getZ() - velocity.z * 0.25D, velocity.x, velocity.y, velocity.z);
            }
            drag = getWaterDrag();
        }

        setDeltaMovement(getDeltaMovement().scale(drag).subtract(0D, gravity, 0D));
    }

    /** Upstream eases the drawn angles toward the motion by a fifth each tick rather than snapping. */
    private void alignRotationWithMotion() {
        Vec3 velocity = getDeltaMovement();
        double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float yaw = (float) (Math.atan2(velocity.x, velocity.z) * 180D / Math.PI);
        float pitch = (float) (Math.atan2(velocity.y, horizontal) * 180D / Math.PI);

        while (pitch - this.xRotO < -180F) this.xRotO -= 360F;
        while (pitch - this.xRotO >= 180F) this.xRotO += 360F;
        while (yaw - this.yRotO < -180F) this.yRotO -= 360F;
        while (yaw - this.yRotO >= 180F) this.yRotO += 360F;

        setXRot(this.xRotO + (pitch - this.xRotO) * 0.2F);
        setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);
    }

    private List<EntityHitResult> entitiesAlong(Vec3 from, Vec3 to) {
        List<EntityHitResult> hits = new ArrayList<>();
        for (Entity entity : level().getEntities(this, getBoundingBox().expandTowards(to.subtract(from))
                .inflate(1.0D))) {
            if (!entity.isPickable() || !entity.isAlive()) continue;
            if (entity == this.thrower && this.ticksInAir < selfDamageDelay()) continue;
            entity.getBoundingBox().inflate(0.3D).clip(from, to)
                    .ifPresent(where -> hits.add(new EntityHitResult(entity, where)));
        }
        hits.sort((a, b) -> Double.compare(a.getLocation().distanceToSqr(from),
                b.getLocation().distanceToSqr(from)));
        return hits;
    }

    private @Nullable EntityHitResult nearestEntityAlong(Vec3 from, Vec3 to) {
        List<EntityHitResult> hits = entitiesAlong(from, to);
        return hits.isEmpty() ? null : hits.get(0);
    }

    public boolean fullBlockCollisions() {
        return false;
    }

    public boolean doesImpactEntities() {
        return true;
    }

    public boolean doesPenetrate() {
        return false;
    }

    public boolean isSpectral() {
        return false;
    }

    public int selfDamageDelay() {
        return 5;
    }

    public void getStuck(BlockPos pos, int side) {
        this.stuckBlockPos = pos;
        this.stuckBlock = level().getBlockState(pos).getBlock();
        this.inGround = true;
        setDeltaMovement(Vec3.ZERO);
        setStuckIn(side);
    }

    public double getGravityVelocity() {
        return 0.03D;
    }

    protected float getAirDrag() {
        return 0.99F;
    }

    protected float getWaterDrag() {
        return 0.8F;
    }

    protected int groundDespawn() {
        return 1200;
    }

    protected abstract void onImpact(HitResult hit);

    @Override
    protected void saveData(DataOut output) {
        output.putInt("xTile", this.stuckBlockPos.getX());
        output.putInt("yTile", this.stuckBlockPos.getY());
        output.putInt("zTile", this.stuckBlockPos.getZ());
        output.storeNullable("inTile", BuiltInRegistries.BLOCK.byNameCodec(), this.stuckBlock);
        output.putByte("shake", (byte) this.throwableShake);
        output.putBoolean("inGround", this.inGround);
        output.putInt("ticksInGround", this.ticksInGround);
        output.putInt("ticksInAir", this.ticksInAir);
    }

    @Override
    protected void loadData(DataIn input) {
        this.stuckBlockPos = new BlockPos(input.getIntOr("xTile", 0), input.getIntOr("yTile", 0),
                input.getIntOr("zTile", 0));
        this.stuckBlock = input.read("inTile", BuiltInRegistries.BLOCK.byNameCodec()).orElse(null);
        this.throwableShake = input.getByteOr("shake", (byte) 0) & 255;
        this.inGround = input.getBooleanOr("inGround", false);
        this.ticksInGround = input.getIntOr("ticksInGround", 0);
        this.ticksInAir = input.getIntOr("ticksInAir", 0);
    }
}
