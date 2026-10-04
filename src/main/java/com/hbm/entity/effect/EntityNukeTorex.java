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
 * Toroidal-convection mushroom-cloud effect entity (<b>Tor</b>oidal Convection Simulation <b>Ex</b>plosion),
 * 1.7's {@code EntityNukeTorex}: the entity is invisible server-side (it only ages out at
 * {@link #getMaxAge}); the visual simulation runs <em>client-side</em> in {@link #tick()}, spawning the
 * {@link Cloudlet} billboards that {@code TorexRenderer} draws.
 *
 * <p>Cloudlet kinds ({@link TorexType}): STANDARD (the rising torus of convection cells), SHOCK (the
 * ground shockwave ring, first 150 ticks), RING (the collar, first 195 ticks), and CONDENSATION (the two
 * condensation shells, ticks 195..900 and 300..900).
 *
 * <p><b>1.7, not 1.12.2.</b> 1.7 simulates every cloud at the fixed scale 1.5 and maps the cloudlets out
 * by the synced scale when drawing; the scale is {@code clamp(squirt(r * 0.01) * 1.5, 0.5, 5)} ({@link #scaleFor}). 1.12.2 scaled
 * the simulation itself by {@code clamp(r * 0.01, 0.25, 5)} and reworked spawning, colours, the shockwave and
 * the humidity-gated condensation; that version is gone (History section 19ej-1d).
 *
 * <p>Client-only interactions (the sky flash, the detonation sound) are routed through
 * {@link TorexClientEffects} so a dedicated server never resolves {@code net.minecraft.client} symbols.
 * The scale, colour type, server animation age and visual seed ride {@link SynchedEntityData}. Not saved to
 * disk ({@link #shouldBeSaved()}).
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
    private boolean benchmarkFixture;

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

    /**
     * 1.7 EntityNukeTorex.onUpdate, client half. The simulation always runs at a fixed scale of 1.5
     * ({@code s = cs = 1.5}; 1.7 commented out {@code this.getScale()} there); the synced scale only sets
     * the lifetime and maps cloudlets out at render time.
     */
    private void simulateClientTick(boolean playEffects) {
        double s = 1.5; // 1.7: "1.5; //this.getScale();"
        double cs = 1.5;
        this.maxAge = getMaxAge();

        if (lastSpawnY == -1) lastSpawnY = getY() - 3;

        if (playEffects && this.tickCount < 100) TorexClientEffects.skyFlash(level());

        int spawnTarget = replaySpawnTarget();
        double moveSpeed = 0.5D;
        if (Math.abs(spawnTarget - lastSpawnY) < moveSpeed) lastSpawnY = spawnTarget;
        else lastSpawnY += moveSpeed * Math.signum(spawnTarget - lastSpawnY);

        // spawn mush clouds
        double range = (torusWidth - rollerSize) * 0.25;
        double simSpeed = getSimulationSpeed();
        int toSpawn = (int) Math.ceil(10 * simSpeed * simSpeed);
        int lifetime = Math.min((this.tickCount * this.tickCount) + 200, maxAge - this.tickCount + 200);

        for (int i = 0; i < toSpawn; i++) {
            double x = getX() + visualRandom.nextGaussian() * range;
            double z = getZ() + visualRandom.nextGaussian() * range;
            Cloudlet cloud = new Cloudlet(x, lastSpawnY, z, (float) (visualRandom.nextDouble() * 2D * Math.PI), 0, lifetime);
            cloud.setScale(1F + this.tickCount * 0.005F * (float) cs, 5F * (float) cs);
            cloudlets.add(cloud);
        }

        // spawn shock clouds
        if (this.tickCount < 150) {
            int cloudCount = this.tickCount * 5;
            int shockLife = Math.max(300 - this.tickCount * 20, 50);

            for (int i = 0; i < cloudCount; i++) {
                double vx = (this.tickCount * 1.5 + visualRandom.nextDouble()) * 1.5;
                float rot = (float) (Math.PI * 2 * visualRandom.nextDouble());
                double[] vec = rotateAroundY(vx, 0, 0, rot);
                this.cloudlets.add(new Cloudlet(vec[0] + getX(),
                        replaySurfaceHeight((int) (vec[0] + getX()) + 1, (int) (vec[2] + getZ())),
                        vec[2] + getZ(), rot, 0, shockLife, TorexType.SHOCK)
                        .setScale(7F, 2F)
                        .setMotion(this.tickCount > 15 ? 0.75 : 0));
            }

            if (playEffects && !didPlaySound) didPlaySound = TorexClientEffects.tryPlayDetonationSound(this);
        }

        // spawn ring clouds
        if (this.tickCount < 130 * s) {
            lifetime *= s;
            for (int i = 0; i < 2; i++) {
                Cloudlet cloud = new Cloudlet(getX(), getY() + coreHeight, getZ(),
                        (float) (visualRandom.nextDouble() * 2D * Math.PI), 0, lifetime, TorexType.RING);
                cloud.setScale(1F + this.tickCount * 0.0025F * (float) (cs * cs), 3F * (float) (cs * cs));
                cloudlets.add(cloud);
            }
        }

        // spawn condensation clouds
        if (this.tickCount > 130 * s && this.tickCount < 600 * s) {
            spawnCondensation(5, 1, -5, s, cs);
        }
        if (this.tickCount > 200 * s && this.tickCount < 600 * s) {
            spawnCondensation(3, 0.5, 25, cs, cs);
        }

        for (Cloudlet cloud : cloudlets) {
            cloud.update();
        }
        coreHeight += 0.15 / s;
        torusWidth += 0.05 / s;
        rollerSize = torusWidth * 0.35;
        convectionHeight = coreHeight + rollerSize;

        int maxHeat = (int) (50 * cs);
        // 1.7: Math.pow((maxHeat * ticksExisted) / maxAge, 1), an INT division, so the heat falls in steps
        heat = maxHeat - Math.pow((maxHeat * this.tickCount) / maxAge, 1);

        cloudlets.removeIf(x -> x.isDead);
    }

    /**
     * 1.7's two condensation shells: 20 x 4 cloudlets per tick on a ring {@code rollerBase} roller sizes
     * (plus up to {@code rollerRandom}) outside the torus, tilted pi/45 per row, {@code yOffset} above the
     * core head with each row {@code rowStep} higher.
     */
    private void spawnCondensation(double rollerBase, double rollerRandom, double yOffset, double rowStep, double cs) {
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 4; j++) {
                float angle = (float) (Math.PI * 2 * visualRandom.nextDouble());
                double[] vec = rotateAroundZ(torusWidth + rollerSize * (rollerBase + visualRandom.nextDouble() * rollerRandom),
                        0, 0, (float) (Math.PI / 45 * j));
                vec = rotateAroundY(vec[0], vec[1], vec[2], angle);
                Cloudlet cloud = new Cloudlet(getX() + vec[0], getY() + coreHeight + yOffset + j * rowStep,
                        getZ() + vec[2], angle, 0,
                        (int) ((20 + this.tickCount / 10) * (1 + visualRandom.nextDouble() * 0.1)), TorexType.CONDENSATION);
                cloud.setScale(0.125F * (float) (cs), 3F * (float) (cs));
                cloudlets.add(cloud);
            }
        }
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

    /**
     * 1.7 setScale also rescaled the geometry by {@code scale / 1.5}, but only on the side that called it:
     * the server, whose geometry nothing reads. The client simulation never rescales (its tick-1
     * {@code setScale(1.5)} is a no-op), so here only the synced value and the lifetime change.
     */
    public EntityNukeTorex setScale(float scale) {
        if (!level().isClientSide())
            this.entityData.set(SCALE, scale);
        this.maxAge = (int) (45 * 20 * scale);
        return this;
    }

    /** 1.7 getMaxAge: 45 seconds per unit of synced scale. */
    public int getMaxAge() {
        return (int) (45 * 20 * getScale());
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

    /** 1.7 getSimulationSpeed: full speed for a quarter of the life, slowing to a stop at half. */
    public double getSimulationSpeed() {

        int lifetime = getMaxAge();
        int simSlow = lifetime / 4;
        int simStop = lifetime / 2;
        int life = this.tickCount;

        if (life > simStop) {
            return 0D;
        }

        if (life > simSlow) {
            return 1D - ((double) (life - simSlow) / (double) (simStop - simSlow));
        }

        return 1.0D;
    }

    /** 1.7 getGreying: 1 until three quarters of the life, then rising to 2. */
    public double getGreying() {

        int lifetime = getMaxAge();
        int greying = lifetime * 3 / 4;

        if (this.tickCount > greying) {
            return 1 + ((double) (this.tickCount - greying) / (double) (lifetime - greying));
        }

        return 1D;
    }

    public float getAlpha() {

        int lifetime = getMaxAge();
        int fadeOut = lifetime * 3 / 4;
        int life = this.tickCount;

        if (life > fadeOut) {
            float fac = (float) (life - fadeOut) / (float) (lifetime - fadeOut);
            return 1F - fac;
        }

        return 1.0F;
    }

    /**
     * 1.7 {@code EntityNukeTorex.Cloudlet}. Positions live in the fixed scale-1.5 simulation space; the
     * renderer maps them out by the synced scale ({@link #getInterpX}), except the ground shockwave.
     */
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
        public float startingScale = 1F;
        public float growingScale = 5F;
        private long creationOrdinal;
        private double motionMult = 1F;

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
            // 1.7 leaves prevColor null until the first update; the renderer never sees a cloudlet before it
            this.prevColorR = this.colorR;
            this.prevColorG = this.colorG;
            this.prevColorB = this.colorB;
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
            double simPosX = EntityNukeTorex.this.getX() + length(simDeltaX, 0D, simDeltaZ);
            double simPosZ = EntityNukeTorex.this.getZ();

            if (this.type == TorexType.STANDARD) {
                double[] convection = getConvectionMotion(simPosX, simPosZ);
                double[] lift = getLiftMotion(simPosX);

                double factor = Mth.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0, 1);
                this.motionX = convection[0] * factor + lift[0] * (1D - factor);
                this.motionY = convection[1] * factor + lift[1] * (1D - factor);
                this.motionZ = convection[2] * factor + lift[2] * (1D - factor);
            } else if (this.type == TorexType.SHOCK) {
                double factor = Mth.clamp((this.posY - EntityNukeTorex.this.getY()) / EntityNukeTorex.this.coreHeight, 0, 1);
                double[] motion = rotateAroundY(1, 0, 0, this.angle);
                this.motionX = motion[0] * factor;
                this.motionY = motion[1] * factor;
                this.motionZ = motion[2] * factor;
            } else if (this.type == TorexType.RING) {
                double[] motion = getRingMotion(simPosX, simPosZ);
                this.motionX = motion[0];
                this.motionY = motion[1];
                this.motionZ = motion[2];
            } else if (this.type == TorexType.CONDENSATION) {
                // 1.7 getCondensationMotion: the horizontal offset, scaled (not normalized) by 0.00002 * age
                double speed = 0.00002 * EntityNukeTorex.this.tickCount;
                this.motionX = (posX - EntityNukeTorex.this.getX()) * speed;
                this.motionY = 0;
                this.motionZ = (posZ - EntityNukeTorex.this.getZ()) * speed;
            }

            double mult = this.motionMult * getSimulationSpeed();

            this.posX += this.motionX * mult;
            this.posY += this.motionY * mult;
            this.posZ += this.motionZ * mult;

            this.updateColor();
        }

        /** 1.7 getRingMotion: steer toward the torus' half-height ring; beyond twice the torus width, rest. */
        private double[] getRingMotion(double simPosX, double simPosZ) {

            if (simPosX > EntityNukeTorex.this.getX() + torusWidth * 2) return new double[3];

            double torusX = EntityNukeTorex.this.getX() + torusWidth;
            double torusY = EntityNukeTorex.this.getY() + coreHeight * 0.5;
            double torusZ = EntityNukeTorex.this.getZ();

            double[] motion = torusTarget(torusX, torusY, torusZ, simPosX, simPosZ,
                    EntityNukeTorex.this.rollerSize * this.rangeMod * 0.25);
            // 1.7 scales by 0.001 and then normalizes, so only the direction survives
            motion = normalize(motion[0] * 0.001D, motion[1] * 0.001D, motion[2] * 0.001D);
            return rotateAroundY(motion[0], motion[1], motion[2], this.angle);
        }

        /** 1.7 getConvectionMotion, simulated on the X/Y plane and rotated back by the cloudlet's angle. */
        private double[] getConvectionMotion(double simPosX, double simPosZ) {

            double torusX = EntityNukeTorex.this.getX() + torusWidth;
            double torusY = EntityNukeTorex.this.getY() + coreHeight;
            double torusZ = EntityNukeTorex.this.getZ();

            double[] motion = torusTarget(torusX, torusY, torusZ, simPosX, simPosZ,
                    EntityNukeTorex.this.rollerSize * this.rangeMod);
            motion = normalize(motion[0], motion[1], motion[2]);
            return rotateAroundY(motion[0], motion[1], motion[2], this.angle);
        }

        /**
         * The shared middle of 1.7's two torus motions: the vector from the cloudlet to the point on the
         * roller's perimeter it is steering for. {@code rot} is {@code -delta / dist}, divided rather than
         * normalized, exactly as 1.7.
         */
        private double[] torusTarget(double torusX, double torusY, double torusZ, double simPosX, double simPosZ,
                                     double roller) {
            double deltaX = torusX - simPosX;
            double deltaY = torusY - this.posY;
            double deltaZ = torusZ - simPosZ;

            double dist = length(deltaX, deltaY, deltaZ) / roller - 1D;

            double func = 1D - Math.pow(Math.E, -dist); // [0;1]
            float angle = (float) (func * Math.PI * 0.5D); // [0;90 degrees]

            double[] rot = rotateAroundZ(-deltaX / dist, -deltaY / dist, -deltaZ / dist, angle);

            return new double[] {
                    torusX + rot[0] - simPosX,
                    torusY + rot[1] - this.posY,
                    torusZ + rot[2] - simPosZ};
        }

        /** 1.7 getLiftMotion: straight at the convection point, fading out past the torus. */
        private double[] getLiftMotion(double simPosX) {
            double scale = Mth.clamp(1D - (simPosX - (EntityNukeTorex.this.getX() + torusWidth)), 0, 1);

            double[] motion = normalize(EntityNukeTorex.this.getX() - this.posX,
                    (EntityNukeTorex.this.getY() + convectionHeight) - this.posY,
                    EntityNukeTorex.this.getZ() - this.posZ);
            return new double[] {motion[0] * scale, motion[1] * scale, motion[2] * scale};
        }

        /** 1.7 updateColor: the heat-scaled distance to the core head, as orange or balefire green. */
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
            distSq /= EntityNukeTorex.this.heat;
            double dist = Math.sqrt(distSq);

            dist = Math.max(dist, 1);
            double col = 2D / dist;

            if (EntityNukeTorex.this.getTorexType() == 1) {
                this.colorR = Math.max(col * 1, 0.25);
                this.colorG = Math.max(col * 2, 0.25);
                this.colorB = Math.max(col * 0.5, 0.25);
            } else {
                this.colorR = Math.max(col * 2, 0.25);
                this.colorG = Math.max(col * 1.5, 0.25);
                this.colorB = Math.max(col * 0.5, 0.25);
            }
        }

        /** 1.7 getInterpPos: simulation space mapped out by the synced scale, the shockwave excepted. */
        public double getInterpX(float interp) {
            return mapOut(prevPosX + (posX - prevPosX) * interp, EntityNukeTorex.this.getX());
        }

        public double getInterpY(float interp) {
            return mapOut(prevPosY + (posY - prevPosY) * interp, EntityNukeTorex.this.getY());
        }

        public double getInterpZ(float interp) {
            return mapOut(prevPosZ + (posZ - prevPosZ) * interp, EntityNukeTorex.this.getZ());
        }

        private double mapOut(double base, double origin) {
            if (this.type == TorexType.SHOCK) return base; // 1.7: rescaling would move the ground ring
            return (base - origin) * (float) EntityNukeTorex.this.getScale() + origin;
        }

        /** 1.7 getInterpColor, one channel: white condensation, otherwise colour times the greying ramp. */
        public double getInterpColor(int channel, float interp) {
            if (this.type == TorexType.CONDENSATION) return 1D;
            double greying = EntityNukeTorex.this.getGreying();
            if (this.type == TorexType.RING) greying += 1;
            double prev = channel == 0 ? prevColorR : channel == 1 ? prevColorG : prevColorB;
            double now = channel == 0 ? colorR : channel == 1 ? colorG : colorB;
            return (prev + (now - prev) * interp) * greying;
        }

        public float getAlpha() {
            float alpha = (1F - ((float) age / (float) cloudletLife)) * EntityNukeTorex.this.getAlpha();
            if (this.type == TorexType.CONDENSATION) alpha *= 0.25;
            return Mth.clamp(alpha, 0.0001F, 1F);
        }

        /** 1.7 getScale: grows over the cloudlet's life, times the synced scale except for the shockwave. */
        public float getScale() {
            float base = startingScale + ((float) age / (float) cloudletLife) * growingScale;
            if (this.type != TorexType.SHOCK) base *= (float) EntityNukeTorex.this.getScale();
            return base;
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

    /** 1.7 Vec3.rotateAroundY (MathHelper float trig). */
    private static double[] rotateAroundY(double x, double y, double z, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        return new double[] {x * cos + z * sin, y, z * cos - x * sin};
    }

    /** 1.7 Vec3.rotateAroundZ (MathHelper float trig). */
    private static double[] rotateAroundZ(double x, double y, double z, float angle) {
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        return new double[] {x * cos + y * sin, y * cos - x * sin, z};
    }

    /** 1.7 Vec3.lengthVector: MathHelper.sqrt_double, a FLOAT square root. */
    private static double length(double x, double y, double z) {
        return (double) (float) Math.sqrt(x * x + y * y + z * z);
    }

    /** 1.7 Vec3.normalize: the zero vector below a length of 1e-4, float square root. */
    private static double[] normalize(double x, double y, double z) {
        double length = length(x, y, z);
        return length < 1.0E-4D ? new double[3] : new double[] {x / length, y / length, z / length};
    }

    public enum TorexType {
        STANDARD,
        RING,
        CONDENSATION,
        SHOCK
    }

    /**
     * 1.7 statFac: {@code clamp((float) squirt(r * 0.01) * 1.5F, 0.5F, 5F)} with BobMathUtil.squirt, a
     * smoothed square root ({@code sqrt(x + 1 / (x + 2)^2) - 1 / (x + 2)}): 1.081 for r = 100.
     */
    public static float scaleFor(float radius) {
        return Mth.clamp((float) squirt(radius * 0.01) * 1.5F, 0.5F, 5F);
    }

    /** 1.7 BobMathUtil.squirt. */
    public static double squirt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }

    /** Builds (but does not spawn) a standard Torex; the caller adds it to the level. */
    public static EntityNukeTorex statFac(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), level).setScale(scaleFor(scale));
        torex.setPos(x, y, z);
        if (!level.isClientSide()) {
            torex.captureEnvironment(level, x, y, z);
            torex.setVisualSeed(level.getRandom().nextLong());
        }
        return torex;
    }

    /** Builds (but does not spawn) a balefire Torex; the caller adds it to the level. */
    public static EntityNukeTorex statFacBale(Level level, double x, double y, double z, float scale) {
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), level).setScale(scaleFor(scale)).setType(1);
        torex.setPos(x, y, z);
        if (!level.isClientSide()) {
            torex.captureEnvironment(level, x, y, z);
            torex.setVisualSeed(level.getRandom().nextLong());
        }
        return torex;
    }

    /** Builds (but does not spawn) a Torex with an exact synced scale, as 1.7's hand-built Little Boy cloud. */
    public static EntityNukeTorex statFacExact(Level level, double x, double y, double z, float scale, int type) {
        EntityNukeTorex torex = new EntityNukeTorex(ModEntities.NUKE_TOREX.get(), level).setScale(scale).setType(type);
        torex.setPos(x, y, z);
        if (!level.isClientSide()) {
            torex.captureEnvironment(level, x, y, z);
            torex.setVisualSeed(level.getRandom().nextLong());
        }
        return torex;
    }

    /** Development-only deterministic cloud used by the client render benchmark. */
    public static EntityNukeTorex benchmarkFac(Level level, double x, double y, double z) {
        return benchmarkFac(level, x, y, z, 1_200);
    }

    /** Development-only deterministic cloud replayed to the requested animation age. */
    public static EntityNukeTorex benchmarkFac(Level level, double x, double y, double z, int age) {
        EntityNukeTorex torex = statFac(level, x, y, z, 500F);
        torex.benchmarkFixture = true;
        if (!level.isClientSide()) {
            torex.tickCount = age;
            torex.entityData.set(ANIMATION_AGE, age);
            long start = level.getGameTime() - age;
            torex.entityData.set(START_TIME_LOW, (int) start);
            torex.entityData.set(START_TIME_HIGH, (int) (start >>> 32));
            torex.setVisualSeed(0x48424D544F524558L);
        }
        return torex;
    }

    public boolean isBenchmarkFixture() {
        return benchmarkFixture;
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
