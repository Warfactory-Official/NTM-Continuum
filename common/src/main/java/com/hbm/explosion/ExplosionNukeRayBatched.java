package com.hbm.explosion;

import com.hbm.Constants;
import com.hbm.interfaces.IExplosionRay;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
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

    private final HashMap<ChunkPos, BitSet> perChunk = new HashMap<>();
    private final List<ChunkPos> orderedChunks = new ArrayList<>();
    private final CoordComparator comparator = new CoordComparator();
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
    private int prefetchIndex = 0;

    private CompletableFuture<Void> rayJob;

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
            if (!prefetchTick(processTimeMs)) return; // still resolving chunks
            seedOriginClear();
            rayJob = CompletableFuture.runAsync(this::computeRays, ForkJoinPool.commonPool());
            return;
        }

        if (rayJob.isDone() && !isAusf3Complete) {
            // The coordinator sets isAusf3Complete itself on success — reaching this means it died.
            try {
                rayJob.join();
            } catch (Exception e) {
                Constants.LOG.error("[NUKE] crater ray phase failed; destroying what was gathered", e);
            }
            finishRayPhase();
        }
    }

    /**
     * Server-thread, time-sliced sync resolution of the blast square into {@link #chunkCache} (workers
     * must never touch {@code Level}). Chunks near the blast are almost always already loaded; the cold
     * edge of a big blast may need loading/generation, hence the budget. Returns true when done.
     */
    private boolean prefetchTick(int processTimeMs) {
        long deadline = System.currentTimeMillis() + processTimeMs;
        int total = chunkSpan * chunkSpan;
        while (prefetchIndex < total) {
            int cx = chunkMinX + prefetchIndex % chunkSpan;
            int cz = chunkMinZ + prefetchIndex / chunkSpan;
            chunkCache.put(ChunkPos.pack(cx, cz), world.getChunk(cx, cz));
            prefetchIndex++;
            if ((prefetchIndex & 7) == 0 && System.currentTimeMillis() >= deadline) return false;
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
    private void computeRays() {
        double gspX = Math.PI;
        double gspY = 0.0;
        int gspNum = 1;

        ForkJoinPool pool = ForkJoinPool.commonPool();
        int maxPending = pool.getParallelism() * 2 + 1;
        ArrayDeque<ForkJoinTask<HashMap<ChunkPos, BitSet>>> pending = new ArrayDeque<>();

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
                mergeHits(pending.removeFirst().join());
            }
        }

        while (!pending.isEmpty()) {
            mergeHits(pending.removeFirst().join());
        }

        finishRayPhase();
    }

    private void mergeHits(Map<ChunkPos, BitSet> local) {
        for (Map.Entry<ChunkPos, BitSet> e : local.entrySet()) {
            perChunk.merge(e.getKey(), e.getValue(), (a, b) -> { a.or(b); return a; });
        }
    }

    private synchronized void finishRayPhase() {
        if (isAusf3Complete) return;
        orderedChunks.addAll(perChunk.keySet());
        orderedChunks.sort(comparator);
        isAusf3Complete = true;
    }

    /** Marches one batch of ray directions against the read-only chunk cache. Pool-thread only. */
    private HashMap<ChunkPos, BitSet> marchBatch(double[] dirs, int count) {
        HashMap<ChunkPos, BitSet> hits = new HashMap<>();
        MutableBlockPos pos = new MutableBlockPos();

        for (int i = 0; i < count && !cancelled; i++) {
            double vx = dirs[i * 3];
            double vy = dirs[i * 3 + 1];
            double vz = dirs[i * 3 + 2];

            float rayStrength = strength * 0.3F;

            // Find the end of the ray (verbatim old march, including the r = 0,2,3,… sampling).
            for (int r = 0; r < radius + 1; r++) {

                int iY = (int) Math.floor(posY + (vy * r));
                if (iY < minY || iY > maxY) {
                    isContained = false;
                    break;
                }

                int iX = (int) Math.floor(posX + (vx * r));
                int iZ = (int) Math.floor(posZ + (vz * r));

                LevelChunk chunk = chunkCache.get(ChunkPos.pack(iX >> 4, iZ >> 4));
                BlockState blockState = chunk != null ? chunk.getBlockState(pos.set(iX, iY, iZ)) : AIR;
                if (blockState.getBlock().getExplosionResistance() >= 2_000_000)
                    break;

                rayStrength -= (float) (Math.pow(getNukeResistance(blockState) + 1, 3 * ((float) (r = (r > 0) ? r : 1)) / ((float) radius)) - 1);

                if (rayStrength > 0) {
                    if (!blockState.isAir()) {
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
        return hits;
    }

    private void setBit(Map<ChunkPos, BitSet> map, int x, int y, int z) {
        ChunkPos chunk = new ChunkPos(x >> 4, z >> 4);
        BitSet hitPositions = map.computeIfAbsent(chunk, k -> new BitSet((maxY - minY + 1) << 8));
        hitPositions.set(((maxY - y) << 8) + ((x - chunk.getMinBlockX()) << 4) + (z - chunk.getMinBlockZ()));
    }

    /**
     * The old resistance table, special cases included: liquids are nearly transparent to the blast,
     * and the old mod deliberately rated sandstone at 4 and obsidian at 18 (vanilla resistance 1200 —
     * without this, obsidian blast-bunkers survive a direct nuke, which the old mod did not allow).
     */
    public static float getNukeResistance(BlockState blockState) {
        if (blockState.liquid()) return 0.1F;
        Block b = blockState.getBlock();
        if (b == Blocks.SANDSTONE) return 4F;
        if (b == Blocks.OBSIDIAN) return 18F;
        return b.getExplosionResistance();
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void cancel() {
        cancelled = true;
        isAusf3Complete = true;
        orderedChunks.clear();
        perChunk.clear();
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

    /** Little comparator for roughly sorting chunks by distance to the centre. */
    public class CoordComparator implements Comparator<ChunkPos> {

        @Override
        public int compare(ChunkPos o1, ChunkPos o2) {
            int chunkX = ExplosionNukeRayBatched.this.posX >> 4;
            int chunkZ = ExplosionNukeRayBatched.this.posZ >> 4;

            int diff1 = Math.abs((chunkX - (o1.getMinBlockX() >> 4))) + Math.abs((chunkZ - (o1.getMinBlockZ() >> 4)));
            int diff2 = Math.abs((chunkX - (o2.getMinBlockX() >> 4))) + Math.abs((chunkZ - (o2.getMinBlockZ() >> 4)));

            return Integer.compare(diff1, diff2);
        }
    }

    // ------------------------------------------------------------------ destruction (server thread)

    public void destructionTick(int time) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + time) {
            if (perChunk.isEmpty()) return;
            processChunkBlocks(start, time);
        }
    }

    private BitSet hitArray;
    private ChunkPos destChunk;
    private boolean needsNewHitArray = true;
    private int index = 0;

    public void processChunkBlocks(long start, int time) {
        if (this.perChunk.isEmpty()) return;
        if (needsNewHitArray) {
            destChunk = orderedChunks.get(0);
            hitArray = perChunk.get(destChunk);
            index = hitArray.nextSetBit(0);
            needsNewHitArray = false;
        }

        int chunkX = destChunk.getMinBlockX();
        int chunkZ = destChunk.getMinBlockZ();

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
            if (blocksRemoved % 256 == 0 && System.currentTimeMillis() + 1 > start + time) {
                break;
            }
        }

        if (index < 0) {
            perChunk.remove(destChunk);
            orderedChunks.remove(0);
            needsNewHitArray = true;
        }
    }
}
