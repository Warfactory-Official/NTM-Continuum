package com.hbm.entity.projectile;

import dev.golem.core.data.Tags;

import com.hbm.entity.ModEntities;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A bullet in flight.
 *
 * <p>From 1.12.2 {@code EntityBulletBaseMK4} merged with the movement half of its {@code EntityThrowableNT} base.
 * Upstream's {@code onUpdate} carries {@code //FIXME: This wont work for 1.12 due to 1.9 physics changes}, so the
 * movement and collision code is written against the modern clip API. Split the base back out when a second
 * projectile needs it.
 *
 * <p>The flight model is upstream's, not vanilla projectile physics: the motion vector is a <b>unit heading</b>
 * and {@link BulletConfig#velocity} is the per-tick step length, so a round with velocity 10 travels 10 blocks per
 * tick, traced across the whole span. There is no air drag (upstream returns 1.0), and gravity (usually zero) is
 * subtracted from the heading after the step.
 *
 * <p>The config is synched by its <em>runtime</em> registry index, not its id: the client reads it every frame
 * and the index never leaves this session (see {@link BulletConfigs}).
 */
public class EntityBulletBaseMK4 extends CompatEntity {

    private static final EntityDataAccessor<Integer> BULLET_CONFIG =
            SynchedEntityData.defineId(EntityBulletBaseMK4.class, EntityDataSerializers.INT);

    /** How far past an entity's own box a bullet still counts as a hit, as upstream. */
    private static final double HITBOX_GRACE = 0.3D;

    public BulletConfig config;
    public LivingEntity thrower;
    public float damage;
    public int ricochets = 0;
    /** Rendering aids: the distance actually covered last tick. */
    public double velocity;
    /**
     * Extra step length a self-propelled round adds to its config velocity each tick. A rocket leaves
     * the tube slowly and builds up, which is why it is a field on the entity and not on the config.
     */
    public double accel;
    /**
     * What a guided round steers towards. The turn rate opens up with age, so a rocket flies straight
     * out of the launcher and only starts chasing once it has cleared it.
     */
    public @Nullable Entity lockonTarget;
    public double prevVelocity;
    /** Source charge-thrower hook state; culling is consumed by the client renderer. */
    public boolean ignoreFrustum;
    private boolean inGround;

    public EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, Level level) {
        super(type, level);
    }

    /**
     * For submunitions (ntm-next): spawned at an explicit position with an explicit motion, no shooter frame.
     * The source scales the unit heading by {@code config.spread + gunSpread} of scatter.
     */
    public EntityBulletBaseMK4(Level level, LivingEntity entity, BulletConfig config, float damage, float gunSpread,
                               double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        this(ModEntities.BULLET_MK4.get(), level);

        this.thrower = entity;
        this.setBulletConfig(config);

        this.damage = damage;

        this.snapTo(posX, posY, posZ, 0, 0);

        this.setDeltaMovement(motionX, motionY, motionZ);
        this.shoot(motionX, motionY, motionZ, this.config.spread + gunSpread);
    }

    /** The standard gun constructor: fired from {@code shooter}'s eyes with a muzzle offset. */
    public EntityBulletBaseMK4(LivingEntity shooter, BulletConfig config, float baseDamage, float gunSpread,
            double sideOffset, double heightOffset, double frontOffset) {
        this(ModEntities.BULLET_MK4.get(), shooter.level());
        this.thrower = shooter;
        setBulletConfig(config);
        this.damage = baseDamage * config.damageMult;

        float yaw = shooter.getYRot();
        float pitch = shooter.getXRot();
        setYRot(yaw);
        setXRot(pitch);

        // The muzzle offset is given in the shooter's own frame (side, height, forward) and has to be
        // rotated into world space before it can be added to the eye position.
        Vec3 offset = new Vec3(sideOffset, heightOffset, frontOffset)
                .xRot(-pitch * ((float) Math.PI / 180F))
                .yRot(-yaw * ((float) Math.PI / 180F));
        setPos(shooter.getX() + offset.x, shooter.getEyeY() + offset.y, shooter.getZ() + offset.z);

        double headingX = -Math.sin(yaw * Math.PI / 180D) * Math.cos(pitch * Math.PI / 180D);
        double headingZ = Math.cos(yaw * Math.PI / 180D) * Math.cos(pitch * Math.PI / 180D);
        double headingY = -Math.sin(pitch * Math.PI / 180D);
        shoot(headingX, headingY, headingZ, gunSpread);
    }

    /**
     * The turret constructor: no living shooter, and the heading comes straight from the turret's own
     * aim rather than from an entity's look vector. Angles arrive in <b>radians</b> and pitch is
     * negated, matching how a turret stores its own rotation.
     */
    public EntityBulletBaseMK4(Level level, BulletConfig config, float baseDamage, float gunSpread,
            float yawRadians, float pitchRadians) {
        this(ModEntities.BULLET_MK4.get(), level);
        setBulletConfig(config);
        this.damage = baseDamage * config.damageMult;

        float yaw = (float) Math.toDegrees(yawRadians);
        float pitch = (float) -Math.toDegrees(pitchRadians);
        setYRot(yaw);
        this.yRotO = yaw;
        setXRot(pitch);
        this.xRotO = pitch;

        double headingX = -Math.sin(yaw * Math.PI / 180D) * Math.cos(pitch * Math.PI / 180D);
        double headingZ = Math.cos(yaw * Math.PI / 180D) * Math.cos(pitch * Math.PI / 180D);
        double headingY = -Math.sin(pitch * Math.PI / 180D);
        shoot(headingX, headingY, headingZ, gunSpread);
    }

    /**
     * Aims this bullet along {@code (x, y, z)} with {@code spread} of inaccuracy, storing the result as a
     * unit heading. The gaussian-per-axis scatter and its 0.0075 scale are vanilla's own {@code shoot}
     * maths, which upstream relies on for every round's spread to mean what its config says.
     */
    public void shoot(double x, double y, double z, float spread) {
        Vec3 heading = new Vec3(x, y, z).normalize().add(
                random.nextGaussian() * 0.0075D * spread,
                random.nextGaussian() * 0.0075D * spread,
                random.nextGaussian() * 0.0075D * spread);
        setDeltaMovement(heading);
        alignRotationWithMotion();
        // The source keeps the spawn angle as both previous and current; subsequent movement updates
        // only the current angle so the renderer can interpolate the displacement turn.
        yRotO = getYRot();
        xRotO = getXRot();
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(BULLET_CONFIG, -1);
    }

    @Override
    protected void loadData(DataIn input) {
        setBulletConfig(BulletConfigs.get(input.getStringOr("config", "")));
        damage = input.getFloatOr("damage", 0F);
        ricochets = input.getIntOr("ricochets", 0);
    }

    @Override
    protected void saveData(DataOut output) {
        // Saved by id, not by the synched index: a world reloaded after a content change must still
        // resolve the same round.
        output.putString("config", config != null && config.id != null ? config.id.toString() : "");
        output.putFloat("damage", damage);
        output.putInt("ricochets", ricochets);
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false;
    }

    /** The distance this round covers in one tick: its config velocity plus whatever it has built up. */
    public double step() {
        return config.velocity + accel;
    }

    private void steerTowardsLockon() {
        Entity locked = lockonTarget;
        if (locked == null || !locked.isAlive()) return;

        Vec3 motion = getDeltaMovement();
        double speed = motion.length();
        if (speed == 0D) return;

        Vec3 delta = new Vec3(locked.getX() - getX(),
                locked.getY() + locked.getBbHeight() / 2D - getY(), locked.getZ() - getZ());
        float turn = Math.min(0.005F * tickCount, 1F);
        setDeltaMovement(new Vec3(
                Mth.lerp(turn, motion.x, delta.x),
                Mth.lerp(turn, motion.y, delta.y),
                Mth.lerp(turn, motion.z, delta.z)).normalize().scale(speed));
    }

    public BulletConfig getBulletConfig() {
        return BulletConfigs.byIndex(entityData.get(BULLET_CONFIG));
    }

    /** Keeps callers on the entity's seeded RNG without exposing the inherited field. */
    public RandomSource randomSource() {
        return random;
    }

    public void setBulletConfig(BulletConfig config) {
        this.config = config;
        entityData.set(BULLET_CONFIG, config == null ? -1 : BulletConfigs.indexOf(config));
    }

    /** Whoever fired this round, if they are still around. */
    public LivingEntity getThrower() {
        return thrower;
    }

    /** Keeps a hook embedded in its block rather than launching it again on the next tick. */
    public void getStuck(BlockPos pos, int side) {
        inGround = true;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (inGround) return;

        if (config == null) config = getBulletConfig();
        if (config == null) {
            discard();
            return;
        }

        steerTowardsLockon();

        Vec3 from = position();
        Vec3 heading = getDeltaMovement();
        Vec3 to = from.add(heading.scale(step()));

        // Blocks first: a block hit shortens the segment the entity sweep is allowed to cover, so a
        // bullet cannot reach through a wall to something standing behind it.
        HitResult hit = null;
        if (!config.isSpectral) {
            BlockHitResult blockHit = level().clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                hit = blockHit;
                to = blockHit.getLocation();
            }
        }

        if (!level().isClientSide() && config.impactsEntities) {
            if (config.doesPenetrate) {
                // A penetrating round keeps going, so every entity along the segment is hit in turn
                // rather than just the nearest one.
                for (EntityHitResult entityHit : entitiesAlong(from, to)) {
                    onImpact(entityHit);
                    if (isRemoved()) break;
                }
            } else {
                EntityHitResult entityHit = nearestEntityAlong(from, to);
                if (entityHit != null) hit = entityHit;
            }
        }

        if (hit != null && !isRemoved()) onImpact(hit);
        if (isRemoved()) return;

        prevVelocity = velocity;
        // The old renderer receives the distance covered by this tick, not the distance from the
        // entity to its starting point (which is zero before the move).  Preserve the shortened
        // block-hit segment as well; tracer length must stop at the wall.
        velocity = from.distanceTo(to);

        Vec3 displacement = getDeltaMovement().scale(step());
        setPos(position().add(displacement));
        updateRotationFromDisplacement(displacement);
        // Gravity acts on the heading, so a heavy round's arc builds up over its flight. Air drag is
        // No air drag: upstream's getAirDrag() returns 1.0 for bullets.
        if (config.gravity != 0F) {
            setDeltaMovement(getDeltaMovement().subtract(0D, config.gravity, 0D));
        }
        if (!level().isClientSide() && tickCount > config.expires) {
            discard();
            return;
        }

        if (config.onUpdate != null) config.onUpdate.accept(this);
    }

    /** Runs the config's impact chain: bespoke impact first, then ricochet, then the entity hit. */
    protected void onImpact(HitResult hit) {
        if (level().isClientSide()) return;

        if (config.onImpact != null) config.onImpact.accept(this, hit);
        if (isRemoved()) return;
        if (config.onRicochet != null) config.onRicochet.accept(this, hit);
        if (isRemoved()) return;
        if (config.onEntityHit != null) config.onEntityHit.accept(this, hit);
    }

    /** Every hittable entity whose box the segment crosses, nearest first. */
    private List<EntityHitResult> entitiesAlong(Vec3 from, Vec3 to) {
        List<EntityHitResult> hits = new ArrayList<>();
        for (Entity entity : candidates(from, to)) {
            AABB box = entity.getBoundingBox().inflate(HITBOX_GRACE);
            Optional<Vec3> clip = box.clip(from, to);
            if (clip.isPresent()) hits.add(new EntityHitResult(entity, clip.get()));
        }
        hits.sort((a, b) -> Double.compare(a.getLocation().distanceToSqr(from),
                b.getLocation().distanceToSqr(from)));
        return hits;
    }

    private EntityHitResult nearestEntityAlong(Vec3 from, Vec3 to) {
        List<EntityHitResult> hits = entitiesAlong(from, to);
        return hits.isEmpty() ? null : hits.get(0);
    }

    private List<Entity> candidates(Vec3 from, Vec3 to) {
        // The shooter is only exempt for the first few ticks, so a bullet can still hit its own firer
        // once it has had time to travel, which is how upstream's ricochets can come back at you.
        Predicate<Entity> filter = entity -> entity.isPickable()
                && !(entity == thrower && tickCount < config.selfDamageDelay);
        return level().getEntities(this, new AABB(from, to).inflate(1.0D), filter);
    }

    private void alignRotationWithMotion() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-7D) return;
        setYRot((float) (Math.atan2(motion.x, motion.z) * 180D / Math.PI));
        setXRot((float) (Math.atan2(motion.y, Math.sqrt(motion.x * motion.x + motion.z * motion.z)) * 180D / Math.PI));
    }

    /** Source-style rotation from the actual displacement, retaining the old angle for interpolation. */
    private void updateRotationFromDisplacement(Vec3 displacement) {
        if (displacement.lengthSqr() < 1.0E-7D) return;
        float previousYaw = getYRot();
        float previousPitch = getXRot();
        yRotO = previousYaw;
        xRotO = previousPitch;
        float yaw = (float) (Math.atan2(displacement.x, displacement.z) * 180D / Math.PI);
        float pitch = (float) (Math.atan2(displacement.y,
                Math.sqrt(displacement.x * displacement.x + displacement.z * displacement.z)) * 180D / Math.PI);
        setYRot(yaw);
        setXRot(pitch);
        while (getXRot() - xRotO < -180F) xRotO -= 360F;
        while (getXRot() - xRotO >= 180F) xRotO += 360F;
        while (getYRot() - yRotO < -180F) yRotO -= 360F;
        while (getYRot() - yRotO >= 180F) yRotO += 360F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256.0D * 256.0D;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
