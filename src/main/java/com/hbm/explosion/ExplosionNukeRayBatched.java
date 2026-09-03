package com.hbm.explosion;

import com.hbm.Constants;
import com.hbm.config.BombConfig;
import com.hbm.interfaces.IExplosionRay;
import com.hbm.util.ChunkCarver;
import dev.golem.core.world.Chunks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * The Fat Man / mk5 crater raytracer, ported from 1.12.2's {@code ExplosionNukeRayBatched} — with the
 * ray phase parallelized to match the wall-clock of the fork's <b>default</b> algorithm
 * ({@code explosionAlgorithm = 2} = the threaded DDA {@code ExplosionNukeRayParallelized}; only this
 * batched spiral is ported, but its compute now fans out the same way).
 *
 * <p><b>Phases:</b>
 * <ol>
 *   <li><b>Chunk acquisition</b> (server thread, time-sliced): seed resident chunks, then request only
 *       chunks reached by rays through bounded loading-only claims. Workers never call {@code Level}
 *       methods and the server thread never waits for generation.</li>
 *   <li><b>Rays</b> (ForkJoin common pool): a coordinator walks the generalized-spiral recurrence
 *       (inherently sequential — each longitude is a prefix sum) and hands direction batches to pool
 *       tasks; each batch marches its rays against the chunk cache into a <em>worker-local</em>
 *       per-chunk {@link BitSet} map, merged by the coordinator. Identical rays/hits to the old
 *       sequential loop, ÷cores wall-clock.</li>
 *   <li><b>Destruction</b> (server thread, time-sliced to the caller's ms budget): walk the bitsets
 *       nearest-chunk-first and clear the blocks — unchanged from the old code.</li>
 * </ol>
 *
 * <p><b>The origin blind spot ("a few blocks always survive under the bomb"):</b> the spiral march
 * samples r = 0, 2, 3, … (the resistance expression rewrites r=0→1, then the loop increments), and no
 * unit ray can floor-sample the cell directly below the origin at r=2 (it needs vx,vz ∈ [0,0.5) and
 * |vy| ≤ 0.5 — impossible for a unit vector). The old mod's default DDA sampled every cell and had no
 * such hole, so the port seeds the 3×3×3 cube around the origin into the destruction set explicitly
 * ({@link #seedOriginClear}).
 *
 * <p>The bit-packing was generalised from the old hard-coded 0..255 column to the level's real height.
 * Deferrals vs 1.12.2: the {@code isWarDim} gate, the DDA algorithms themselves, and the mid-explosion
 * NBT save of in-flight state (a blast interrupted by a save restarts).
 */
public class ExplosionNukeRayBatched implements IExplosionRay {

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    /** Notify clients, skip neighbour shape updates, suppress drops — fast bulk clearing. */
    private static final int CLEAR_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /** Directions per pool task in the ray phase. */
    private static final int RAY_BATCH = 4096;
    /** Maximum chunks being generated concurrently per common-pool worker. */
    private static final int LOAD_WINDOW_PER_WORKER = 48;
    private static final int MAX_LOAD_WINDOW = 512;

    /**
     * Destroyed blocks, as one bitset per chunk keyed by packed chunk position.
     *
     * <p>Long-keyed, not {@code ChunkPos}-keyed: {@link #setBit} runs once per destroyed block — millions
     * of times for a Fat Man — and a {@code ChunkPos} key allocates on every one of them.
     */
    private final Long2ObjectOpenHashMap<BitSet> perChunk = new Long2ObjectOpenHashMap<>();
    private long[] orderedChunks = new long[0];
    private int orderedIndex = 0;
    private volatile boolean isContained = true;
    private final int posX;
    private final int posY;
    private final int posZ;
    private final Level world;

    private final int strength;
    private final int radius;

    /** Inclusive column bounds for this level, used for both the y range test and the bit packing. */
    private final int minY;
    private final int maxY;

    private final int gspNumMax;

    private volatile boolean isAusf3Complete = false;
    private volatile boolean cancelled = false;
    private UUID detonator;

    // --- prefetch state (server thread only) ---
    private final AtomicReferenceArray<LevelChunk> chunkCache;
    private final int chunkMinX, chunkMinZ, chunkSpan;

    /**
     * Rays stopped at an unloaded chunk, grouped by that chunk as
     * {@code (vx, vy, vz, strength, r, pass)} sextuples.
     *
     * <p>The strength and step index travel with the direction so a resumed ray continues from where it
     * stopped instead of re-walking its prefix. Every successful resume either terminates the finite ray
     * or advances it through at least one newly cached chunk.
     */
    private final Long2ObjectOpenHashMap<it.unimi.dsi.fastutil.doubles.DoubleArrayList> waitingRays =
            new Long2ObjectOpenHashMap<>();
    /** Resumed-ray batches whose required chunk is resident, awaiting a bounded worker slot. */
    private final ArrayDeque<double[]> readyResumeBatches = new ArrayDeque<>();
    /** Resumed-ray workers currently running. Server thread owns the list. */
    private final List<CompletableFuture<BatchResult>> resumeJobs = new ArrayList<>();

    /** Doubles per parked ray: direction (3), accumulated strength, stopped step, and pass number. */
    private static final int PARK_STRIDE = 6;
    /** Chunk keys with waiters that have not been claimed yet. */
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet missedChunks =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /** Requested chunks that have not reached FULL/resident state yet. */
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet awaitingChunks =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /** Loading-only claims retained until their chunks are no longer needed by this blast. */
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet heldChunks =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    private int maxRayPasses = 1;
    private int residentChunksSeeded = 0;
    private int chunksLoadedOnDemand = 0;
    private int peakAwaitingChunks = 0;
    private int peakHeldChunks = 0;
    private int chunkWaitTicks = 0;
    private long chunkWaitStartedAt = 0;
    private long chunkWaitWallMs = 0;

    private CompletableFuture<Void> rayJob;
    private boolean initialRayComplete;
    private final AtomicLong resumedRayCpuNanos = new AtomicLong();
    private volatile boolean failed;
    private int rayPipelineTicks;
    /** Immutable attenuation tables, copy-on-write published to all ray workers. */
    private volatile AttenuationTable[] attenuationTables = new AttenuationTable[0];
    /** Test seam: force resident chunks through the waiter path without unloading the world. */
    private boolean deferAlternateResidentChunksForTest;

    // --- instrumentation: where the wall-clock actually goes (see IMPLEMENTATION-HISTORY §19z-f) ---
    private final long createdAt = System.currentTimeMillis();
    private long prefetchMs;
    private long rayWallMs;
    private long rayPhaseWallMs;
    private long rayStartedAt;
    private long destructionMs;
    private int blocksDestroyed;
    private int destructionTicks;

    public ExplosionNukeRayBatched(Level world, int x, int y, int z, int strength, int radius) {
        if (radius <= 0) throw new IllegalArgumentException("Nuclear crater radius must be positive");
        this.world = world;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.strength = strength;
        this.radius = radius;
        this.minY = world.getMinY();
        this.maxY = world.getMaxY();

        // Total number of points on the spiral sphere.
        this.gspNumMax = (int) (2.5 * Math.PI * Math.pow(this.strength, 2));

        // Every chunk a ray of length radius+1 can touch.
        int chunkRadius = (radius >> 4) + 2;
        this.chunkMinX = (posX >> 4) - chunkRadius;
        this.chunkMinZ = (posZ >> 4) - chunkRadius;
        this.chunkSpan = chunkRadius * 2 + 1;
        this.chunkCache = new AtomicReferenceArray<>(chunkSpan * chunkSpan);
    }

    // ------------------------------------------------------------------ phases

    @Override
    public void update(int processTimeMs) {
        if (cancelled) return;
        if (failed) {
            cancel();
            return;
        }

        if (isAusf3Complete) {
            destructionTick(processTimeMs);
            return;
        }

        if (rayJob == null) {
            // Initial pass. Only chunks already resident are seeded — see seedResidentChunks.
            long t0 = System.currentTimeMillis();
            seedResidentChunks();
            prefetchMs += System.currentTimeMillis() - t0;
            seedOriginClear();
            rayStartedAt = System.currentTimeMillis();
            startInitialRayJob();
            return;
        }

        if (!initialRayComplete) {
            if (!rayJob.isDone()) return;
            try {
                rayJob.join();
            } catch (Exception e) {
                Constants.LOG.error("[NUKE] initial crater ray phase failed; aborting this blast", e);
                fail();
                return;
            }
            initialRayComplete = true;
        }

        if (!harvestResumeJobs()) return;
        long t0 = System.currentTimeMillis();
        resolveMissesTick(processTimeMs);
        prefetchMs += System.currentTimeMillis() - t0;
        submitReadyResumeJobs();

        rayPipelineTicks++;
        if (rayPipelineTicks % 200 == 0) {
            Constants.LOG.info("[NUKE] ray pipeline waiting: {} waiter chunks, {} unclaimed misses, "
                            + "{} awaiting chunks, {} ready batches, {} running batches, max pass {}",
                    waitingRays.size(), missedChunks.size(), awaitingChunks.size(),
                    readyResumeBatches.size(), resumeJobs.size(), maxRayPasses);
        }

        if (resumeJobs.isEmpty() && readyResumeBatches.isEmpty() && waitingRays.isEmpty()
                && missedChunks.isEmpty() && awaitingChunks.isEmpty()) {
            finishRayPhase();
        }
    }

    /**
     * Seeds {@link #chunkCache} with the chunks that are <b>already in memory</b>, and nothing else.
     *
     * <p>This used to walk the whole blast square calling {@code world.getChunk}, which loads and, past
     * the edge of what is loaded, <b>generates</b> — up to 625 chunks for a Fat Man, on the server
     * thread, before a single block was destroyed. It was 76% of the crater's wall time even in a
     * benchmark where every chunk was already resident, and it is unbounded when terrain has to be
     * generated. Neither 1.12.2 nor the fork does this: they touch chunks as rays reach them.
     *
     * <p>{@code getChunkNow} never loads or generates, so this is a few hundred map lookups. Chunks the
     * rays actually need but nobody has loaded are discovered during the march and fetched by
     * {@link #resolveMissesTick}.
     */
    private void seedResidentChunks() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel server)) return;
        int total = chunkSpan * chunkSpan;
        for (int i = 0; i < total; i++) {
            int cx = chunkMinX + i % chunkSpan;
            int cz = chunkMinZ + i / chunkSpan;
            LevelChunk chunk = server.getChunkSource().getChunkNow(cx, cz);
            if (chunk != null) {
                if (deferAlternateResidentChunksForTest && (((cx - chunkMinX) + (cz - chunkMinZ)) & 1) == 0) {
                    continue;
                }
                claimChunk(server, ChunkPos.pack(cx, cz));
                cacheChunk(ChunkPos.pack(cx, cz), chunk);
                residentChunksSeeded++;
            }
        }
    }

    /**
     * Server-thread, time-sliced requesting of the chunks the last ray round asked for. Returns true
     * when every requested chunk is resident and in the cache.
     *
     * <p>No call here waits for generation. Loading-only tickets start bounded background work and
     * {@code getChunkNow} harvests completed chunks on later server ticks. The old synchronous
     * {@code world.getChunk} path cost 44 seconds for a radius-500 blast and produced one enormous tick.
     */
    private boolean resolveMissesTick(int processTimeMs) {
        if (!(world instanceof ServerLevel server)) return true;

        long deadline = System.nanoTime() + processTimeMs * 1_000_000L;

        // First harvest requests that completed since the previous tick.
        var awaiting = awaitingChunks.iterator();
        int harvested = 0;
        while (awaiting.hasNext()) {
            long key = awaiting.nextLong();
            LevelChunk chunk = server.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (chunk == null) continue;
            cacheChunk(key, chunk);
            awaiting.remove();
            chunksLoadedOnDemand++;
            queueWaitingRays(key);
            if ((++harvested & 63) == 0 && System.nanoTime() >= deadline) break;
        }

        // Keep a deep but bounded generation pipeline. Resident chunks free a slot immediately while
        // their loading-only claim remains held for later ray/destruction access.
        int loadWindow = Math.min(MAX_LOAD_WINDOW,
                Math.max(32, ForkJoinPool.commonPool().getParallelism() * LOAD_WINDOW_PER_WORKER));
        int handled = 0;
        while (awaitingChunks.size() < loadWindow && !missedChunks.isEmpty()) {
            long key = missedChunks.iterator().nextLong();
            missedChunks.remove(key);
            if (chunkCacheIndex(key) < 0) {
                Constants.LOG.error("[NUKE] ray requested chunk [{}, {}] outside cache square [{}, {}] span {}",
                        ChunkPos.getX(key), ChunkPos.getZ(key), chunkMinX, chunkMinZ, chunkSpan);
                fail();
                return false;
            }
            if (cachedChunk(key) != null) {
                queueWaitingRays(key);
                continue;
            }
            LevelChunk resident = server.getChunkSource().getChunkNow(ChunkPos.getX(key), ChunkPos.getZ(key));
            if (resident != null) {
                claimChunk(server, key);
                cacheChunk(key, resident);
                chunksLoadedOnDemand++;
                queueWaitingRays(key);
                continue;
            }
            claimChunk(server, key);
            awaitingChunks.add(key);
            if (awaitingChunks.size() > peakAwaitingChunks) peakAwaitingChunks = awaitingChunks.size();
            if ((++handled & 63) == 0 && System.nanoTime() >= deadline) break;
        }

        if (missedChunks.isEmpty() && awaitingChunks.isEmpty()) {
            if (chunkWaitStartedAt != 0) {
                chunkWaitWallMs += System.currentTimeMillis() - chunkWaitStartedAt;
                chunkWaitStartedAt = 0;
            }
            return true;
        }
        if (chunkWaitStartedAt == 0) chunkWaitStartedAt = System.currentTimeMillis();
        chunkWaitTicks++;
        return false;
    }

    /** Starts the one generalized-spiral pass. Parked rays resume independently afterward. */
    private void startInitialRayJob() {
        rayJob = CompletableFuture.runAsync(() -> {
            long t0 = System.currentTimeMillis();
            computeInitialRays();
            rayWallMs += System.currentTimeMillis() - t0;
        }, ForkJoinPool.commonPool());
    }

    private void queueWaitingRays(long chunkKey) {
        it.unimi.dsi.fastutil.doubles.DoubleArrayList waiting = waitingRays.remove(chunkKey);
        if (waiting == null || waiting.isEmpty()) return;
        double[] rays = waiting.toDoubleArray();
        int batchLength = RAY_BATCH * PARK_STRIDE;
        for (int off = 0; off < rays.length; off += batchLength) {
            int len = Math.min(batchLength, rays.length - off);
            readyResumeBatches.addLast(off == 0 && len == rays.length
                    ? rays : java.util.Arrays.copyOfRange(rays, off, off + len));
        }
    }

    private void submitReadyResumeJobs() {
        int maxJobs = Math.max(1, ForkJoinPool.commonPool().getParallelism() * 2 + 1);
        while (resumeJobs.size() < maxJobs && !readyResumeBatches.isEmpty()) {
            double[] batch = readyResumeBatches.removeFirst();
            resumeJobs.add(CompletableFuture.supplyAsync(() -> {
                long t0 = System.nanoTime();
                try {
                    return marchResumed(batch, batch.length / PARK_STRIDE);
                } finally {
                    resumedRayCpuNanos.addAndGet(System.nanoTime() - t0);
                }
            }, ForkJoinPool.commonPool()));
        }
    }

    private boolean harvestResumeJobs() {
        for (int i = resumeJobs.size() - 1; i >= 0; i--) {
            CompletableFuture<BatchResult> job = resumeJobs.get(i);
            if (!job.isDone()) continue;
            resumeJobs.remove(i);
            try {
                mergeResult(job.join());
            } catch (Exception e) {
                Constants.LOG.error("[NUKE] resumed crater ray batch failed; aborting this blast", e);
                fail();
                return false;
            }
        }
        return true;
    }

    /**
     * Marks the 3×3×3 cube around the detonation point for destruction — the cells the spiral march
     * can never sample (see the class doc). Runs on the server thread before the ray job starts.
     */
    private void seedOriginClear() {
        MutableBlockPos pos = new MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int y = posY + dy;
                if (y < minY || y > maxY) continue;
                for (int dz = -1; dz <= 1; dz++) {
                    BlockState state = world.getBlockState(pos.set(posX + dx, y, posZ + dz));
                    if (!state.isAir() && state.getBlock().getExplosionResistance() < 2_000_000) {
                        setBit(perChunk, posX + dx, y, posZ + dz);
                    }
                }
            }
        }
    }

    /**
     * The ray-phase coordinator (runs on a pool thread): walks the gsp recurrence sequentially (the
     * longitude is a running prefix sum, so it cannot be split), batching directions for pool tasks and
     * merging their worker-local hit maps into {@link #perChunk}. Sets {@link #isAusf3Complete} last —
     * the volatile write publishes the maps to the server thread.
     */
    private void computeInitialRays() {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        int maxPending = pool.getParallelism() * 2 + 1;
        ArrayDeque<ForkJoinTask<BatchResult>> pending = new ArrayDeque<>();
        try {
            double gspX = Math.PI;
            double gspY = 0.0;
            int gspNum = 1;

            while (gspNum <= gspNumMax && !cancelled && !failed) {
                int count = Math.min(RAY_BATCH, gspNumMax - gspNum + 1);
                double[] dirs = new double[count * 3];
                for (int i = 0; i < count; i++) {
                    double sinX = Math.sin(gspX);
                    dirs[i * 3] = sinX == 0.0D ? 0.0D : sinX * Math.cos(gspY);
                    dirs[i * 3 + 1] = sinX == 0.0D ? 0.0D : sinX * Math.sin(gspY);
                    dirs[i * 3 + 2] = Math.cos(gspX);

                    // Advance the recurrence (verbatim old generateGspUp).
                    if (gspNum < gspNumMax) {
                        int k = gspNum + 1;
                        double hk = -1.0 + 2.0 * (k - 1.0) / (gspNumMax - 1.0);
                        gspX = Math.acos(hk);
                        gspY = (gspY + 3.6 / Math.sqrt(gspNumMax) / Math.sqrt(1.0 - hk * hk)) % (Math.PI * 2);
                    } else {
                        gspX = 0.0;
                        gspY = 0.0;
                    }
                    gspNum++;
                }

                double[] batch = dirs;
                int batchCount = count;
                pending.addLast(pool.submit(() -> marchBatch(batch, batchCount)));

                if (pending.size() >= maxPending) {
                    mergeResult(pending.removeFirst().join());
                }
            }

            while (!pending.isEmpty()) {
                mergeResult(pending.removeFirst().join());
            }
        } catch (RuntimeException | Error error) {
            markFailed();
            while (!pending.isEmpty()) {
                try {
                    pending.removeFirst().join();
                } catch (RuntimeException | Error ignored) {
                    // Drain every child; failed makes queued/running work stop cooperatively.
                }
            }
            throw error;
        }
    }

    /** One batch's findings: blocks to destroy and rays grouped by the chunk they need next. */
    private record BatchResult(Long2ObjectOpenHashMap<BitSet> hits,
                               Long2ObjectOpenHashMap<it.unimi.dsi.fastutil.doubles.DoubleArrayList> waiters,
                               int maxPass) {}

    private record AttenuationTable(int resistanceBits, float[] values) {}

    private synchronized void mergeResult(BatchResult result) {
        if (cancelled) return;
        for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<BitSet> e : result.hits().long2ObjectEntrySet()) {
            perChunk.merge(e.getLongKey(), e.getValue(), (a, b) -> { a.or(b); return a; });
        }
        for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<it.unimi.dsi.fastutil.doubles.DoubleArrayList> e
                : result.waiters().long2ObjectEntrySet()) {
            long key = e.getLongKey();
            it.unimi.dsi.fastutil.doubles.DoubleArrayList existing = waitingRays.get(key);
            if (existing == null) waitingRays.put(key, e.getValue());
            else existing.addAll(e.getValue());
            missedChunks.add(key);
        }
        if (result.maxPass() > maxRayPasses) maxRayPasses = result.maxPass();
    }

    private synchronized void finishRayPhase() {
        if (isAusf3Complete) return;
        long t0 = System.currentTimeMillis();
        if (world instanceof ServerLevel server) {
            // A chunk that was resident during ray tracing can otherwise unload before destruction.
            // Retain every hit chunk so the destruction phase also remains strictly non-blocking.
            for (long key : perChunk.keySet()) claimChunk(server, key);

            // Chunks traversed by rays but containing no hit never participate in destruction.
            var held = heldChunks.iterator();
            while (held.hasNext()) {
                long key = held.nextLong();
                if (perChunk.containsKey(key)) continue;
                held.remove();
                Chunks.releaseForLoading(server, ChunkPos.getX(key), ChunkPos.getZ(key));
            }
        }
        prefetchMs += System.currentTimeMillis() - t0;
        rayPhaseWallMs = System.currentTimeMillis() - rayStartedAt;
        orderChunks();
        isAusf3Complete = true;
        if (perChunk.isEmpty()) {
            releaseAllChunks();
            logSummary();
        }
    }

    private void fail() {
        markFailed();
        cancel();
    }

    private synchronized boolean markFailed() {
        if (failed) return false;
        failed = true;
        return true;
    }

    public boolean hasFailed() {
        return failed;
    }

    /** Forces half the resident cache through the event-driven resume path for full-hit-set tests. */
    public void deferAlternateResidentChunksForTest() {
        if (rayJob != null) throw new IllegalStateException("ray phase already started");
        deferAlternateResidentChunksForTest = true;
    }

    /** Whether ray tracing, but not necessarily destruction, has completed. */
    public boolean isRayPhaseComplete() {
        return isAusf3Complete;
    }

    /** Exact relative per-chunk bitset comparison used to prove scheduler equivalence. */
    public boolean hasSameHitsRelativeTo(ExplosionNukeRayBatched other) {
        if (!isAusf3Complete || !other.isAusf3Complete || perChunk.size() != other.perChunk.size()) return false;
        int thisX = posX >> 4, thisZ = posZ >> 4;
        int otherX = other.posX >> 4, otherZ = other.posZ >> 4;
        for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<BitSet> entry : perChunk.long2ObjectEntrySet()) {
            int relativeX = ChunkPos.getX(entry.getLongKey()) - thisX;
            int relativeZ = ChunkPos.getZ(entry.getLongKey()) - thisZ;
            BitSet otherBits = other.perChunk.get(ChunkPos.pack(otherX + relativeX, otherZ + relativeZ));
            if (!entry.getValue().equals(otherBits)) return false;
        }
        return true;
    }

    private int chunkCacheIndex(long key) {
        int localX = ChunkPos.getX(key) - chunkMinX;
        int localZ = ChunkPos.getZ(key) - chunkMinZ;
        if (localX < 0 || localX >= chunkSpan || localZ < 0 || localZ >= chunkSpan) return -1;
        return localZ * chunkSpan + localX;
    }

    private LevelChunk cachedChunk(long key) {
        int index = chunkCacheIndex(key);
        return index < 0 ? null : chunkCache.get(index);
    }

    private void cacheChunk(long key, LevelChunk chunk) {
        int index = chunkCacheIndex(key);
        if (index >= 0) chunkCache.set(index, chunk);
    }

    private void claimChunk(ServerLevel server, long key) {
        if (!heldChunks.add(key)) return;
        Chunks.claimForLoading(server, ChunkPos.getX(key), ChunkPos.getZ(key));
        if (heldChunks.size() > peakHeldChunks) peakHeldChunks = heldChunks.size();
    }

    private void releaseChunk(long key) {
        if (!(world instanceof ServerLevel server) || !heldChunks.remove(key)) return;
        Chunks.releaseForLoading(server, ChunkPos.getX(key), ChunkPos.getZ(key));
    }

    private void releaseAllChunks() {
        if (world instanceof ServerLevel server) {
            for (long key : heldChunks) {
                Chunks.releaseForLoading(server, ChunkPos.getX(key), ChunkPos.getZ(key));
            }
        }
        heldChunks.clear();
        awaitingChunks.clear();
        missedChunks.clear();
        waitingRays.clear();
        readyResumeBatches.clear();
        resumeJobs.clear();
    }

    /** Nearest-chunk-first, so the crater opens from the middle outwards like the original. */
    private void orderChunks() {
        int chunkX = posX >> 4;
        int chunkZ = posZ >> 4;
        long[] keys = perChunk.keySet().toLongArray();
        // Sort by (distance, index) packed into one long: no comparator object, no boxing.
        long[] sortKeys = new long[keys.length];
        for (int i = 0; i < keys.length; i++) {
            int dist = Math.abs(chunkX - ChunkPos.getX(keys[i])) + Math.abs(chunkZ - ChunkPos.getZ(keys[i]));
            sortKeys[i] = ((long) dist << 32) | i;
        }
        java.util.Arrays.sort(sortKeys);
        orderedChunks = new long[keys.length];
        for (int i = 0; i < keys.length; i++) orderedChunks[i] = keys[(int) sortKeys[i]];
        orderedIndex = 0;
    }

    /** Marches one batch of ray directions against the read-only chunk cache. Pool-thread only. */
    private BatchResult marchBatch(double[] dirs, int count) {
        Long2ObjectOpenHashMap<BitSet> hits = new Long2ObjectOpenHashMap<>();
        Long2ObjectOpenHashMap<it.unimi.dsi.fastutil.doubles.DoubleArrayList> waiters = new Long2ObjectOpenHashMap<>();
        MutableBlockPos pos = new MutableBlockPos();
        int[] maxPass = {1};

        for (int i = 0; i < count && !cancelled && !failed; i++) {
            marchOne(dirs[i * 3], dirs[i * 3 + 1], dirs[i * 3 + 2], strength * 0.3F, 0, 1,
                    hits, waiters, maxPass, pos);
        }
        return new BatchResult(hits, waiters, maxPass[0]);
    }

    /** Re-marches rays that parked, each resuming from its own stored strength and step. */
    private BatchResult marchResumed(double[] rays, int count) {
        Long2ObjectOpenHashMap<BitSet> hits = new Long2ObjectOpenHashMap<>();
        Long2ObjectOpenHashMap<it.unimi.dsi.fastutil.doubles.DoubleArrayList> waiters = new Long2ObjectOpenHashMap<>();
        MutableBlockPos pos = new MutableBlockPos();
        int[] maxPass = {1};

        for (int i = 0; i < count && !cancelled && !failed; i++) {
            int base = i * PARK_STRIDE;
            marchOne(rays[base], rays[base + 1], rays[base + 2],
                    (float) rays[base + 3], (int) rays[base + 4], (int) rays[base + 5],
                    hits, waiters, maxPass, pos);
        }
        return new BatchResult(hits, waiters, maxPass[0]);
    }

    /** One ray, from step {@code startR} with {@code startStrength} left. Pool-thread only. */
    private void marchOne(double vx, double vy, double vz, float startStrength, int startR, int rayPass,
                           Long2ObjectOpenHashMap<BitSet> hits,
                           Long2ObjectOpenHashMap<it.unimi.dsi.fastutil.doubles.DoubleArrayList> waiters,
                           int[] maxPass,
                           MutableBlockPos pos) {
        {
            float rayStrength = startStrength;
            // A ray crosses ~16 cells per chunk, so the lookup is the same one over and over. Holding the
            // last (key, chunk) turns a hash probe per sampled block into a long compare.
            long lastKey = Long.MIN_VALUE;
            LevelChunk lastChunk = null;
            int lastResistanceBits = 0;
            float[] lastAttenuation = null;

            // Find the end of the ray (verbatim old march, including the r = 0,2,3,… sampling).
            for (int r = startR; r < radius + 1 && !cancelled && !failed; r++) {

                int iY = (int) Math.floor(posY + (vy * r));
                if (iY < minY || iY > maxY) {
                    isContained = false;
                    break;
                }

                int iX = (int) Math.floor(posX + (vx * r));
                int iZ = (int) Math.floor(posZ + (vz * r));

                long chunkKey = ChunkPos.pack(iX >> 4, iZ >> 4);
                LevelChunk chunk;
                if (chunkKey == lastKey) {
                    chunk = lastChunk;
                } else {
                    chunk = cachedChunk(chunkKey);
                    lastKey = chunkKey;
                    lastChunk = chunk;
                }
                if (chunk == null) {
                    if (chunkCacheIndex(chunkKey) < 0) {
                        if (markFailed()) {
                            Constants.LOG.error("[NUKE] ray at [{}, {}, {}] requested out-of-cache chunk [{}, {}] "
                                            + "from origin [{}, {}, {}], r={}, pass={}",
                                    iX, iY, iZ, iX >> 4, iZ >> 4, posX, posY, posZ, r, rayPass);
                        }
                        break;
                    }
                    // Nobody has this chunk in memory. Park the ray rather than treat unloaded terrain as
                    // vacuum (which is what the old code did, letting a blast punch through un-generated
                    // ground); the server thread loads it and resumes only this chunk's waiters.
                    int nextPass = rayPass + 1;
                    it.unimi.dsi.fastutil.doubles.DoubleArrayList parked = waiters.computeIfAbsent(
                            chunkKey, ignored -> new it.unimi.dsi.fastutil.doubles.DoubleArrayList());
                    parked.add(vx);
                    parked.add(vy);
                    parked.add(vz);
                    parked.add(rayStrength);
                    parked.add(r);
                    parked.add(nextPass);
                    if (nextPass > maxPass[0]) maxPass[0] = nextPass;
                    break;
                }
                BlockState blockState = chunk.getBlockState(pos.set(iX, iY, iZ));

                // The r = 0 -> 1 rewrite, hoisted out of the attenuation expression it used to hide in.
                // It has to happen on EVERY step, air included: it is what produces upstream's r = 0, 2,
                // 3, ... sampling, so skipping it below would change which cells a ray visits.
                if (r == 0) r = 1;

                boolean air = blockState.isAir();
                if (!air) {
                    // One resistance read, used for both the indestructible test and the attenuation.
                    float resistance = blockState.getBlock().getExplosionResistance();
                    if (resistance >= 2_000_000) break;

                    float effectiveResistance = nukeResistance(blockState, resistance);
                    int resistanceBits = Float.floatToRawIntBits(effectiveResistance);
                    if (lastAttenuation == null || resistanceBits != lastResistanceBits) {
                        lastResistanceBits = resistanceBits;
                        lastAttenuation = attenuationTable(resistanceBits).values();
                    }
                    rayStrength -= lastAttenuation[r];
                }
                // Air is skipped above rather than run through the maths: its resistance is 0, so the
                // attenuation term is pow(1, k) - 1 = 0 exactly. Most cells a big blast samples are air,
                // and this is a Math.pow plus two virtual calls per cell.

                if (rayStrength > 0) {
                    if (!air) {
                        // All-air chunks don't need to be buffered at all.
                        setBit(hits, iX, iY, iZ);
                    }
                    if (r >= radius) {
                        isContained = false;
                    }
                } else {
                    break;
                }
            }
        }
    }

    private void setBit(Long2ObjectOpenHashMap<BitSet> map, int x, int y, int z) {
        long key = ChunkPos.pack(x >> 4, z >> 4);
        BitSet hitPositions = map.get(key);
        if (hitPositions == null) {
            hitPositions = new BitSet((maxY - minY + 1) << 8);
            map.put(key, hitPositions);
        }
        hitPositions.set(((maxY - y) << 8) + ((x & 15) << 4) + (z & 15));
    }

    /**
     * The old resistance table, special cases included: liquids are nearly transparent to the blast,
     * and the old mod deliberately rated sandstone at 4 and obsidian at 18 (vanilla resistance 1200 —
     * without this, obsidian blast-bunkers survive a direct nuke, which the old mod did not allow).
     */
    public static float getNukeResistance(BlockState blockState) {
        return nukeResistance(blockState, blockState.getBlock().getExplosionResistance());
    }

    /** As above, for callers that already read the block's resistance and should not read it twice. */
    private static float nukeResistance(BlockState blockState, float resistance) {
        if (blockState.liquid()) return 0.1F;
        Block b = blockState.getBlock();
        if (b == Blocks.SANDSTONE) return 4F;
        if (b == Blocks.OBSIDIAN) return 18F;
        return resistance;
    }

    private AttenuationTable attenuationTable(int bits) {
        AttenuationTable[] tables = attenuationTables;
        for (AttenuationTable table : tables) {
            if (table.resistanceBits() == bits) return table;
        }
        return installAttenuationTable(bits);
    }

    private synchronized AttenuationTable installAttenuationTable(int resistanceBits) {
        for (AttenuationTable table : attenuationTables) {
            if (table.resistanceBits() == resistanceBits) return table;
        }
        float resistance = Float.intBitsToFloat(resistanceBits);
        AttenuationTable table = new AttenuationTable(resistanceBits,
                buildAttenuationTable(resistance, radius));
        AttenuationTable[] expanded = java.util.Arrays.copyOf(attenuationTables, attenuationTables.length + 1);
        expanded[expanded.length - 1] = table;
        attenuationTables = expanded;
        return table;
    }

    private static float[] buildAttenuationTable(float resistance, int radius) {
        float[] values = new float[radius + 1];
        for (int r = 1; r <= radius; r++) {
            values[r] = (float) (Math.pow(resistance + 1,
                    3 * ((float) r) / ((float) radius)) - 1);
        }
        if (radius > 0) values[0] = values[1];
        return values;
    }

    /** Pins cached attenuation to the exact former per-sample expression. */
    public static boolean attenuationTableContractForTest() {
        int radius = 500;
        float[] resistances = {0.1F, 0.5F, 4F, 18F, 1200F};
        int[] samples = {1, 2, 17, 249, 500};
        for (float resistance : resistances) {
            float[] table = buildAttenuationTable(resistance, radius);
            for (int r : samples) {
                float direct = (float) (Math.pow(resistance + 1,
                        3 * ((float) r) / ((float) radius)) - 1);
                if (Float.floatToRawIntBits(table[r]) != Float.floatToRawIntBits(direct)) return false;
            }
        }
        return true;
    }


    // ------------------------------------------------------------------ lifecycle

    @Override
    public synchronized void cancel() {
        if (cancelled) return;
        cancelled = true;
        isAusf3Complete = true;
        orderedChunks = new long[0];
        orderedIndex = 0;
        perChunk.clear();
        // A half-carved chunk is left as it is: the blocks already cleared stay cleared, and the
        // per-chunk repair is skipped along with the rest of the blast. Dropping the reference is what
        // stops the next tick from calling finish() against a bitset that is no longer there.
        carver = null;
        List<CompletableFuture<?>> running = new ArrayList<>();
        if (rayJob != null && !rayJob.isDone()) running.add(rayJob);
        for (CompletableFuture<BatchResult> job : resumeJobs) if (!job.isDone()) running.add(job);
        if (!running.isEmpty() && world instanceof ServerLevel server) {
            CompletableFuture.allOf(running.toArray(CompletableFuture[]::new))
                    .whenComplete((ignored, error) -> server.getServer().execute(this::releaseAllChunks));
        } else {
            releaseAllChunks();
        }
    }

    @Override
    public boolean isComplete() {
        return isAusf3Complete && perChunk.isEmpty();
    }

    @Override
    public boolean isContained() {
        return isContained;
    }

    /** Diagnostics and deterministic benchmark assertion. */
    public int getBlocksDestroyed() {
        return blocksDestroyed;
    }

    @Override
    public void setDetonator(UUID detonator) {
        this.detonator = detonator;
    }

    // ------------------------------------------------------------------ destruction (server thread)

    public void destructionTick(int time) {
        long start = System.currentTimeMillis();
        destructionTicks++;
        while (System.currentTimeMillis() < start + time) {
            if (perChunk.isEmpty()) break;
            if (!processChunkBlocks(start, time)) break;
        }
        destructionMs += System.currentTimeMillis() - start;
        if (perChunk.isEmpty()) {
            releaseAllChunks();
            logSummary();
        }
    }

    private boolean logged = false;

    /**
     * One line per blast, with the phase split. This is the only way to tell a slow crater apart from a
     * slow ray phase after the fact, and the numbers are what any performance change has to move.
     */
    private void logSummary() {
        if (logged) return;
        logged = true;
        long total = System.currentTimeMillis() - createdAt;
        Constants.LOG.info("[NUKE] r={} done in {} ms wall: chunks {} ms server + {} ms async wait over {} ticks "
                        + "({} initially resident, {} loaded on demand, peak {} awaiting/{} held, {} max ray passes), "
                        + "rays {} ms initial + {} ms aggregate resumed worker time ({} ms phase wall), "
                        + "{} attenuation tables, destruction {} ms over {} ticks ({} blocks, {} blocks/ms)",
                radius, total, prefetchMs, chunkWaitWallMs, chunkWaitTicks,
                residentChunksSeeded, chunksLoadedOnDemand, peakAwaitingChunks, peakHeldChunks, maxRayPasses,
                rayWallMs, resumedRayCpuNanos.get() / 1_000_000L, rayPhaseWallMs,
                attenuationTables.length, destructionMs, destructionTicks,
                blocksDestroyed, destructionMs > 0 ? blocksDestroyed / destructionMs : blocksDestroyed);
    }

    private BitSet hitArray;
    private long destChunk;
    private boolean needsNewHitArray = true;
    private int index = 0;

    /**
     * The section carve for the chunk currently being cleared, or null between chunks.
     *
     * <p>Null also whenever the carve path is unavailable (see {@link #processChunkBlocks}); the
     * per-block loop below is then used instead, so this field is the only thing distinguishing the
     * two paths at runtime.
     */
    private ChunkCarver carver;

    private boolean processChunkBlocks(long start, int time) {
        if (this.perChunk.isEmpty() || orderedIndex >= orderedChunks.length) return false;

        if (BombConfig.craterSectionCarve && world instanceof ServerLevel server) {
            return carveChunk(server, start, time);
        }

        if (needsNewHitArray) {
            destChunk = orderedChunks[orderedIndex];
            if (world instanceof ServerLevel server
                    && server.getChunkSource().getChunkNow(ChunkPos.getX(destChunk), ChunkPos.getZ(destChunk)) == null) {
                return false;
            }
            hitArray = perChunk.get(destChunk);
            index = hitArray.nextSetBit(0);
            needsNewHitArray = false;
        }

        int chunkX = ChunkPos.getX(destChunk) << 4;
        int chunkZ = ChunkPos.getZ(destChunk) << 4;

        MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int blocksRemoved = 0;
        while (index > -1) {
            pos.set(((index >> 4) & 15) + chunkX, maxY - (index >> 8), (index & 15) + chunkZ);
            // setBlock(AIR), NOT removeBlock: removeBlock replaces a block with its fluid state's
            // legacy block (getFluidState(pos).createLegacyBlock()), so it leaves water/lava behind —
            // the old setBlockToAir cleared everything. Suppress drops + neighbour cascades for perf
            // (a nuke clears millions of blocks; we don't want item drops or block-update storms).
            world.setBlock(pos, AIR, CLEAR_FLAGS);
            index = hitArray.nextSetBit(index + 1);
            blocksRemoved++;
            blocksDestroyed++;
            if (blocksRemoved % 256 == 0 && System.currentTimeMillis() + 1 > start + time) {
                break;
            }
        }

        if (index < 0) {
            perChunk.remove(destChunk);
            releaseChunk(destChunk);
            orderedIndex++;
            needsNewHitArray = true;
        }
        return true;
    }

    /**
     * The fast destruction path: clear the chunk's blocks straight into its sections and repair the
     * per-chunk state once, instead of paying vanilla's per-block bookkeeping millions of times. See
     * {@link ChunkCarver} for what that bookkeeping is and why doing it per chunk is equivalent.
     *
     * <p>The chunk, not the tick, is the unit that has to complete: the heightmap, light and resend
     * pass in {@link ChunkCarver#finish()} only makes sense against a finished shape. The carve itself
     * is still time-sliced, so a single chunk spanning several ticks is fine — {@code finish()} simply
     * runs on the tick that exhausts its bitset.
     */
    private boolean carveChunk(ServerLevel server, long start, int time) {
        if (carver == null) {
            destChunk = orderedChunks[orderedIndex];
            int cx = ChunkPos.getX(destChunk);
            int cz = ChunkPos.getZ(destChunk);
            LevelChunk chunk = server.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) return false;
            carver = new ChunkCarver(server, chunk, perChunk.get(destChunk), cx, cz, maxY);
        }

        if (!carver.carveUntil(start + time)) return true;

        carver.finish();
        blocksDestroyed += carver.carved();
        carver = null;
        perChunk.remove(destChunk);
        releaseChunk(destChunk);
        orderedIndex++;
        return true;
    }

}
