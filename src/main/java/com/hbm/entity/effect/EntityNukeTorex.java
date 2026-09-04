package com.hbm.entity.effect;

import dev.golem.core.data.CompatEntity;
import com.hbm.client.TorexClientEffects;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.EntityChunkLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Toroidal-convection mushroom-cloud effect entity (<b>Tor</b>oidal Convection Simulation <b>Ex</b>plosion).
 * A faithful port of 1.12.2's {@code EntityNukeTorex}: the entity is invisible server-side (it only ages
 * out at {@link #maxAge}); the whole visual simulation runs <em>client-side</em> in {@link #tick()},
 * spawning thousands of {@link Cloudlet} billboards that the {@code TorexRenderer} draws.
 *
 * <p>Cloudlet kinds ({@link TorexType}): STANDARD (the rising torus of convection cells), SHOCK (the
 * ground shockwave ring, first 150 ticks), RING (the collar around the core, first 200 ticks), and
 * CONDENSATION (the Wilson-cloud rings at heights 130/170, gated by biome humidity, first 220 ticks).
 *
 * <p>Client-only interactions (the sky flash, the detonation sound, biome humidity) are routed through
 * {@link TorexClientEffects} so a dedicated server never resolves {@code net.minecraft.client} symbols.
 * The scale, colour type and server animation age ride {@link SynchedEntityData}. Not saved to disk
 * ({@link #shouldBeSaved()}).
 */
public class EntityNukeTorex extends CompatEntity {

    private static final long CLIENT_CACHE_MILLIS = 10 * 60 * 1000L;
    private static final long CLIENT_REPLAY_BUDGET_NANOS = 5_000_000L;
    private static final Map<UUID, CachedAnimation> CLIENT_ANIMATIONS = new ConcurrentHashMap<>();

    private record CachedAnimation(long cachedAt, double coreHeight, double convectionHeight,
                                   double torusWidth, double rollerSize, double heat, double lastSpawnY,
                                   int animationAge, int maxAge, float humidity,
                                   boolean didPlaySound, boolean didShake,
                                   long randomState, boolean hasGaussian, double gaussian,
                                   long nextCloudletOrdinal,
                                   long timelineFingerprint,
                                   ArrayList<CloudletState> cloudlets) {}

    private record CloudletState(double posX, double posY, double posZ,
                                 double prevPosX, double prevPosY, double prevPosZ,
                                 double motionX, double motionY, double motionZ,
                                 int age, int life, float angle, boolean dead, float rangeMod,
                                 float colorMod, double colorR, double colorG, double colorB,
                                 double prevColorR, double prevColorG, double prevColorB,
                                 TorexType type, float startingScale, float growingScale,
                                 double motionMult, long creationOrdinal) {}

    public static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Integer> ANIMATION_AGE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> INITIAL_SPAWN_TARGET = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> FINAL_SPAWN_TARGET = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> TERRAIN_CHANGE_AGE = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> HUMIDITY = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> RANDOM_SEED_LOW = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> RANDOM_SEED_HIGH = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> START_TIME_LOW = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> START_TIME_HIGH = SynchedEntityData.defineId(EntityNukeTorex.class, EntityDataSerializers.INT);

    public static final int firstCondenseHeight = 130;
    public static final int secondCondenseHeight = 170;
    public static final int blastWaveHeadstart = 5;
    public static final int maxCloudlets = 20_000;

    // Nuke colors
    public static final double nr1 = 2.5;
    public static final double ng1 = 1.3;
    public static final double nb1 = 0.4;
    public static final double nr2 = 0.1;
    public static final double ng2 = 0.075;
    public static final double nb2 = 0.05;

    // Balefire colors
    public static final double br1 = 1;
    public static final double bg1 = 2;
    public static final double bb1 = 0.5;
    public static final double br2 = 0.1;
    public static final double bg2 = 0.1;
    public static final double bb2 = 0.1;

    public double coreHeight = 3;
    public double convectionHeight = 3;
    public double torusWidth = 3;
    public double rollerSize = 1;
    public double heat = 1;
    public double lastSpawnY = -1;
    public ArrayList<Cloudlet> cloudlets = new ArrayList<>();
    public int lastRenderSortTick = Integer.MIN_VALUE;
    public double lastRenderSortCameraX = Double.NaN;
    public double lastRenderSortCameraY = Double.NaN;
    public double lastRenderSortCameraZ = Double.NaN;
    public int maxAge = 1000;
    public float humidity = -1;

    public boolean didPlaySound = false;
    public boolean didShake = false;
    private boolean clientAgeAligned;
    private boolean clientScaleInitialized;
    private boolean clientAnimationCached;
    private boolean clientAnimationReady = true;
    private boolean clientHistoricalTimeline;
    private int clientReplayTargetAge = -1;
    private VisualRandom visualRandom;
    private long nextCloudletOrdinal;
    private long clientTimelineFingerprint = Long.MIN_VALUE;
    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 0);

    public EntityNukeTorex(EntityType<?> type, Level level) {
        super(type, level);
        // Fire immunity + the 20×40 bounding box come from the EntityType.Builder (see ModEntities).
    }

    @Override
    protected void defineData(dev.golem.core.data.SynchedData data) {
        data.define(SCALE, 1.0F);
        data.define(TYPE, (byte) 0);
        data.define(ANIMATION_AGE, 0);
        data.define(INITIAL_SPAWN_TARGET, Integer.MIN_VALUE);
        data.define(FINAL_SPAWN_TARGET, Integer.MIN_VALUE);
        data.define(TERRAIN_CHANGE_AGE, -1);
        data.define(HUMIDITY, 0F);
        data.define(RANDOM_SEED_LOW, 0);
        data.define(RANDOM_SEED_HIGH, 0);
        data.define(START_TIME_LOW, 0);
        data.define(START_TIME_HIGH, 0);
    }

    @Override
    protected void loadData(DataIn input) {
        setScale(input.getFloatOr("scale", 1.0F));
        this.entityData.set(TYPE, input.getByteOr("type", (byte) 0));
    }

    @Override
    protected void saveData(DataOut output) {
        output.putFloat("scale", this.entityData.get(SCALE));
        output.putByte("type", this.entityData.get(TYPE));
    }

    /** The effect is transient — never persist it (old {@code writeToNBTOptional == false}). */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void remove(RemovalReason reason) {
        cacheClientAnimationIfNeeded();
        chunkLoader.clearChunkLoader();
        super.remove(reason);
    }

    @Override
    public void onClientRemoval() {
        cacheClientAnimationIfNeeded();
        super.onClientRemoval();
    }

    @Override
    protected boolean onHurt(DamageSource source, float damage) {
        return false;
    }

    /** Render regardless of distance/frustum (old {@code isInRangeToRenderDist == true}). */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            prepareClientAnimation();
            if (!advanceClientReplay()) return;
            if (this.tickCount >= maxAge && authoritativeAnimationAge() >= maxAge) return;
            simulateClientTick(true);
        } else {
            chunkLoader.tick();
            detectTerrainChange();
            if (this.tickCount > maxAge) this.discard();
        }

        super.tick();
        if (!level().isClientSide()) this.entityData.set(ANIMATION_AGE, this.tickCount);
    }

    private void simulateClientTick(boolean playEffects) {
        double s = this.getScale();
        double cs = 1.5;
        if (this.tickCount == 1 && !clientScaleInitialized) {
            this.setScale((float) s);
            clientScaleInitialized = true;
        }

        if (humidity == -1) humidity = this.entityData.get(HUMIDITY);
        if (lastSpawnY == -1) lastSpawnY = getY() - 3;

        int spawnTarget = replaySpawnTarget();
        double moveSpeed = 0.5D;
        if (Math.abs(spawnTarget - lastSpawnY) < moveSpeed) lastSpawnY = spawnTarget;
        else lastSpawnY += moveSpeed * Math.signum(spawnTarget - lastSpawnY);

        double range = (torusWidth - rollerSize) * 0.5;
        double simSpeed = getSimulationSpeed();
        int lifetime = Math.min((this.tickCount * this.tickCount) + 200, maxAge - this.tickCount + 200);
        int toSpawn = (int) (0.6 * Math.min(Math.max(0, maxCloudlets - cloudlets.size()),
                Math.ceil(10 * simSpeed * simSpeed * Math.min(1, 1200 / (double) lifetime))));
        for (int i = 0; i < toSpawn; i++) {
            double x = getX() + visualRandom.nextGaussian() * range;
            double z = getZ() + visualRandom.nextGaussian() * range;
            Cloudlet cloud = new Cloudlet(x, lastSpawnY, z,
                    (float) (visualRandom.nextDouble() * 2D * Math.PI), 0, lifetime);
            cloud.setScale((float) (StrictMath.sqrt(s) * 3 + this.tickCount * 0.0025 * s),
                    (float) (StrictMath.sqrt(s) * 3 + this.tickCount * 0.0025 * 6 * cs * s));
            cloudlets.add(cloud);
        }

        if (playEffects && this.tickCount < 120 * s) TorexClientEffects.skyFlash(level());

        if (this.tickCount < 150) {
            int cloudCount = Math.min(this.tickCount * 2, 100);
            int shockLife = Math.max(400 - this.tickCount * 20, 50);
            for (int i = 0; i < cloudCount; i++) {
                double vx = (this.tickCount + visualRandom.nextDouble() * 2) * 1.5;
                float rot = (float) (Math.PI * 2 * visualRandom.nextDouble());
                double cos = StrictMath.cos(rot);
                double sin = StrictMath.sin(rot);
                double rx = vx * cos;
                double rz = -vx * sin;
                this.cloudlets.add(new Cloudlet(rx + getX(), replaySurfaceHeight(
                        (int) (rx + getX()) + 1, (int) (rz + getZ())), rz + getZ(), rot, 0,
                        shockLife, TorexType.SHOCK).setScale((float) s * 5F, (float) s * 2F)
                        .setMotion(Mth.clamp(0.25 * this.tickCount - 5, 0, 1)));
            }
            if (playEffects && !didPlaySound) didPlaySound = TorexClientEffects.tryPlayDetonationSound(this);
        }

        if (this.tickCount < 200) {
            lifetime *= s;
            for (int i = 0; i < 2; i++) {
                Cloudlet cloud = new Cloudlet(getX(), getY() + coreHeight, getZ(),
                        (float) (visualRandom.nextDouble() * 2D * Math.PI), 0, lifetime, TorexType.RING);
                cloud.setScale((float) (StrictMath.sqrt(s) * cs + this.tickCount * 0.0015 * s),
                        (float) (StrictMath.sqrt(s) * cs + this.tickCount * 0.0015 * 6 * cs * s));
                cloudlets.add(cloud);
            }
        }

        if (this.humidity > 0 && this.tickCount < 220) {
            spawnCondensationClouds(this.tickCount, this.humidity, firstCondenseHeight, 80, 4, s, cs);
            spawnCondensationClouds(this.tickCount, this.humidity, secondCondenseHeight, 80, 2, s, cs);
        }

        for (int i = cloudlets.size() - 1; i >= 0; i--) {
            Cloudlet cloud = cloudlets.get(i);
            if (cloud.isDead) cloudlets.remove(i);
            else cloud.update();
        }

        coreHeight += 0.15;
        torusWidth += 0.05;
        rollerSize = torusWidth * 0.35;
        convectionHeight = coreHeight + rollerSize;
        int maxHeat = (int) (50 * s * s);
        heat = maxHeat - StrictMath.pow((maxHeat * this.tickCount) / maxAge, 0.6);
    }

    private int replaySpawnTarget() {
        int terrainChangeAge = this.entityData.get(TERRAIN_CHANGE_AGE);
        int initial = this.entityData.get(INITIAL_SPAWN_TARGET);
        int target = terrainChangeAge < 0 || this.tickCount < terrainChangeAge
                ? initial : this.entityData.get(FINAL_SPAWN_TARGET);
        if (clientHistoricalTimeline && target != Integer.MIN_VALUE) return target;
        return Math.max(level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(getX()), (int) Math.floor(getZ())) - 3, 1);
    }

    private int replaySurfaceHeight(int x, int z) {
        int terrainChangeAge = this.entityData.get(TERRAIN_CHANGE_AGE);
        int initial = this.entityData.get(INITIAL_SPAWN_TARGET);
        int target = terrainChangeAge < 0 || this.tickCount < terrainChangeAge
                ? initial : this.entityData.get(FINAL_SPAWN_TARGET);
        if (clientHistoricalTimeline && target != Integer.MIN_VALUE) return target + 3;
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** Called from both tick and renderer extraction so no age-zero frame can be submitted on retrack. */
    public void prepareClientAnimation() {
        if (!level().isClientSide()) return;
        ensureVisualRandom();
        clientHistoricalTimeline = true;
        int serverAge = authoritativeAnimationAge();
        long timelineFingerprint = timelineFingerprint();
        boolean timelineChanged = clientTimelineFingerprint != Long.MIN_VALUE
                && clientTimelineFingerprint != timelineFingerprint;
        clientTimelineFingerprint = timelineFingerprint;
        if (timelineChanged) {
            beginClientReplay(serverAge);
            clientAgeAligned = true;
            return;
        }
        if (!clientAgeAligned) {
            boolean restored = restoreClientAnimation();
            if (!restored && serverAge > 0) {
                beginClientReplay(serverAge);
            } else if (restored && serverAge > this.tickCount) {
                beginClientCatchup(serverAge);
            }
            clientAgeAligned = true;
            return;
        }
    }

    /** First-time tracking has no local snapshot, so prepare a silent bounded replay. */
    private void beginClientReplay(int age) {
        this.coreHeight = 3D;
        this.convectionHeight = 3D;
        this.torusWidth = 3D;
        this.rollerSize = 1D;
        this.heat = 1D;
        this.lastSpawnY = -1D;
        this.maxAge = 1000;
        this.humidity = -1F;
        this.tickCount = 0;
        this.clientScaleInitialized = false;
        this.cloudlets.clear();
        this.nextCloudletOrdinal = 0L;
        this.lastRenderSortTick = Integer.MIN_VALUE;
        this.lastRenderSortCameraX = Double.NaN;
        this.lastRenderSortCameraY = Double.NaN;
        this.lastRenderSortCameraZ = Double.NaN;
        resetVisualRandom();
        this.clientHistoricalTimeline = true;
        this.clientReplayTargetAge = age;
        this.clientAnimationReady = false;
    }

    private void beginClientCatchup(int age) {
        this.clientReplayTargetAge = age;
        this.clientAnimationReady = false;
        this.clientHistoricalTimeline = true;
    }

    private boolean advanceClientReplay() {
        if (clientReplayTargetAge < 0) return true;
        clientReplayTargetAge = Math.max(clientReplayTargetAge, authoritativeAnimationAge());
        long deadline = System.nanoTime() + CLIENT_REPLAY_BUDGET_NANOS;
        int steps = 0;
        do {
            simulateClientTick(false);
            this.tickCount++;
            steps++;
        } while (this.tickCount < clientReplayTargetAge
                && (steps < 2 || System.nanoTime() < deadline));
        clientReplayTargetAge = Math.max(clientReplayTargetAge, authoritativeAnimationAge());
        if (this.tickCount < clientReplayTargetAge) return false;

        clientReplayTargetAge = -1;
        clientAnimationReady = true;
        if (authoritativeAnimationAge() >= 150) {
            this.didPlaySound = true;
            this.didShake = true;
        }
        return false;
    }

    public boolean isClientAnimationReady() {
        return clientAnimationReady;
    }

    private void cacheClientAnimationIfNeeded() {
        if (clientAnimationCached || !level().isClientSide()
                || !clientAnimationReady || this.tickCount > this.maxAge || this.cloudlets.isEmpty()) return;
        clientAnimationCached = true;
        cacheClientAnimation();
    }

    private void cacheClientAnimation() {
        long now = System.currentTimeMillis();
        CLIENT_ANIMATIONS.entrySet().removeIf(entry -> now - entry.getValue().cachedAt() > CLIENT_CACHE_MILLIS);
        ArrayList<CloudletState> states = new ArrayList<>(cloudlets.size());
        for (Cloudlet cloud : cloudlets) {
            states.add(new CloudletState(cloud.posX, cloud.posY, cloud.posZ,
                    cloud.prevPosX, cloud.prevPosY, cloud.prevPosZ,
                    cloud.motionX, cloud.motionY, cloud.motionZ,
                    cloud.age, cloud.cloudletLife, cloud.angle, cloud.isDead, cloud.rangeMod,
                    cloud.colorMod, cloud.colorR, cloud.colorG, cloud.colorB,
                    cloud.prevColorR, cloud.prevColorG, cloud.prevColorB,
                    cloud.type, cloud.startingScale, cloud.growingScale, cloud.motionMult,
                    cloud.creationOrdinal));
        }
        CachedAnimation cached = new CachedAnimation(now, coreHeight, convectionHeight,
                torusWidth, rollerSize, heat, lastSpawnY, tickCount, maxAge, humidity,
                didPlaySound, didShake, visualRandom.state, visualRandom.hasGaussian,
                visualRandom.gaussian, nextCloudletOrdinal,
                timelineFingerprint(), states);
        UUID id = getUUID();
        CLIENT_ANIMATIONS.put(id, cached);
        long token = cached.cachedAt();
        CompletableFuture.delayedExecutor(CLIENT_CACHE_MILLIS, TimeUnit.MILLISECONDS)
                .execute(() -> CLIENT_ANIMATIONS.computeIfPresent(id,
                        (key, value) -> value.cachedAt() == token ? null : value));
    }

    private boolean restoreClientAnimation() {
        UUID id = getUUID();
        CachedAnimation cached = CLIENT_ANIMATIONS.get(id);
        if (cached == null) return false;
        if (System.currentTimeMillis() - cached.cachedAt() > CLIENT_CACHE_MILLIS) {
            CLIENT_ANIMATIONS.remove(id, cached);
            return false;
        }
        if (cached.timelineFingerprint() != timelineFingerprint()) return false;
        CLIENT_ANIMATIONS.remove(id, cached);

        this.coreHeight = cached.coreHeight();
        this.convectionHeight = cached.convectionHeight();
        this.torusWidth = cached.torusWidth();
        this.rollerSize = cached.rollerSize();
        this.heat = cached.heat();
        this.lastSpawnY = cached.lastSpawnY();
        this.tickCount = cached.animationAge();
        this.maxAge = cached.maxAge();
        this.clientScaleInitialized = true;
        this.humidity = cached.humidity();
        this.didPlaySound = cached.didPlaySound();
        this.didShake = cached.didShake();
        this.nextCloudletOrdinal = cached.nextCloudletOrdinal();
        this.clientHistoricalTimeline = true;
        this.cloudlets.clear();

        for (CloudletState state : cached.cloudlets()) {
            this.cloudlets.add(new Cloudlet(state));
        }
        visualRandom.restore(cached.randomState(), cached.hasGaussian(), cached.gaussian());
        return true;
    }

    /** Claims the source chunk before a spawning parent releases its own ticket. */
    public void startChunkLoading() {
        chunkLoader.tick();
    }

    public boolean isChunkLoading() {
        return chunkLoader.isLoading();
    }

    public void markTerrainChanged() {
        if (!level().isClientSide() && this.entityData.get(TERRAIN_CHANGE_AGE) < 0) {
            this.entityData.set(TERRAIN_CHANGE_AGE, this.tickCount);
        }
    }

    public int getTerrainChangeAge() {
        return this.entityData.get(TERRAIN_CHANGE_AGE);
    }

    private void detectTerrainChange() {
        if (this.entityData.get(TERRAIN_CHANGE_AGE) >= 0) return;
        int initial = this.entityData.get(INITIAL_SPAWN_TARGET);
        if (initial == Integer.MIN_VALUE) return;
        int current = Math.max(level().getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(getX()), (int) Math.floor(getZ())) - 3, 1);
        if (current != initial) {
            this.entityData.set(FINAL_SPAWN_TARGET, current);
            markTerrainChanged();
        }
    }

    private void ensureVisualRandom() {
        if (visualRandom != null) return;
        resetVisualRandom();
    }

    private void resetVisualRandom() {
        long seed = Integer.toUnsignedLong(this.entityData.get(RANDOM_SEED_LOW))
                | ((long) this.entityData.get(RANDOM_SEED_HIGH) << 32);
        visualRandom = new VisualRandom(seed);
    }

    private int authoritativeAnimationAge() {
        long start = Integer.toUnsignedLong(this.entityData.get(START_TIME_LOW))
                | ((long) this.entityData.get(START_TIME_HIGH) << 32);
        long elapsed = Math.max(0L, level().getGameTime() - start);
        long age = Math.max(this.entityData.get(ANIMATION_AGE), elapsed);
        return (int) Math.min(age, (int) (45 * 20 * getScale()));
    }

    private long timelineFingerprint() {
        long hash = 0xcbf29ce484222325L;
        hash = (hash ^ Float.floatToRawIntBits(this.entityData.get(SCALE))) * 0x100000001b3L;
        hash = (hash ^ this.entityData.get(TYPE)) * 0x100000001b3L;
        hash = (hash ^ this.entityData.get(INITIAL_SPAWN_TARGET)) * 0x100000001b3L;
        hash = (hash ^ this.entityData.get(FINAL_SPAWN_TARGET)) * 0x100000001b3L;
        hash = (hash ^ this.entityData.get(TERRAIN_CHANGE_AGE)) * 0x100000001b3L;
        hash = (hash ^ Float.floatToRawIntBits(this.entityData.get(HUMIDITY))) * 0x100000001b3L;
        hash = (hash ^ this.entityData.get(RANDOM_SEED_LOW)) * 0x100000001b3L;
        hash = (hash ^ this.entityData.get(RANDOM_SEED_HIGH)) * 0x100000001b3L;
        hash = (hash ^ this.entityData.get(START_TIME_LOW)) * 0x100000001b3L;
        return (hash ^ this.entityData.get(START_TIME_HIGH)) * 0x100000001b3L;
    }

    public void spawnCondensationClouds(int age, float humidity, int height, int count, int spreadAngle, double s, double cs) {
        if ((getY() + age) > height) {

            for (int i = 0; i < (int) (5 * humidity * count / (double) spreadAngle); i++) {
                for (int j = 1; j < spreadAngle; j++) {
                    float angle = (float) (Math.PI * 2 * visualRandom.nextDouble());
                    // vec = (0, age, 0) rotated around Z then around Y
                    double vy = age;
                    double vx = 0;
                    float zRot = (float) StrictMath.acos((height - getY()) / (age)) + (float) StrictMath.toRadians(humidity * humidity * 90 * j * (0.1 * visualRandom.nextDouble() - 0.05));
                    // rotateAroundZ(zRot): x' = x*cos + y*sin ; y' = y*cos - x*sin
                    double zc = StrictMath.cos(zRot);
                    double zs = StrictMath.sin(zRot);
                    double rx = vx * zc + vy * zs;
                    double ry = vy * zc - vx * zs;
                    double rz = 0;
                    // rotateAroundY(angle): x' = x*cos + z*sin ; z' = z*cos - x*sin
                    double yc = StrictMath.cos(angle);
                    double ys = StrictMath.sin(angle);
                    double fx = rx * yc + rz * ys;
                    double fz = rz * yc - rx * ys;
                    Cloudlet cloud = new Cloudlet(getX() + fx, getY() + ry, getZ() + fz, angle, 0, (int) ((20 + age / 10) * (1 + visualRandom.nextDouble() * 0.1)), TorexType.CONDENSATION);
                    cloud.setScale(3F * (float) (cs * s), 4F * (float) (cs * s));
                    cloudlets.add(cloud);
                }
            }
        }
    }

    public EntityNukeTorex setScale(float scale) {
        if (!level().isClientSide())
            this.entityData.set(SCALE, scale);
        this.coreHeight = this.coreHeight * scale;
        this.convectionHeight = this.convectionHeight * scale;
        this.torusWidth = this.torusWidth * scale;
        this.rollerSize = this.rollerSize * scale;
        this.maxAge = (int) (45 * 20 * scale);
        return this;
    }

    public EntityNukeTorex setType(int type) {
        this.entityData.set(TYPE, (byte) type);
        return this;
    }

    public double getScale() {
        return this.entityData.get(SCALE);
    }

    /** The colour scheme: 0 = nuke (orange), 1 = balefire (green). Named to avoid {@code Entity.getType()}. */
    public byte getTorexType() {
        return this.entityData.get(TYPE);
    }

    public double getSimulationSpeed() {

        int simSlow = maxAge / 4;
        int life = this.tickCount;

        if (life > maxAge) {
            return 0D;
        }

        if (life > simSlow) {
            return 1D - ((double) (life - simSlow) / (double) (maxAge - simSlow));
        }

        return 1.0D;
    }

    public float getAlpha() {

        int fadeOut = maxAge * 3 / 4;
        int life = this.tickCount;

        if (life > fadeOut) {
            float fac = (float) (life - fadeOut) / (float) (maxAge - fadeOut);
            return 1F - fac;
        }

        return 1.0F;
    }

    public class Cloudlet {

        public double posX;
        public double posY;
        public double posZ;
        public double prevPosX;
        public double prevPosY;
        public double prevPosZ;
        public double motionX;
        public double motionY;
        public double motionZ;
        public int age;
        public int cloudletLife;
        public float angle;
        public boolean isDead = false;
        float rangeMod = 1.0F;
        public float colorMod = 1.0F;
        public double colorR;
        public double colorG;
        public double colorB;
        public double prevColorR;
        public double prevColorG;
        public double prevColorB;
        public double renderSortDistanceSq;
        public TorexType type;
        public float startingScale = 3F;
        public float growingScale = 5F;
        private long creationOrdinal;
        private double computedMotionX;
        private double computedMotionY;
        private double computedMotionZ;

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge) {
            this(posX, posY, posZ, angle, age, maxAge, TorexType.STANDARD);
        }

        public Cloudlet(double posX, double posY, double posZ, float angle, int age, int maxAge, TorexType type) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.age = age;
            this.cloudletLife = maxAge;
            this.angle = angle;
            this.rangeMod = 0.3F + visualRandom.nextFloat() * 0.7F;
            this.colorMod = 0.8F + visualRandom.nextFloat() * 0.2F;
            this.type = type;
            this.creationOrdinal = nextCloudletOrdinal++;

            this.updateColor();
        }

        private Cloudlet(CloudletState state) {
            this.posX = state.posX();
            this.posY = state.posY();
            this.posZ = state.posZ();
            this.prevPosX = state.prevPosX();
            this.prevPosY = state.prevPosY();
            this.prevPosZ = state.prevPosZ();
            this.motionX = state.motionX();
            this.motionY = state.motionY();
            this.motionZ = state.motionZ();
            this.age = state.age();
            this.cloudletLife = state.life();
            this.angle = state.angle();
            this.isDead = state.dead();
            this.rangeMod = state.rangeMod();
            this.colorMod = state.colorMod();
            this.colorR = state.colorR();
            this.colorG = state.colorG();
            this.colorB = state.colorB();
            this.prevColorR = state.prevColorR();
            this.prevColorG = state.prevColorG();
            this.prevColorB = state.prevColorB();
            this.type = state.type();
            this.startingScale = state.startingScale();
            this.growingScale = state.growingScale();
            this.motionMult = state.motionMult();
            this.creationOrdinal = state.creationOrdinal();
        }

        public long getCreationOrdinal() {
            return creationOrdinal;
        }

        private double motionMult = 1F;
        private double motionConvectionMult = 0.5F;
        private double motionLiftMult = 0.625F;
        private double motionRingMult = 0.5F;
        private double motionCondensationMult = 1F;
        private double motionShockwaveMult = 1F;

        private void update() {
            age++;

            if (age > cloudletLife) {
                this.isDead = true;
            }

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            double simDeltaX = EntityNukeTorex.this.getX() - this.posX;
            double simDeltaZ = EntityNukeTorex.this.getZ() - this.posZ;
            double simPosX = EntityNukeTorex.this.getX() + StrictMath.sqrt(simDeltaX * simDeltaX + simDeltaZ * simDeltaZ);

            if (this.type == TorexType.STANDARD) {
                getConvectionMotion(simPosX);
                double convectionX = this.computedMotionX;
                double convectionY = this.computedMotionY;
                double convectionZ = this.computedMotionZ;
                getLiftMotion(simPosX);

                double factor = Mth.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0, 1);
                double inverseFactor = 1D - factor;
                this.motionX = convectionX * factor + this.computedMotionX * inverseFactor;
                this.motionY = convectionY * factor + this.computedMotionY * inverseFactor;
                this.motionZ = convectionZ * factor + this.computedMotionZ * inverseFactor;
            } else if (this.type == TorexType.RING) {
                getRingMotion(simPosX);
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            } else if (this.type == TorexType.CONDENSATION) {
                getCondensationMotion();
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            } else if (this.type == TorexType.SHOCK) {
                getShockwaveMotion();
                this.motionX = this.computedMotionX;
                this.motionY = this.computedMotionY;
                this.motionZ = this.computedMotionZ;
            }

            double mult = this.motionMult * getSimulationSpeed();

            this.posX += this.motionX * mult;
            this.posY += this.motionY * mult;
            this.posZ += this.motionZ * mult;

            this.updateColor();
        }

        private void getCondensationMotion() {
            double speed = motionCondensationMult * EntityNukeTorex.this.getScale() * 0.125D;
            setNormalizedMotion(this.posX - EntityNukeTorex.this.getX(), 0D, this.posZ - EntityNukeTorex.this.getZ(), speed);
        }

        private void getShockwaveMotion() {
            double speed = motionShockwaveMult * EntityNukeTorex.this.getScale() * 0.25D;
            setNormalizedMotion(this.posX - EntityNukeTorex.this.getX(), 0D, this.posZ - EntityNukeTorex.this.getZ(), speed);
        }

        private void getRingMotion(double simPosX) {

            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double torusPosX = EntityNukeTorex.this.getX() + torusWidth;
            double torusPosY = EntityNukeTorex.this.getY() + coreHeight * 0.5D;

            double deltaX = torusPosX - simPosX;
            double deltaY = torusPosY - this.posY;

            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod * 0.25D;
            double dist = StrictMath.sqrt(deltaX * deltaX + deltaY * deltaY) / roller - 1D;

            double func = 1D - StrictMath.exp(-dist);
            float angle = (float) (func * Math.PI * 0.5D); // [0;90°]

            double rotX = -deltaX / dist;
            double rotY = -deltaY / dist;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            double rotatedX = rotX * cos + rotY * sin;
            double rotatedY = rotY * cos - rotX * sin;

            setNormalizedMotion(torusPosX + rotatedX - simPosX, torusPosY + rotatedY - this.posY, 0D, motionRingMult * 0.5D);
            rotateComputedMotionAroundY();
        }

        /* simulated on a 2D-plane along the X/Y axis */
        private void getConvectionMotion(double simPosX) {

            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double torusPosX = EntityNukeTorex.this.getX() + torusWidth;
            double torusPosY = EntityNukeTorex.this.getY() + coreHeight;

            double deltaX = torusPosX - simPosX;
            double deltaY = torusPosY - this.posY;

            double roller = EntityNukeTorex.this.rollerSize * this.rangeMod;
            double dist = StrictMath.sqrt(deltaX * deltaX + deltaY * deltaY) / roller - 1D;

            double func = 1D - StrictMath.exp(-dist);
            float angle = (float) (func * Math.PI * 0.5D); // [0;90°]

            double rotX = -deltaX / dist;
            double rotY = -deltaY / dist;
            float sin = Mth.sin(angle);
            float cos = Mth.cos(angle);
            double rotatedX = rotX * cos + rotY * sin;
            double rotatedY = rotY * cos - rotX * sin;

            setNormalizedMotion(torusPosX + rotatedX - simPosX, torusPosY + rotatedY - this.posY, 0D, motionConvectionMult);
            rotateComputedMotionAroundY();
        }

        private void getLiftMotion(double simPosX) {
            double scale = Mth.clamp(1D - (simPosX - (EntityNukeTorex.this.getX() + torusWidth)), 0, 1) * motionLiftMult;

            setNormalizedMotion(
                    EntityNukeTorex.this.getX() - this.posX,
                    (EntityNukeTorex.this.getY() + convectionHeight) - this.posY,
                    EntityNukeTorex.this.getZ() - this.posZ,
                    scale);
        }

        private void setComputedMotion(double x, double y, double z) {
            this.computedMotionX = x;
            this.computedMotionY = y;
            this.computedMotionZ = z;
        }

        private void setNormalizedMotion(double x, double y, double z, double speed) {
            double lengthSq = x * x + y * y + z * z;
            if (lengthSq < 1.0E-8D) {
                setComputedMotion(0D, 0D, 0D);
                return;
            }

            double scale = speed / StrictMath.sqrt(lengthSq);
            setComputedMotion(x * scale, y * scale, z * scale);
        }

        private void rotateComputedMotionAroundY() {
            float cos = Mth.cos(this.angle);
            float sin = Mth.sin(this.angle);
            double motionX = this.computedMotionX;
            double motionZ = this.computedMotionZ;
            this.computedMotionX = motionX * cos + motionZ * sin;
            this.computedMotionZ = motionZ * cos - motionX * sin;
        }

        private void updateColor() {
            this.prevColorR = this.colorR;
            this.prevColorG = this.colorG;
            this.prevColorB = this.colorB;

            double exX = EntityNukeTorex.this.getX();
            double exY = EntityNukeTorex.this.getY() + EntityNukeTorex.this.coreHeight;
            double exZ = EntityNukeTorex.this.getZ();

            double distX = exX - posX;
            double distY = exY - posY;
            double distZ = exZ - posZ;

            double distSq = distX * distX + distY * distY + distZ * distZ;
            distSq /= this.type == TorexType.SHOCK ? EntityNukeTorex.this.heat * 3 : EntityNukeTorex.this.heat;

            double col = 2D / Math.max(distSq, 1); // col goes from 2-0

            byte type = EntityNukeTorex.this.getTorexType();
            if (type == 0) {
                this.colorR = nr2 + (nr1 - nr2) * col;
                this.colorG = ng2 + (ng1 - ng2) * col;
                this.colorB = nb2 + (nb1 - nb2) * col;
                return;
            }

            this.colorR = br2 + (br1 - br2) * col;
            this.colorG = bg2 + (bg1 - bg2) * col;
            this.colorB = bb2 + (bb1 - bb2) * col;
        }

        public float getAlpha() {
            float alpha = (1F - ((float) age / (float) cloudletLife)) * EntityNukeTorex.this.getAlpha();
            if (this.type == TorexType.CONDENSATION) alpha *= 0.25;
            return Mth.clamp(alpha, 0.0001F, 1F);
        }

        public float getScale() {
            return startingScale + ((float) age / (float) cloudletLife) * growingScale;
        }

        public Cloudlet setScale(float start, float grow) {
            this.startingScale = start;
            this.growingScale = grow;
            return this;
        }

        public Cloudlet setMotion(double mult) {
            this.motionMult = mult;
            return this;
        }
    }

    public enum TorexType {
        STANDARD,
        RING,
        CONDENSATION,
        SHOCK
    }

    /** Builds (but does not spawn) a standard Torex; the caller adds it to the level. */
    public static EntityNukeTorex statFac(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), level).setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F));
        torex.setPos(x, y, z);
        if (!level.isClientSide()) {
            torex.captureEnvironment(level, x, y, z);
            torex.setVisualSeed(level.getRandom().nextLong());
        }
        return torex;
    }

    /** Builds (but does not spawn) a balefire Torex; the caller adds it to the level. */
    public static EntityNukeTorex statFacBale(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), level).setScale(Mth.clamp(scale * 0.01F, 0.25F, 5F)).setType(1);
        torex.setPos(x, y, z);
        if (!level.isClientSide()) {
            torex.captureEnvironment(level, x, y, z);
            torex.setVisualSeed(level.getRandom().nextLong());
        }
        return torex;
    }

    private void setVisualSeed(long seed) {
        this.entityData.set(RANDOM_SEED_LOW, (int) seed);
        this.entityData.set(RANDOM_SEED_HIGH, (int) (seed >>> 32));
    }

    private void captureEnvironment(Level level, double x, double y, double z) {
        int target = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(x), (int) Math.floor(z)) - 3, 1);
        this.entityData.set(INITIAL_SPAWN_TARGET, target);
        this.entityData.set(FINAL_SPAWN_TARGET, target);
        boolean precipitation = level.getBiome(BlockPos.containing(x, y, z)).value().hasPrecipitation();
        this.entityData.set(HUMIDITY, precipitation ? 0.5F : 0F);
        long start = level.getGameTime();
        this.entityData.set(START_TIME_LOW, (int) start);
        this.entityData.set(START_TIME_HIGH, (int) (start >>> 32));
    }

    private static final class VisualRandom {
        private static final long MULTIPLIER = 0x5DEECE66DL;
        private static final long ADDEND = 0xBL;
        private static final long MASK = (1L << 48) - 1;
        private long state;
        private boolean hasGaussian;
        private double gaussian;

        VisualRandom(long seed) {
            this.state = (seed ^ MULTIPLIER) & MASK;
        }

        private int next(int bits) {
            state = (state * MULTIPLIER + ADDEND) & MASK;
            return (int) (state >>> (48 - bits));
        }

        float nextFloat() {
            return next(24) / ((float) (1 << 24));
        }

        double nextDouble() {
            return (((long) next(26) << 27) + next(27)) * 0x1.0p-53;
        }

        double nextGaussian() {
            if (hasGaussian) {
                hasGaussian = false;
                return gaussian;
            }
            double x;
            double y;
            double radius;
            do {
                x = 2D * nextDouble() - 1D;
                y = 2D * nextDouble() - 1D;
                radius = x * x + y * y;
            } while (radius >= 1D || radius == 0D);
            double scale = StrictMath.sqrt(-2D * StrictMath.log(radius) / radius);
            gaussian = y * scale;
            hasGaussian = true;
            return x * scale;
        }

        void restore(long state, boolean hasGaussian, double gaussian) {
            this.state = state;
            this.hasGaussian = hasGaussian;
            this.gaussian = gaussian;
        }
    }
}
