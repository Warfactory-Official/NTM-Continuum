package com.hbm.entity.projectile;

import com.hbm.entity.ModEntities;
import com.hbm.entity.projectile.EntityCoin;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import dev.golem.core.data.CompatEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.SynchedData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The MK4 hitscan projectile. Its impact callback is deliberately separate from ordinary bullets. */
public class EntityBulletBeamBase extends CompatEntity {

    private static final double MIN_BEAM_LENGTH = 1.0E-6D;
    private static final double DEFAULT_BEAM_RANGE = 250D;

    private static final EntityDataAccessor<Integer> BULLET_CONFIG =
            SynchedEntityData.defineId(EntityBulletBeamBase.class, EntityDataSerializers.INT);

    public @Nullable LivingEntity thrower;
    public BulletConfig config;
    public float damage;
    public double headingX;
    public double headingY;
    public double headingZ;
    public double beamLength;
    /** The measured endpoint is retained separately so the client receives the exact source segment. */
    public double beamEndX;
    public double beamEndY;
    public double beamEndZ;
    /** Set only after the server has measured the authoritative endpoint. */
    private boolean renderStateReady;

    /** API for the beam factory owner; it mirrors BulletConfig.onImpactBeam without changing that class. */
    private @Nullable BiConsumer<EntityBulletBeamBase, HitResult> impactCallback;

    public EntityBulletBeamBase(EntityType<? extends EntityBulletBeamBase> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public EntityBulletBeamBase(Level level, BulletConfig config, float baseDamage) {
        this(ModEntities.BEAM_MK4.get(), level);
        setBulletConfig(config);
        damage = baseDamage * config.damageMult;
    }

    public EntityBulletBeamBase(LivingEntity shooter, BulletConfig config, float baseDamage) {
        this(shooter.level(), config, baseDamage);
        thrower = shooter;
    }

    public EntityBulletBeamBase(LivingEntity shooter, BulletConfig config, float baseDamage,
            float angularInaccuracy, double sideOffset, double heightOffset, double frontOffset) {
        this(shooter.level(), config, baseDamage);
        thrower = shooter;

        float yaw = shooter.getYRot() + (float) random.nextGaussian() * angularInaccuracy;
        float pitch = shooter.getXRot() + (float) random.nextGaussian() * angularInaccuracy;
        setYRot(yaw);
        setXRot(pitch);
        Vec3 offset = new Vec3(sideOffset, heightOffset, frontOffset)
                .xRot(-pitch * ((float) Math.PI / 180F))
                .yRot(-yaw * ((float) Math.PI / 180F));
        setPos(shooter.getX() + offset.x, shooter.getEyeY() + offset.y, shooter.getZ() + offset.z);

        Vec3 direction = directionFromRotations(yaw, pitch).scale(DEFAULT_BEAM_RANGE);
        headingX = direction.x;
        headingY = direction.y;
        headingZ = direction.z;
        performHitscan();
    }

    public void setImpactCallback(@Nullable BiConsumer<EntityBulletBeamBase, HitResult> callback) {
        this.impactCallback = callback;
    }

    public LivingEntity getThrower() { return thrower; }

    public void setRotationsFromVector(Vec3 delta) {
        Vec3 normalized = delta.lengthSqr() > MIN_BEAM_LENGTH
                ? delta.normalize() : new Vec3(0D, -1D, 0D);
        headingX = normalized.x;
        headingY = normalized.y;
        headingZ = normalized.z;
        setXRot((float) (-Math.asin(normalized.y) * 180D / Math.PI));
        setYRot((float) (-Math.atan2(normalized.x, normalized.z) * 180D / Math.PI));
    }

    /** Applies the server's source render state without re-running hitscan on the client. */
    public void applyRenderState(com.hbm.packet.ProjectileStatePayload payload,
            @Nullable BulletConfig syncedConfig) {
        Vec3 endpoint = new Vec3(payload.endpointX(), payload.endpointY(), payload.endpointZ());
        Vec3 endpointDirection = endpoint.subtract(position());
        Vec3 heading = new Vec3(payload.headingX(), payload.headingY(), payload.headingZ());
        if (heading.lengthSqr() <= MIN_BEAM_LENGTH) heading = endpointDirection;
        if (heading.lengthSqr() <= MIN_BEAM_LENGTH) return;
        double length = payload.beamLength();
        if (length <= MIN_BEAM_LENGTH) length = endpointDirection.length();
        if (length <= MIN_BEAM_LENGTH) return;

        headingX = heading.x;
        headingY = heading.y;
        headingZ = heading.z;
        beamEndX = endpoint.x;
        beamEndY = endpoint.y;
        beamEndZ = endpoint.z;
        beamLength = length;
        setYRot(payload.yaw());
        setXRot(payload.pitch());
        if (syncedConfig != null) config = syncedConfig;
        else if (!payload.configId().isEmpty())
            com.hbm.Constants.LOG.warn("Missing bullet config {} for beam entity {}", payload.configId(), getId());
        renderStateReady = true;
    }

    public void performHitscanExternal(double range) {
        Vec3 heading = new Vec3(headingX, headingY, headingZ);
        if (heading.lengthSqr() <= MIN_BEAM_LENGTH) heading = directionFromRotations(getYRot(), getXRot());
        Vec3 direction = heading.normalize().scale(Math.max(range, MIN_BEAM_LENGTH));
        headingX = direction.x;
        headingY = direction.y;
        headingZ = direction.z;
        performHitscan();
    }

    @Override
    protected void defineData(SynchedData data) {
        data.define(BULLET_CONFIG, -1);
    }

    public BulletConfig getBulletConfig() {
        return BulletConfigs.byIndex(entityData.get(BULLET_CONFIG));
    }

    public void setBulletConfig(BulletConfig config) {
        this.config = config;
        entityData.set(BULLET_CONFIG, config == null ? -1 : BulletConfigs.indexOf(config));
        // The gun LEGO path constructs and performs the hitscan before it can add the entity.  Resolve
        // the config callback here, not only in the later spawn path, or the first impact is lost.
        this.impactCallback = config == null ? null : config.onImpactBeam;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) com.hbm.packet.ProjectileStatePayload.applyPending(this);
        if (config == null) config = getBulletConfig();
        if (config == null) {
            discard();
            return;
        }
        if (config.onUpdate != null) config.onUpdate.accept(this);
        super.tick();
        if (!level().isClientSide() && hasRenderState()) com.hbm.packet.ProjectileStatePayload.send(this);
        if (!level().isClientSide() && tickCount > config.expires) discard();
    }

    protected void performHitscan() {
        Vec3 from = position();
        Vec3 to = from.add(headingX, headingY, headingZ);
        HitResult block = null;
        if (!isSpectral()) {
            BlockHitResult hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (hit.getType() != HitResult.Type.MISS) {
                block = hit;
                to = hit.getLocation();
            }
        }

        if (!level().isClientSide() && config.impactsEntities) {
            CoinHit coin = findCoin(from, to);
            if (coin != null) {
                setBeamEndpoint(from, coin.location());
                ricochetAtCoin(coin);
                return;
            }
            List<EntityHitResult> hits = entityHits(from, to);
            if (config.doesPenetrate) {
                for (EntityHitResult hit : hits) impactCallback(hit);
            } else if (!hits.isEmpty()) {
                block = hits.get(0);
                to = block.getLocation();
            }
        }

        if (block != null) impactCallback(block);
        setBeamEndpoint(from, to);
    }

    private void setBeamEndpoint(Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) <= MIN_BEAM_LENGTH) {
            Vec3 direction = new Vec3(headingX, headingY, headingZ);
            if (direction.lengthSqr() <= MIN_BEAM_LENGTH) direction = directionFromRotations(getYRot(), getXRot());
            to = from.add(direction.normalize().scale(MIN_BEAM_LENGTH));
        }
        beamEndX = to.x;
        beamEndY = to.y;
        beamEndZ = to.z;
        beamLength = from.distanceTo(to);
        renderStateReady = true;
        // Publish as soon as a tracked beam has a measured endpoint; waiting for the next tick leaves
        // the client with a zero-length render state during the beam's first visible frame.
        if (!level().isClientSide() && hasRenderState() && level().getEntity(getId()) == this)
            com.hbm.packet.ProjectileStatePayload.send(this);
    }

    public boolean hasRenderState() { return renderStateReady; }

    private static Vec3 directionFromRotations(float yaw, float pitch) {
        double yawRadians = yaw * Math.PI / 180D;
        double pitchRadians = pitch * Math.PI / 180D;
        return new Vec3(-Math.sin(yawRadians) * Math.cos(pitchRadians),
                -Math.sin(pitchRadians), Math.cos(yawRadians) * Math.cos(pitchRadians));
    }

    private CoinHit findCoin(Vec3 from, Vec3 to) {
        EntityCoin closest = null;
        Vec3 closestLocation = null;
        double distance = Double.MAX_VALUE;
        for (Entity entity : level().getEntities(this, new AABB(from, to).inflate(1D),
                candidate -> candidate instanceof EntityCoin && candidate.isAlive())) {
            Vec3 location = entity.getBoundingBox().inflate(0.3D).clip(from, to).orElse(null);
            if (location == null) continue;
            double candidateDistance = from.distanceToSqr(location);
            if (candidateDistance < distance) {
                distance = candidateDistance;
                closest = (EntityCoin) entity;
                closestLocation = location;
            }
        }
        return closest == null ? null : new CoinHit(closest, closestLocation);
    }

    private void ricochetAtCoin(CoinHit hit) {
        EntityCoin coin = hit.coin();
        coin.discard();
        double range = 50D;
        Entity nearestCoin = null;
        Entity nearestPlayer = null;
        Entity nearestMonster = null;
        Entity nearestLiving = null;
        double coinDistance = Double.MAX_VALUE;
        double playerDistance = Double.MAX_VALUE;
        double monsterDistance = Double.MAX_VALUE;
        double livingDistance = Double.MAX_VALUE;
        AABB area = new AABB(hit.location(), hit.location()).inflate(range);
        for (Entity entity : level().getEntities(this, area, candidate -> candidate.isAlive())) {
            if (entity == thrower) continue;
            double distance = entity.distanceToSqr(coin);
            if (distance > range * range) continue;
            if (entity instanceof EntityCoin && distance < coinDistance) {
                coinDistance = distance;
                nearestCoin = entity;
            } else if (entity instanceof Player && distance < playerDistance) {
                playerDistance = distance;
                nearestPlayer = entity;
            } else if (entity instanceof Monster && distance < monsterDistance) {
                monsterDistance = distance;
                nearestMonster = entity;
            } else if (entity instanceof LivingEntity && distance < livingDistance) {
                livingDistance = distance;
                nearestLiving = entity;
            }
        }

        Entity target = nearestCoin != null ? nearestCoin
                : nearestPlayer != null ? nearestPlayer
                : nearestMonster != null ? nearestMonster : nearestLiving;
        LivingEntity owner = coin.getThrower() != null ? coin.getThrower() : thrower;
        if (owner == null) return;
        EntityBulletBeamBase reflected = new EntityBulletBeamBase(owner, config, damage * 1.25F);
        reflected.setPos(hit.location());
        if (target != null) {
            reflected.setRotationsFromVector(new Vec3(target.getX() - reflected.getX(),
                    target.getY() + target.getBbHeight() / 2D - reflected.getY(), target.getZ() - reflected.getZ()));
        } else {
            reflected.setRotationsFromVector(new Vec3(random.nextGaussian() * 0.5D, -1D,
                    random.nextGaussian() * 0.5D));
        }
        reflected.performHitscanExternal(250D);
        level().addFreshEntity(reflected);
        if (level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.sendParticles(ParticleTypes.EXPLOSION, hit.location().x, hit.location().y,
                    hit.location().z, 1, 0D, 0D, 0D, 0D);
        }
    }

    private record CoinHit(EntityCoin coin, Vec3 location) {}

    private List<EntityHitResult> entityHits(Vec3 from, Vec3 to) {
        List<EntityHitResult> hits = new ArrayList<>();
        AABB search = new AABB(from, to).inflate(1D);
        for (Entity entity : level().getEntities(this, search, candidate -> candidate.isPickable()
                && candidate != thrower && candidate.isAlive())) {
            entity.getBoundingBox().inflate(0.3D).clip(from, to)
                    .ifPresent(location -> hits.add(new EntityHitResult(entity, location)));
        }
        hits.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(from)));
        return hits;
    }

    private void impactCallback(HitResult hit) {
        if (!level().isClientSide() && impactCallback != null) impactCallback.accept(this, hit);
    }

    public boolean doesImpactEntities() { return config != null && config.impactsEntities; }
    public boolean doesPenetrate() { return config != null && config.doesPenetrate; }
    public boolean isSpectral() { return config != null && config.isSpectral; }

    @Override public boolean shouldRenderAtSqrDistance(double distance) { return true; }
    // The original Entity subclass inherits Entity.canBeCollidedWith() == false.  Hitscan target
    // detection is explicit and must not turn the beam itself into a physical collision target.
    @Override public boolean isPickable() { return false; }
    @Override public boolean fireImmune() { return true; }
    @Override public boolean shouldBeSaved() { return false; }

    @Override protected void saveData(DataOut output) {
        output.putString("config", config != null && config.id != null ? config.id.toString() : "");
        output.putFloat("damage", damage);
    }

    @Override protected void loadData(DataIn input) {
        setBulletConfig(BulletConfigs.get(input.getStringOr("config", "")));
        damage = input.getFloatOr("damage", 0F);
    }
}
