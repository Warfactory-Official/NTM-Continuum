package com.hbm.explosion;

import com.hbm.Constants;
import com.hbm.config.BombConfig;
import com.hbm.interfaces.IExplosionRay;
import com.hbm.util.ChunkCarver;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

/**
 * The Fat Man / mk5 crater raytracer, ported from 1.12.2's {@code ExplosionNukeRayBatched} — with the
 * ray phase parallelized to match the wall-clock of the fork's <b>default</b> algorithm
 * ({@code explosionAlgorithm = 2} = the threaded DDA {@code ExplosionNukeRayParallelized}; only this
 * batched spiral is ported, but its compute now fans out the same way).
 *
 * <p><b>Phases:</b>
 * <ol>
 *   <li><b>Prefetch</b> (server thread, time-sliced): sync-resolve every chunk in the blast square into
 *       a read-only cache — workers must never call {@code Level} methods.</li>
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
    private final Long2ObjectOpenHashMap<LevelChunk> chunkCache;
    private final int chunkMinX, chunkMinZ, chunkSpan;

    /**
     * How many rounds of "march, park on a missing chunk, load it, march again" are allowed.
     *
     * <p>Scaled to the blast, not a flat number: a ray stops at the <em>first</em> chunk it cannot read,
     * so a radial line of un-generated chunks costs one round per chunk. A Fat Man reaches eleven chunks
     * out, and a fixed cap of 8 would clip its crater short in ungenerated terrain. Extra rounds are
     * cheap — only parked rays re-march, and they resume where they stopped.
     */
    private final int maxRayRounds;
    /**
     * Rays stopped at an unloaded chunk, as {@code (vx, vy, vz, strength, r)} quintuples.
     *
     * <p>The strength and step index travel with the direction so a resumed ray continues from where it
     * stopped instead of re-walking its prefix. Re-walking is correct (the hit bitsets are a set) but at
     * Fat Man scale, with up to {@link #maxRayRounds} rounds, it would multiply the most expensive
     * phase by the number of rounds.
     */
    private final it.unimi.dsi.fastutil.doubles.DoubleArrayList parkedDirs =
            new it.unimi.dsi.fastutil.doubles.DoubleArrayList();

    /** Doubles per parked ray: direction (3) + accumulated strength + the step it stopped at. */
    private static final int PARK_STRIDE = 5;
    /** Chunk keys the parked rays need. Server thread loads these between rounds. */
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet missedChunks =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    private int rayRound = 0;
    private int chunksLoadedOnDemand = 0;

    private CompletableFuture<Void> rayJob;

    // --- instrumentation: where the wall-clock actually goes (see IMPLEMENTATION-HISTORY §19z-f) ---
    private final long createdAt = System.currentTimeMillis();
    private long prefetchMs;
    private long rayWallMs;
    private long rayStartedAt;
    private long destructionMs;
    private int blocksDestroyed;
    private int destructionTicks;

    public ExplosionNukeRayBatched(Level world, int x, int y, int z, int strength, int radius) {
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
        this.chunkCache = new Long2ObjectOpenHashMap<>(chunkSpan * chunkSpan);
        this.maxRayRounds = chunkRadius + 4;
    }

    // ------------------------------------------------------------------ phases

    @Override
    public void update(int processTimeMs) {
        if (cancelled) return;

        if (isAusf3Complete) {
            destructionTick(processTimeMs);
            return;
        }

        if (rayJob == null) {
            // Round 1. Only chunks already resident are seeded — see seedResidentChunks.
            long t0 = System.currentTimeMillis();
            seedResidentChunks();
            prefetchMs += System.currentTimeMillis() - t0;
            seedOriginClear();
            rayStartedAt = System.currentTimeMillis();
            startRayRound();
            return;
        }

        if (!rayJob.isDone()) return;

        try {
            rayJob.join();
        } catch (Exception e) {
            Constants.LOG.error("[NUKE] crater ray phase failed; destroying what was gathered", e);
            finishRayPhase();
            return;
        }

        // Rays that ran into a chunk nobody had loaded parked instead of pretending it was vacuum.
        // Resolve what they asked for — on this thread, within the budget — and re-march just those rays.
        if (!parkedDirs.isEmpty() && rayRound < maxRayRounds) {
            long t0 = System.currentTimeMillis();
            boolean resolved = resolveMissesTick(processTimeMs);
            prefetchMs += System.currentTimeMillis() - t0;
            if (!resolved) return; // more chunks to fetch; stay in this phase
            startRayRound();
            return;
        }

        finishRayPhase();
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
            if (chunk != null) chunkCache.put(ChunkPos.pack(cx, cz), chunk);
        }
    }

    /**
     * Server-thread, time-sliced loading of the chunks the last ray round asked for. Returns true when
     * every requested chunk is in the cache.
     */
    private boolean resolveMissesTick(int processTimeMs) {
        long deadline = System.currentTimeMillis() + processTimeMs;
        int handled = 0;
        while (!missedChunks.isEmpty()) {
            long key = missedChunks.iterator().nextLong();
            missedChunks.remove(key);
            int cx = ChunkPos.getX(key);
            int cz = ChunkPos.getZ(key);
            if (!chunkCache.containsKey(key)) {
                chunkCache.put(key, world.getChunk(cx, cz)); // loads/generates — the expensive call
                chunksLoadedOnDemand++;
            }
            if ((++handled & 3) == 0 && System.currentTimeMillis() >= deadline) return false;
        }
        return true;
    }

    /** Fires off one round of ray work: round 1 streams the spiral, later rounds re-march parked rays. */
    private void startRayRound() {
        rayRound++;
        double[] parked = parkedDirs.isEmpty() ? null : parkedDirs.toDoubleArray();
        parkedDirs.clear();
        rayJob = CompletableFuture.runAsync(() -> {
            long t0 = System.currentTimeMillis();
            computeRays(parked);
            rayWallMs += System.currentTimeMillis() - t0; // compute only; the resolve waits are separate
        }, ForkJoinPool.commonPool());
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
    private void computeRays(double[] parked) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        int maxPending = pool.getParallelism() * 2 + 1;
        ArrayDeque<ForkJoinTask<BatchResult>> pending = new ArrayDeque<>();

        if (parked != null) {
            // A later round: only the rays that stopped at a chunk nobody had loaded. They restart from
            // r = 0, which re-records the hits they already found — the bitsets are a set, so that is
            // free — and then continue past the chunk that is now resident.
            for (int off = 0; off < parked.length; off += RAY_BATCH * PARK_STRIDE) {
                int len = Math.min(RAY_BATCH * PARK_STRIDE, parked.length - off);
                double[] batch = java.util.Arrays.copyOfRange(parked, off, off + len);
                pending.addLast(pool.submit(() -> marchResumed(batch, len / PARK_STRIDE)));
                if (pending.size() >= maxPending) mergeResult(pending.removeFirst().join());
            }
            while (!pending.isEmpty()) mergeResult(pending.removeFirst().join());
            return;
        }

        double gspX = Math.PI;
        double gspY = 0.0;
        int gspNum = 1;

        while (gspNum <= gspNumMax && !cancelled) {
            int count = Math.min(RAY_BATCH, gspNumMax - gspNum + 1);
            double[] dirs = new double[count * 3];
            for (int i = 0; i < count; i++) {
                // Cartesian direction for the current spherical point (90° X-rotation, like the old code).
                dirs[i * 3] = Math.sin(gspX) * Math.cos(gspY);
                dirs[i * 3 + 1] = Math.sin(gspX) * Math.sin(gspY);
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
    }

    /** One batch's findings: blocks to destroy, rays that parked, and the chunks they are waiting on. */
    private record BatchResult(Long2ObjectOpenHashMap<BitSet> hits,
                               it.unimi.dsi.fastutil.doubles.DoubleArrayList parked,
                               it.unimi.dsi.fastutil.longs.LongOpenHashSet misses) {}

    private void mergeResult(BatchResult result) {
        for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<BitSet> e : result.hits().long2ObjectEntrySet()) {
            perChunk.merge(e.getLongKey(), e.getValue(), (a, b) -> { a.or(b); return a; });
        }
        parkedDirs.addAll(result.parked());
        missedChunks.addAll(result.misses());
    }

    private synchronized void finishRayPhase() {
        if (isAusf3Complete) return;
        orderChunks();
        isAusf3Complete = true;
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
        it.unimi.dsi.fastutil.doubles.DoubleArrayList parked = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        it.unimi.dsi.fastutil.longs.LongOpenHashSet misses = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        MutableBlockPos pos = new MutableBlockPos();

        for (int i = 0; i < count && !cancelled; i++) {
            marchOne(dirs[i * 3], dirs[i * 3 + 1], dirs[i * 3 + 2], strength * 0.3F, 0, hits, parked, misses, pos);
        }
        return new BatchResult(hits, parked, misses);
    }

    /** Re-marches rays that parked, each resuming from its own stored strength and step. */
    private BatchResult marchResumed(double[] rays, int count) {
        Long2ObjectOpenHashMap<BitSet> hits = new Long2ObjectOpenHashMap<>();
        it.unimi.dsi.fastutil.doubles.DoubleArrayList parked = new it.unimi.dsi.fastutil.doubles.DoubleArrayList();
        it.unimi.dsi.fastutil.longs.LongOpenHashSet misses = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        MutableBlockPos pos = new MutableBlockPos();

        for (int i = 0; i < count && !cancelled; i++) {
            int base = i * PARK_STRIDE;
            marchOne(rays[base], rays[base + 1], rays[base + 2],
                    (float) rays[base + 3], (int) rays[base + 4], hits, parked, misses, pos);
        }
        return new BatchResult(hits, parked, misses);
    }

    /** One ray, from step {@code startR} with {@code startStrength} left. Pool-thread only. */
    private void marchOne(double vx, double vy, double vz, float startStrength, int startR,
                          Long2ObjectOpenHashMap<BitSet> hits,
                          it.unimi.dsi.fastutil.doubles.DoubleArrayList parked,
                          it.unimi.dsi.fastutil.longs.LongOpenHashSet misses,
                          MutableBlockPos pos) {
        {
            float rayStrength = startStrength;
            // A ray crosses ~16 cells per chunk, so the lookup is the same one over and over. Holding the
            // last (key, chunk) turns a hash probe per sampled block into a long compare.
            long lastKey = Long.MIN_VALUE;
            LevelChunk lastChunk = null;

            // Find the end of the ray (verbatim old march, including the r = 0,2,3,… sampling).
            for (int r = startR; r < radius + 1; r++) {

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
                    chunk = chunkCache.get(chunkKey);
                    lastKey = chunkKey;
                    lastChunk = chunk;
                }
                if (chunk == null) {
                    // Nobody has this chunk in memory. Park the ray rather than treat unloaded terrain as
                    // vacuum (which is what the old code did, letting a blast punch through un-generated
                    // ground); the server thread loads it and this ray is re-marched next round.
                    misses.add(chunkKey);
                    parked.add(vx);
                    parked.add(vy);
                    parked.add(vz);
                    parked.add(rayStrength);
                    parked.add(r);
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

                    rayStrength -= (float) (Math.pow(nukeResistance(blockState, resistance) + 1,
                            3 * ((float) r) / ((float) radius)) - 1);
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

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void cancel() {
        cancelled = true;
        isAusf3Complete = true;
        orderedChunks = new long[0];
        orderedIndex = 0;
        perChunk.clear();
        // A half-carved chunk is left as it is: the blocks already cleared stay cleared, and the
        // per-chunk repair is skipped along with the rest of the blast. Dropping the reference is what
        // stops the next tick from calling finish() against a bitset that is no longer there.
        carver = null;
    }

    @Override
    public boolean isComplete() {
        return isAusf3Complete && perChunk.isEmpty();
    }

    @Override
    public boolean isContained() {
        return isContained;
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
            processChunkBlocks(start, time);
        }
        destructionMs += System.currentTimeMillis() - start;
        if (perChunk.isEmpty()) logSummary();
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
        Constants.LOG.info("[NUKE] r={} done in {} ms wall: chunks {} ms ({} cached, {} loaded on demand, "
                        + "{} ray rounds), rays {} ms, destruction {} ms over {} ticks ({} blocks, {} blocks/ms)",
                radius, total, prefetchMs, chunkCache.size(), chunksLoadedOnDemand, rayRound,
                rayWallMs, destructionMs, destructionTicks,
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

    public void processChunkBlocks(long start, int time) {
        if (this.perChunk.isEmpty() || orderedIndex >= orderedChunks.length) return;

        if (BombConfig.craterSectionCarve && world instanceof ServerLevel server) {
            carveChunk(server, start, time);
            return;
        }

        if (needsNewHitArray) {
            destChunk = orderedChunks[orderedIndex];
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
            orderedIndex++;
            needsNewHitArray = true;
        }
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
    private void carveChunk(ServerLevel server, long start, int time) {
        if (carver == null) {
            destChunk = orderedChunks[orderedIndex];
            int cx = ChunkPos.getX(destChunk);
            int cz = ChunkPos.getZ(destChunk);
            carver = new ChunkCarver(server, world.getChunk(cx, cz), perChunk.get(destChunk), cx, cz, maxY);
        }

        if (!carver.carveUntil(start + time)) return;

        carver.finish();
        blocksDestroyed += carver.carved();
        carver = null;
        perChunk.remove(destChunk);
        orderedIndex++;
    }
}
