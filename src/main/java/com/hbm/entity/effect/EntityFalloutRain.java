package com.hbm.entity.effect;

import dev.golem.core.data.Tags;

import dev.golem.core.data.CompatEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;

import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.generic.BlockFallout;
import com.hbm.config.BombConfig;
import com.hbm.config.FalloutConfig;
import com.hbm.config.FalloutConfig.FalloutEntry;
import com.hbm.mixin.FireBlockAccessor;
import com.hbm.util.ChunkCarver;
import com.hbm.world.biome.ModBiomes;
import dev.golem.core.world.Biomes;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import com.hbm.entity.logic.EntityChunkLoader;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * The fallout sweep spawned when a nuclear crater completes, ported from 1.12.2's
 * {@code EntityFalloutRain}: it walks every chunk the fallout disc touches and "stomps" each column —
 * depositing the radioactive {@code fallout} ash layer (chance peaking at 70% of the scale),
 * petrifying/killing organics, converting terrain into the sellafite shade rings
 * ({@link FalloutConfig}), starting fires, and dropping now-unsupported soft blocks as falling blocks.
 * Invisible logic entity like {@code EntityNukeExplosionMK5} (NoopRenderer).
 *
 * <p><b>Threading (mirrors the fork):</b> the old entity scanned chunks off-thread on a ForkJoin pool
 * and applied block writes on the server thread within a {@code BombConfig.mk5} ms budget per tick.
 * This port does the same: {@link #tick} claims each chunk for background loading and harvests it with
 * {@code getChunkNow} on a later server tick, then hands the {@link LevelChunk} to the common pool where {@link #scanChunk} computes the
 * column stomps using <b>chunk-confined reads only</b> (workers must never call {@code Level} methods
 * — the server chunk map is main-thread-guarded), and the resulting ordered write lists are applied
 * back on the server thread under the budget. A fully single-threaded version processed 1–3 chunks
 * per tick and took minutes over a Fat Man's disc; the fork finished in tens of seconds.
 *
     * <p>Deferred vs the fork: the post-fallout thunderstorm. Biome coordinates are collected on the worker;
     * biome state is resolved and rewritten on the server thread.
 *
 * <p>The chunk lists survive save/reload via NBT ({@code chunks}/{@code outerChunks} int-pair arrays);
 * chunks leave the lists only once their writes are fully applied, so an interrupted sweep resumes
 * (in-flight scans are simply redone).
 */
public class EntityFalloutRain extends CompatEntity {

    private static final int MAX_SOLID_DEPTH = 3;
    private static final int MIN_ANGLE_STEPS = 18;
    private static final int SPOKE_STEP_BLOCKS = 8;
    private static final int MAX_CLAIM_WINDOW_PER_WORKER = 256;
    private static final int MAX_SCAN_FAILURE_RETRIES = 3;

    private int scale = 1;
    /** Radius 0, as upstream's {@code EntityExplosionChunkloading}: the sweep keeps its own chunk up so
     * it can keep resolving the rest of the disc even with no player nearby. */
    private final EntityChunkLoader chunkLoader = new EntityChunkLoader(this, 0);

    /** Chunks fully inside the disc, then the boundary ring (whose columns are clamped to the radius). */
    private LongArrayList chunksToProcess = new LongArrayList();
    private LongArrayList outerChunksToProcess = new LongArrayList();
    private boolean gathered = false;

    /** Chunks claimed by this sweep — requested or scanning — so they are handed out only once. */
    private final LongOpenHashSet inFlight = new LongOpenHashSet();
    /**
     * Chunks asked for but not in memory yet (server thread only).
     *
     * <p>The disc reaches far past whatever a player had loaded, so most of its chunks must be read from
     * disk or generated. Asking for one and <em>waiting</em> — which is what {@code getChunk} does — puts
     * that whole cost on the server thread, one stall per chunk. These are requested instead and
     * collected when they arrive, so the generation runs on the chunk system's own workers.
     */
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet claimedChunks =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /**
     * How many chunks currently need each claim, so a chunk wanted by two neighbours is held once and
     * released once.
     */
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap claimRefs =
            new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    /** Chunks this sweep is holding, so they can all be dropped if it is removed mid-run. */
    private final it.unimi.dsi.fastutil.longs.LongOpenHashSet heldChunks =
            new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
    /** Scans handed to the pool and not yet applied. Bounds pool width; requests are counted separately. */
    private int activeScans;
    /** Scan results awaiting application, produced by pool workers. */
    private final ConcurrentLinkedQueue<ChunkScan> completedScans = new ConcurrentLinkedQueue<>();
    /** Server-thread retry counts; a permanently failing chunk must abort the sweep, not vanish. */
    private final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap scanFailures =
            new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
    private boolean failed;
    /** The scan currently being applied (may span ticks if the budget runs out mid-chunk). */
    private ChunkScan applying;

    // --- instrumentation: where the sweep's wall-clock goes (see IMPLEMENTATION-HISTORY §19z-h) ---
    private final long createdAt = System.currentTimeMillis();
    private long chunkResolveMs;
    private long applyMs;
    private int chunksResolved;
    private int chunksRequested;
    private int writesApplied;
    private int writesFallout, writesFire, writesOther, writesFalling, cellsNotified;
    private int fallingMarkersApplied, duplicateStateWritesRemoved, duplicateFallingMarkersRemoved;
    private long msFallout, msFire, msOther, msFalling, msNotify;
    private long msPlace, msNeighbours, msShapes;
    private int borderCells, borderCellsMissingNeighbour;
    private int chunkWaitPolls, centreApplyParks, neighbourhoodParks;
    /** Set by applyWrites when it stepped aside for a neighbourhood rather than running out of budget. */
    private boolean lastApplyParked;
    /** Peak simultaneous loading-only claims. */
    private int peakClaims;
    private int peakUnresolvedRequests;
    private final int scanParallelism = Math.max(1, ForkJoinPool.commonPool().getParallelism());
    private final int claimWindowPerWorker = Math.max(1,
            Math.min(MAX_CLAIM_WINDOW_PER_WORKER, BombConfig.falloutClaimWindow));
    private final int maxUnresolvedRequests = scanParallelism * claimWindowPerWorker;
    /** Wall clock between consecutive sweep ticks: how long the REST of the server tick took. */
    private long lastTickAt, maxTickGapMs, sumTickGapMs;
    private int tickGaps;
    private long msBorderMissing;
    private int sweepTicks;
    private boolean logged;

    public EntityFalloutRain(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void setScale(int scale) {
        this.scale = Math.max(scale, 1);
    }

    public int getScale() {
        return scale;
    }

    /** Claims the source chunk before the completed MK5 blast releases its own ticket. */
    public void startChunkLoading() {
        chunkLoader.tick();
    }

    public boolean isChunkLoading() {
        return chunkLoader.isLoading();
    }

    /** Diagnostics for the real-world loading pipeline and its GameTest. */
    public int getChunksRequested() {
        return chunksRequested;
    }

    public int getPeakUnresolvedRequests() {
        return peakUnresolvedRequests;
    }

    public int getMaxUnresolvedRequests() {
        return maxUnresolvedRequests;
    }

    /** Diagnostics seam: the state writes one column's stomp emits (last write per position wins). */
    public static java.util.Map<BlockPos, BlockState> stompColumnForTest(ServerLevel level, int x, int z,
                                                                        double distPercent, RandomSource rand) {
        ChunkScan scan = new ChunkScan(ChunkPos.pack(x >> 4, z >> 4));
        stompColumn(scan, level.getChunk(x >> 4, z >> 4), x, z, distPercent, rand);
        java.util.Map<BlockPos, BlockState> writes = new java.util.LinkedHashMap<>();
        for (BlockWrite write : scan.stateWrites) writes.put(BlockPos.of(write.pos()), write.state());
        return writes;
    }

    /** Diagnostics seam proving the references' separate last-state/first-falling position maps. */
    public static boolean writeDeduplicationContractForTest(BlockState first, BlockState last) {
        ChunkScan scan = new ChunkScan(0L);
        long pos = BlockPos.asLong(1, 2, 3);
        scan.addFallingMarker(pos, first);
        scan.addFallingMarker(pos, last);
        scan.addStateWrite(pos, first);
        scan.addStateWrite(pos, last);
        return scan.stateWrites.size() == 1
                && scan.stateWrites.get(0).pos() == pos
                && scan.stateWrites.get(0).state() == last
                && scan.fallingWrites.size() == 1
                && scan.fallingWrites.get(0).pos() == pos
                && scan.fallingWrites.get(0).state() == first
                && scan.duplicateStateWrites == 1
                && scan.duplicateFallingMarkers == 1;
    }

    @Override
    protected void defineData(dev.golem.core.data.SynchedData data) {
        // No synced data — server-driven and invisible (the old SCALE DataParameter fed a renderer we don't have).
    }

    @Override
    protected boolean onHurt(DamageSource source, float damage) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;

        if (failed) {
            discard();
            return;
        }

        chunkLoader.tick();

        if (!gathered && chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()) {
            gatherChunks();
        }
        gathered = true;

        // Force the lazy transform table to build on the server thread — its unsynchronized first
        // build must not race between pool workers.
        FalloutConfig.entries();

        dispatchScans(server);
        applyScans(server);

        sweepTicks++;
        // The sweep can time its own work but not the server's. The gap between its ticks is the rest of
        // the tick, and its shape says what that is: flat means a steady per-tick load (simulation of held
        // chunks, ash and fire ticks), spiky means bursts (chunk generation landing).
        long now = System.currentTimeMillis();
        if (lastTickAt != 0) {
            long gap = now - lastTickAt;
            sumTickGapMs += gap;
            tickGaps++;
            if (gap > maxTickGapMs) maxTickGapMs = gap;
        }
        lastTickAt = now;

        if (chunksToProcess.isEmpty() && outerChunksToProcess.isEmpty()
                && inFlight.isEmpty() && completedScans.isEmpty() && applying == null
                && claimedChunks.isEmpty()) {
            logSummary();
            discard();
        }
    }

    /**
     * Keeps the worker pool fed <b>without ever blocking the server thread on a chunk</b>.
     *
     * <p>Chunks already in memory go straight to the pool. Anything else is <em>claimed</em> with a
     * loading-only ticket, which is a registration and nothing more, and picked up on a later tick once
     * the chunk system has produced it in the background. Claims are dropped as their chunks are
     * consumed, so the window rolls across the disc.
     *
     * <p>The request depth is scan workers times the per-entity, 1..256-clamped snapshot of
     * {@code BombConfig.falloutClaimWindow}. It must remain deep enough to hide generation latency but
     * not flood worldgen: a Tsar at the former value 48 held 1,983 unique neighbourhood chunks and still
     * parked 106,271 times while producing 10-second tick gaps.
     *
     * <p><b>Both earlier versions of this method blocked, and the second one only looked like it did
     * not.</b> The original called {@code server.getChunk}, an outright synchronous load-and-generate.
     * §19z-h replaced it with {@code ServerChunkCache.getChunkFuture(…, create = true)} on the belief
     * that this was the non-blocking half of the same call. It is not: called from the server thread it
     * runs {@code mainThreadProcessor.managedBlock(future::isDone)} and waits for the chunk exactly like
     * {@code getChunk} does. A real Fat Man measured <b>20.6 s of server thread across 1,057 generated
     * chunks</b> — about 19 ms each — with a doomsday warhead at 57 s over 3,177. {@code ntm-next}'s
     * {@code ChunkUtil} carries the warning in one line: <i>"Ticket only; never getChunkFuture - blocks
     * server thread"</i>.
     */
    private void dispatchScans(ServerLevel server) {
        int maxInFlight = scanParallelism;
        int maxClaimed = maxUnresolvedRequests;

        // 1. Claimed chunks that have arrived since last tick.
        if (!claimedChunks.isEmpty()) {
            var iterator = claimedChunks.iterator();
            while (iterator.hasNext() && activeScans < maxInFlight) {
                long packed = iterator.nextLong();
                LevelChunk chunk = server.getChunkSource()
                        .getChunkNow(ChunkPos.getX(packed), ChunkPos.getZ(packed));
                if (chunk == null) {
                    chunkWaitPolls++;
                    continue; // still being generated; look again next tick
                }
                iterator.remove();
                submitScan(packed, chunk, outerChunksToProcess.contains(packed));
            }
        }

        // 2. Resident chunks, then new claims.
        while (activeScans < maxInFlight || claimedChunks.size() < maxClaimed) {
            boolean outer = false;
            long packed = nextUndispatched(chunksToProcess);
            if (packed == Long.MIN_VALUE) {
                packed = nextUndispatched(outerChunksToProcess);
                outer = true;
            }
            if (packed == Long.MIN_VALUE) return;

            int cx = ChunkPos.getX(packed);
            int cz = ChunkPos.getZ(packed);
            LevelChunk resident = server.getChunkSource().getChunkNow(cx, cz);
            if (resident != null) {
                if (activeScans >= maxInFlight) return; // pool is full; leave it for next tick
                chunksResolved++;
                // Held even though this chunk is already here: the notify pass reads across its borders.
                claimWithNeighbours(cx, cz);
                submitScan(packed, resident, outer);
                continue;
            }

            if (claimedChunks.size() >= maxClaimed) return;
            long claimStart = System.currentTimeMillis();
            claimWithNeighbours(cx, cz);
            chunkResolveMs += System.currentTimeMillis() - claimStart;
            claimedChunks.add(packed);
            if (claimedChunks.size() > peakUnresolvedRequests) peakUnresolvedRequests = claimedChunks.size();
            chunksResolved++;
            chunksRequested++;
            // Claimed so nextUndispatched will not hand it out again while it loads.
            inFlight.add(packed);
        }
    }

    /**
     * Claims a chunk <b>and its eight neighbours</b>.
     *
     * <p>The neighbours are the point. The notify pass runs {@code updateNeighborsAt}, which reads the six
     * blocks around every converted cell through {@code Level.getBlockState} — and on a chunk border that
     * reads into the next chunk, where {@code getBlockState} <b>synchronously loads and generates</b> if
     * nobody has it. Claiming only the chunk being worked on left every border cell able to trigger a
     * full chunk generation inside what looks like a block update.
     */
    private void claimWithNeighbours(int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) claimOne(ChunkPos.pack(chunkX + dx, chunkZ + dz));
        }
    }

    private void releaseWithNeighbours(int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) releaseOne(ChunkPos.pack(chunkX + dx, chunkZ + dz));
        }
    }

    private void claimOne(long packed) {
        if (claimRefs.addTo(packed, 1) == 0) {
            heldChunks.add(packed);
            dev.golem.core.world.Chunks.claimForLoading((ServerLevel) level(),
                    ChunkPos.getX(packed), ChunkPos.getZ(packed));
            if (claimRefs.size() > peakClaims) peakClaims = claimRefs.size();
        }
    }

    private void releaseOne(long packed) {
        if (claimRefs.addTo(packed, -1) - 1 <= 0) {
            claimRefs.remove(packed);
            if (heldChunks.remove(packed)) {
                dev.golem.core.world.Chunks.releaseForLoading((ServerLevel) level(),
                        ChunkPos.getX(packed), ChunkPos.getZ(packed));
            }
        }
    }

    /** Drops the claim on one chunk and its neighbours once the sweep is finished with it. */
    private void dropClaim(long packed) {
        releaseWithNeighbours(ChunkPos.getX(packed), ChunkPos.getZ(packed));
    }

    /** Hands one resolved chunk to the pool. Server thread only. */
    private void submitScan(long packed, LevelChunk chunk, boolean clamp) {
        inFlight.add(packed);
        activeScans++;
        double ex = getX(), ez = getZ();
        int sc = getScale();
        ChunkScan scan = new ChunkScan(packed);
        try {
            ForkJoinPool.commonPool().execute(() -> {
            try {
                scanChunk(scan, chunk, clamp, ex, ez, sc);
                scan.completed = true;
            } catch (Throwable e) {
                // The scan is deliberately published as failed, never as an empty successful scan.
                // Its local writes are discarded; the server thread decides whether to retry or abort.
                scan.failure = e;
            } finally {
                completedScans.add(scan);
            }
            });
        } catch (Throwable e) {
            // Rejection happens before the worker wrapper can publish a result. Publish the same
            // explicit failure here so activeScans/claims are released by the normal retry/abort path.
            scan.failure = e;
            completedScans.add(scan);
        }
    }

    /** Last-to-first like the old removeLong(size-1) processing order, skipping in-flight chunks. */
    private long nextUndispatched(LongArrayList list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            long packed = list.getLong(i);
            if (!inFlight.contains(packed)) return packed;
        }
        return Long.MIN_VALUE;
    }

    /** Applies completed scans' block writes on the server thread within the old mk5 ms budget. */
    private void applyScans(ServerLevel server) {
        long applyStart = System.currentTimeMillis();
        try {
            applyScansTimed(server);
        } finally {
            applyMs += System.currentTimeMillis() - applyStart;
        }
    }

    /**
     * One line per sweep with the phase split. Same reasoning as the crater's: without it, "the fallout
     * is slow" cannot be told apart from "the chunk loading behind the fallout is slow".
     */
    private void logSummary() {
        if (logged) return;
        logged = true;
        long total = System.currentTimeMillis() - createdAt;
        com.hbm.Constants.LOG.info("[FALLOUT] scale={} done in {} ms wall over {} ticks: chunks {} ms "
                        + "({} resolved, {} requested on demand), apply {} ms ({} writes, {} writes/ms)",
                getScale(), total, sweepTicks, chunkResolveMs, chunksResolved, chunksRequested,
                applyMs, writesApplied, applyMs > 0 ? writesApplied / applyMs : writesApplied);
        com.hbm.Constants.LOG.info("[FALLOUT]   writes: terrain {} in {} ms | ash {} in {} ms | "
                        + "fire {} in {} ms | falling-block fall calls {} from {} unique markers in {} ms",
                writesOther, msOther / 1_000_000L, writesFallout, msFallout / 1_000_000L,
                writesFire, msFire / 1_000_000L, writesFalling, fallingMarkersApplied,
                msFalling / 1_000_000L);
        com.hbm.Constants.LOG.info("[FALLOUT]   scan dedup: {} superseded state writes and {} duplicate "
                        + "falling markers removed before apply",
                duplicateStateWritesRemoved, duplicateFallingMarkersRemoved);
        com.hbm.Constants.LOG.info("[FALLOUT]   notify pass: {} cells in {} ms ({} cells/ms) = "
                        + "onPlace/removal {} ms + updateNeighborsAt {} ms [{}] + updateNeighbourShapes {} ms [{}]",
                cellsNotified, msNotify / 1_000_000L,
                msNotify > 1_000_000L ? cellsNotified / (msNotify / 1_000_000L) : cellsNotified,
                msPlace / 1_000_000L,
                msNeighbours / 1_000_000L, BombConfig.falloutNeighbourUpdates ? "on" : "OFF",
                msShapes / 1_000_000L, BombConfig.falloutShapeUpdates ? "on" : "OFF");
        com.hbm.Constants.LOG.info("[FALLOUT]   border cells: {} of them, {} with an unloaded neighbour "
                        + "chunk, costing {} ms of the neighbour pass",
                borderCells, borderCellsMissingNeighbour, msBorderMissing / 1_000_000L);
        com.hbm.Constants.LOG.info("[FALLOUT]   loading pipeline: {} scan workers x {} request window = "
                        + "{} unresolved requests max (+ up to {} active scans); peak {} unresolved/{} "
                        + "unique chunks; waits: {} chunk polls, "
                        + "{} centre apply parks, {} neighbourhood parks",
                scanParallelism, claimWindowPerWorker, maxUnresolvedRequests, scanParallelism,
                peakUnresolvedRequests, peakClaims, chunkWaitPolls, centreApplyParks, neighbourhoodParks);
        // Peak claims measures the memory/generation working set. Golem's loading-only tickets do not
        // make these chunks simulate.
        com.hbm.Constants.LOG.info("[FALLOUT]   server tick gap while sweeping: mean {} ms, worst {} ms "
                        + "over {} gaps — the sweep's own work was {} ms of it",
                tickGaps > 0 ? sumTickGapMs / tickGaps : 0, maxTickGapMs, tickGaps, applyMs + chunkResolveMs);
    }

    private void applyScansTimed(ServerLevel server) {
        long deadline = System.nanoTime() + BombConfig.mk5 * 1_000_000L;
        // A chunk that parks waiting for its neighbourhood steps aside for the next one rather than
        // ending the tick — but if every queued chunk is parked there is nothing else to do, so the
        // number of consecutive parks is bounded by how many are waiting.
        int parksThisTick = 0;
        while (true) {
            if (applying == null) applying = completedScans.poll();
            if (applying == null) return;

            if (!applying.completed) {
                long failedChunk = applying.chunkPos;
                int attempts = scanFailures.addTo(failedChunk, 1) + 1;
                activeScans--;
                inFlight.remove(failedChunk);
                dropClaim(failedChunk);
                Throwable failure = applying.failure;
                applying = null;
                if (attempts <= MAX_SCAN_FAILURE_RETRIES) {
                    com.hbm.Constants.LOG.error("[FALLOUT] chunk scan failed for [{}, {}]; retry {}/{}",
                            ChunkPos.getX(failedChunk), ChunkPos.getZ(failedChunk), attempts,
                            MAX_SCAN_FAILURE_RETRIES, failure);
                } else {
                    com.hbm.Constants.LOG.error("[FALLOUT] chunk scan failed {} times for [{}, {}]; aborting sweep",
                            attempts, ChunkPos.getX(failedChunk), ChunkPos.getZ(failedChunk), failure);
                    failed = true;
                    discard();
                    return;
                }
                continue;
            }

            lastApplyParked = false;
            if (!applyWrites(server, applying, deadline)) {
                if (!lastApplyParked) return; // budget exhausted mid-chunk
                completedScans.add(applying);
                applying = null;
                if (++parksThisTick > completedScans.size() || System.nanoTime() >= deadline) return;
                continue;
            }

            long packed = applying.chunkPos;
            duplicateStateWritesRemoved += applying.duplicateStateWrites;
            duplicateFallingMarkersRemoved += applying.duplicateFallingMarkers;
            activeScans--;
            inFlight.remove(packed);
            dropClaim(packed);
            chunksToProcess.rem(packed);
            outerChunksToProcess.rem(packed);
            scanFailures.remove(packed);
            applying = null;
        }
    }

    /**
     * One chunk's final state writes and separately captured first-wins falling entries, plus the state
     * the two-pass apply carries between ticks.
     *
     * <p>Two passes, like both references: every cell is written into the chunk's sections first, and
     * only then is each changed cell's neighbourhood notified. That ordering matters — the notify pass
     * runs {@code updateNeighbourShapes}, and the shapes it should see are the finished ones.
     */
    private static final class ChunkScan {
        final long chunkPos;
        final List<BlockWrite> stateWrites = new ArrayList<>();
        final List<FallingWrite> fallingWrites = new ArrayList<>();
        final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap stateWriteIndices =
                new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        final it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap fallingWriteIndices =
                new it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap();
        final List<BiomeWrite> biomes = new ArrayList<>();
        int duplicateStateWrites;
        int duplicateFallingMarkers;
        boolean completed;
        Throwable failure;
        int stateAppliedUpTo = 0;
        int fallingAppliedUpTo = 0;

        /** Cells the write pass actually changed, and what they were, for the notify pass. */
        final LongArrayList changedPos = new LongArrayList();
        final List<BlockState> changedOld = new ArrayList<>();
        int notifiedUpTo = 0;
        boolean[] touched;
        boolean stateWritesDone = false;
        boolean fallingWritesDone = false;
        boolean biomesDone = false;
        boolean biomesChanged = false;
        ChunkScan(long chunkPos) {
            this.chunkPos = chunkPos;
            stateWriteIndices.defaultReturnValue(-1);
            fallingWriteIndices.defaultReturnValue(-1);
        }

        void addStateWrite(long pos, BlockState state) {
            int existing = stateWriteIndices.get(pos);
            if (existing >= 0) {
                stateWrites.set(existing, new BlockWrite(pos, state));
                duplicateStateWrites++;
                return;
            }
            stateWriteIndices.put(pos, stateWrites.size());
            stateWrites.add(new BlockWrite(pos, state));
        }

        void addFallingMarker(long pos, BlockState capturedState) {
            if (fallingWriteIndices.get(pos) >= 0) {
                duplicateFallingMarkers++;
                return;
            }
            fallingWriteIndices.put(pos, fallingWrites.size());
            fallingWrites.add(new FallingWrite(pos, capturedState));
        }

        void clearWrites() {
            stateWrites.clear();
            fallingWrites.clear();
            stateWriteIndices.clear();
            fallingWriteIndices.clear();
            duplicateStateWrites = 0;
            duplicateFallingMarkers = 0;
        }
    }

    private record BlockWrite(long pos, BlockState state) {}

    private record FallingWrite(long pos, BlockState state) {}

    private record BiomeWrite(int x, int z, double distancePercent, int scale) {}

    /**
     * Applies one chunk's writes, <b>through its sections</b>, and repairs the chunk once at the end.
     *
     * <p>Returns true when the chunk is completely finished; false when the tick's budget ran out
     * part-way, in which case the next tick resumes from where it stopped.
     *
     * <p><b>Why not {@code Level.setBlock} per write.</b> That is what this did, and on a real blast it
     * measured <b>27 conversions per millisecond</b> — 37 µs each, against 1,560/ms for the same call in
     * a quiet benchmark. Per block it pays a chunk lookup, four heightmap updates, a POI lookup pair, a
     * chunk-holder change mark, and a light-engine {@code checkBlock} that allocates a task and submits
     * it to the chunk task dispatcher. All of that is per-<em>chunk</em> work being done a million times;
     * a doomsday warhead does 932,217 terrain conversions and spent <b>35 s</b> on them.
     *
     * <p>So the writes go straight into the sections and the chunk is repaired once — the same split the
     * crater uses ({@link ChunkCarver}) and the same one both references use for this sweep
     * ({@code ChunkUtil.applyAndSwap} plus a per-chunk notify pass). What is <em>kept</em> is the
     * per-block neighbour and shape notification, because both references keep it and it is what makes
     * unsupported blocks pop and attached blocks react; it is simply no longer bundled with everything
     * else.
     *
     * <p>Two cells still take the slow path, exactly as in the references: anything involving a block
     * entity (a raw section write would orphan it and leave its POI record behind) and the falling-block
     * markers, which are entity spawns rather than writes.
     */
    private boolean applyWrites(ServerLevel server, ChunkScan scan, long deadline) {
        int cx = ChunkPos.getX(scan.chunkPos);
        int cz = ChunkPos.getZ(scan.chunkPos);
        LevelChunk chunk = server.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) {
            centreApplyParks++;
            lastApplyParked = true;
            return false;
        }

        if (scan.touched == null) scan.touched = new boolean[chunk.getSections().length];
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if (!scan.stateWritesDone && !writePass(server, chunk, scan, pos, deadline)) return false;

        if (!scan.biomesDone) {
            scan.biomesChanged = applyBiomeWrites(server, chunk, scan.biomes);
            scan.biomesDone = true;
        }

        // Park rather than read through a chunk nobody has.
        //
        // The notify pass runs updateNeighborsAt, which reads the six blocks around each cell; on a chunk
        // border one of those is in the next chunk, and Level.getBlockState there does not fail — it
        // *generates*, synchronously, on this thread. The 3x3 claim above asks for the neighbourhood, but
        // asking is not arriving, and a real blast measured **111 cells that ran ahead of it costing
        // 24,508 ms** — 98% of the whole notify pass, at 220 ms a cell. So the pass does not begin until
        // the neighbourhood is actually resident. This is what both references do: their workers park on
        // a missing chunk instead of touching it.
        if (!neighbourhoodResident(server, cx, cz)) {
            neighbourhoodParks++;
            lastApplyParked = true;
            return false;
        }
        return notifyPass(server, chunk, scan, pos, deadline, cx, cz);
    }

    /** True when all eight neighbours of a chunk are in memory, so a border read cannot generate one. */
    private boolean neighbourhoodResident(ServerLevel server, int chunkX, int chunkZ) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (server.getChunkSource().getChunkNow(chunkX + dx, chunkZ + dz) == null) return false;
            }
        }
        return true;
    }

    /** Pass one: the section writes. Records what changed so pass two can notify it. */
    private boolean writePass(ServerLevel server, LevelChunk chunk, ChunkScan scan,
                              BlockPos.MutableBlockPos pos, long deadline) {
        List<BlockWrite> writes = scan.stateWrites;
        LevelChunkSection[] sections = chunk.getSections();

        for (int i = scan.stateAppliedUpTo; i < writes.size(); i++) {
            if ((i & 63) == 0 && System.nanoTime() >= deadline) {
                scan.stateAppliedUpTo = i;
                return false;
            }
            BlockWrite write = writes.get(i);
            writesApplied++;
            long packed = write.pos();
            int x = BlockPos.getX(packed), y = BlockPos.getY(packed), z = BlockPos.getZ(packed);
            pos.set(x, y, z);
            long kindStart = System.nanoTime();
            BlockState newState = write.state();

            int index = server.getSectionIndex(y);
            if (index < 0 || index >= sections.length) continue;
            LevelChunkSection section = sections[index];
            BlockState oldState = section.getBlockState(x & 15, y & 15, z & 15);
            if (oldState == newState) continue;

            if (oldState.hasBlockEntity() || newState.hasBlockEntity()) {
                // Slow path on purpose: block-entity lifecycle and POI have to stay vanilla's business.
                server.setBlock(pos, newState, Block.UPDATE_ALL);
            } else {
                section.setBlockState(x & 15, y & 15, z & 15, newState, false);
                scan.touched[index] = true;
                scan.changedPos.add(packed);
                scan.changedOld.add(oldState);
            }

            long spent = System.nanoTime() - kindStart;
            if (newState.is(ModFalloutBlocks.fallout.get())) {
                writesFallout++;
                msFallout += spent;
            } else if (newState.is(Blocks.FIRE)) {
                writesFire++;
                msFire += spent;
            } else {
                writesOther++;
                msOther += spent;
            }
        }

        scan.stateAppliedUpTo = writes.size();
        scan.stateWritesDone = true;
        return true;
    }

    /** Reference ordering: all state writes and their notifications complete before falling entries. */
    private boolean fallingPass(ServerLevel server, LevelChunk chunk, ChunkScan scan,
                                BlockPos.MutableBlockPos pos, long deadline) {
        float stonebrickRes = Blocks.STONE_BRICKS.getExplosionResistance();
        for (int i = scan.fallingAppliedUpTo; i < scan.fallingWrites.size(); i++) {
            if ((i & 63) == 0 && System.nanoTime() >= deadline) {
                scan.fallingAppliedUpTo = i;
                return false;
            }
            FallingWrite write = scan.fallingWrites.get(i);
            writesApplied++;
            fallingMarkersApplied++;
            long packed = write.pos();
            pos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            long kindStart = System.nanoTime();
            BlockState now = chunk.getBlockState(pos);
            BlockState captured = write.state();
            // The references retain the first captured state and remove only if the same block still owns
            // the source position when falling begins.
            if (now.is(captured.getBlock())) {
                float hardness = now.getDestroySpeed(server, pos);
                if (hardness >= 0.0F && hardness <= stonebrickRes) {
                    FallingBlockEntity falling = FallingBlockEntity.fall(server, pos.immutable(), captured);
                    falling.dropItem = false;
                    writesFalling++;
                }
            }
            msFalling += System.nanoTime() - kindStart;
        }
        scan.fallingAppliedUpTo = scan.fallingWrites.size();
        scan.fallingWritesDone = true;
        return true;
    }

    /**
     * Pass two: everything vanilla would have done around each changed cell, then one repair for the
     * whole chunk.
     *
     * <p>{@code onPlace} is called by hand here. A section write skips it, and it is not optional — it is
     * what schedules {@link com.hbm.blocks.generic.BlockFallout}'s tick, so ash placed without it would
     * sit there emitting no radiation at all.
     */
    private boolean notifyPass(ServerLevel server, LevelChunk chunk, ChunkScan scan,
                               BlockPos.MutableBlockPos pos, long deadline, int cx, int cz) {
        for (int i = scan.notifiedUpTo; i < scan.changedPos.size(); i++) {
            if ((i & 63) == 0 && System.nanoTime() >= deadline) {
                scan.notifiedUpTo = i;
                return false;
            }
            long notifyStart = System.nanoTime();
            long packed = scan.changedPos.getLong(i);
            pos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            BlockState oldState = scan.changedOld.get(i);
            BlockState newState = chunk.getBlockState(pos);
            if (oldState == newState) continue;

            // The removal hook that UPDATE_NEIGHBORS would have run inside setBlockState, and onPlace,
            // which a section write skips — it is what schedules BlockFallout's tick.
            if (!oldState.is(newState.getBlock())) {
                dev.golem.core.world.Compat.affectNeighborsAfterRemoval(server, pos, oldState);
            }
            newState.onPlace(server, pos.immutable(), oldState, false);
            long afterPlace = System.nanoTime();
            msPlace += afterPlace - notifyStart;

            // Is this cell on a chunk border with a neighbour nobody has loaded? That is the case where
            // updateNeighborsAt's getBlockState turns into a synchronous chunk generation, and this
            // counts it so the claim above can be shown to be working.
            boolean missingNeighbour = false;
            int lx = pos.getX() & 15, lz = pos.getZ() & 15;
            if (lx == 0 || lx == 15 || lz == 0 || lz == 15) {
                borderCells++;
                int ncx = lx == 0 ? cx - 1 : (lx == 15 ? cx + 1 : cx);
                int ncz = lz == 0 ? cz - 1 : (lz == 15 ? cz + 1 : cz);
                missingNeighbour = (ncx != cx && server.getChunkSource().getChunkNow(ncx, cz) == null)
                        || (ncz != cz && server.getChunkSource().getChunkNow(cx, ncz) == null);
                if (missingNeighbour) borderCellsMissingNeighbour++;
            }

            if (BombConfig.falloutNeighbourUpdates) {
                dev.golem.core.world.Compat.updateNeighborsAt(server, pos, newState.getBlock());
            }
            long afterNeighbours = System.nanoTime();
            msNeighbours += afterNeighbours - afterPlace;
            if (missingNeighbour) msBorderMissing += afterNeighbours - afterPlace;

            if (BombConfig.falloutShapeUpdates) {
                newState.updateNeighbourShapes(server, pos, Block.UPDATE_CLIENTS);
            }
            msShapes += System.nanoTime() - afterNeighbours;

            cellsNotified++;
            msNotify += System.nanoTime() - notifyStart;
        }
        scan.notifiedUpTo = scan.changedPos.size();

        if (!scan.fallingWritesDone && !fallingPass(server, chunk, scan, pos, deadline)) return false;

        if (scan.changedPos.isEmpty() && scan.biomesChanged) {
            // No block write will produce a packet for this chunk, but clients still need the new biome
            // container. This is the same logical-main-thread handoff as the full repair path below.
            dev.golem.core.world.Compat.markUnsaved(chunk);
            Biomes.resend(server, chunk);
            return true;
        }

        ChunkCarver.repairAndResend(server, chunk, cx, cz, scan.touched);
        return true;
    }

    /**
     * The old {@code gatherChunks}: the boundary ring is sampled at {@code max(18, 20·scale/32)}
     * angles; the interior along the same spokes every 8 blocks. Dedup'd into inner/outer lists.
     */
    private void gatherChunks() {
        int radius = getScale();
        int angleSteps = Math.max(MIN_ANGLE_STEPS, 20 * radius / 32);

        LongOpenHashSet outer = new LongOpenHashSet();
        LongOpenHashSet inner = new LongOpenHashSet();

        double px = getX();
        double pz = getZ();

        for (int step = 0; step <= angleSteps; step++) {
            double theta = step * (2.0 * Math.PI) / angleSteps;
            double cos = Math.cos(theta);
            double sin = Math.sin(theta);

            int cx = ((int) Math.floor(px + radius * cos)) >> 4;
            int cz = ((int) Math.floor(pz - radius * sin)) >> 4;
            outer.add(ChunkPos.pack(cx, cz));

            for (int d = 0; d <= radius; d += SPOKE_STEP_BLOCKS) {
                int ix = ((int) Math.floor(px + d * cos)) >> 4;
                int iz = ((int) Math.floor(pz - d * sin)) >> 4;
                long packed = ChunkPos.pack(ix, iz);
                if (!outer.contains(packed)) inner.add(packed);
            }
        }
        inner.removeAll(outer);

        chunksToProcess = new LongArrayList(inner);
        outerChunksToProcess = new LongArrayList(outer);
        // Nearest-first, so the fallout spreads outwards from the blast the way it should look. Row-major
        // was tried and works, but it makes the conversion sweep across the landscape like a wipe, which
        // reads as a bug even though it is not; a deep enough claim window makes the ordering's effect on
        // neighbour coherence irrelevant, so this one is chosen for how it looks.
        sortByDistance(chunksToProcess, getX(), getZ());
        sortByDistance(outerChunksToProcess, getX(), getZ());
    }

    /**
     * Sorts a chunk list <b>furthest first</b>, because {@link #nextUndispatched} hands work out from the
     * end of the list — so the sweep converts nearest-first and the fallout spreads outwards.
     */
    private static void sortByDistance(LongArrayList chunks, double centreX, double centreZ) {
        long[] raw = chunks.toLongArray();
        int cx = ((int) Math.floor(centreX)) >> 4;
        int cz = ((int) Math.floor(centreZ)) >> 4;
        long[] keys = new long[raw.length];
        for (int i = 0; i < raw.length; i++) {
            long dx = ChunkPos.getX(raw[i]) - cx;
            long dz = ChunkPos.getZ(raw[i]) - cz;
            // Descending: the largest squared distance sorts first, so the nearest ends up last.
            keys[i] = -(dx * dx + dz * dz);
        }
        Integer[] order = new Integer[raw.length];
        for (int i = 0; i < raw.length; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Long.compare(keys[a], keys[b]));
        chunks.clear();
        for (int i : order) chunks.add(raw[i]);
    }

    // ------------------------------------------------------------------ worker side (pool threads)

    /**
     * Scans one chunk into an ordered write list. Runs on a pool thread: every read is confined to the
     * given {@link LevelChunk} (block states, the chunk-local heightmap, chunk-as-BlockGetter shape
     * queries) — never {@code Level}/{@code ServerLevel}, whose chunk access is main-thread-guarded.
     * Biome decisions are deferred because biome containers are not worker-safe.
     */
    private void scanChunk(ChunkScan scan, LevelChunk chunk, boolean clampToRadius, double ex, double ez, int scale) {
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        RandomSource rand = RandomSource.create();

        for (int x = minX; x < minX + 16; x++) {
            for (int z = minZ; z < minZ + 16; z++) {
                double distance = Math.hypot(x - ex, z - ez);
                if (clampToRadius && distance > scale) continue;
                double percent = distance * 100.0 / scale;
                if (scale >= 25) scan.biomes.add(new BiomeWrite(x, z, percent, scale));
                stompColumn(scan, chunk, x, z, percent, rand);
            }
        }
    }

    /** Resolves original biome holders and mutates biome containers only from the server-thread apply phase. */
    private static boolean applyBiomeWrites(ServerLevel server, LevelChunk chunk, List<BiomeWrite> writes) {
        if (writes.isEmpty()) return false;
        List<Biomes.Change> changes = new ArrayList<>(writes.size());
        for (BiomeWrite write : writes) {
            var original = chunk.getNoiseBiome(write.x() >> 2, 0, write.z() >> 2);
            ModBiomes.Type type = ModBiomes.changeFor(write.distancePercent(), write.scale(), original);
            if (type != null) {
                changes.add(new Biomes.Change(write.x(), write.z(), ModBiomes.holder(server.registryAccess(), type),
                        ModBiomes.preserve(type)));
            }
        }
        return Biomes.apply(chunk, changes);
    }

    /**
     * The old {@code stompColumn}: walk the column top-down, transforming at most
     * {@link #MAX_SOLID_DEPTH} solid blocks, depositing the fallout layer / fire on the way.
     * Emits writes instead of applying them.
     */
    private static void stompColumn(ChunkScan scan, LevelChunk chunk, int x, int z, double distPercent, RandomSource rand) {
        int solidDepth = 0;
        int bottomY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        float stonebrickRes = Blocks.STONE_BRICKS.getExplosionResistance();
        List<FalloutEntry> entries = FalloutConfig.entries();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos posUp = new BlockPos.MutableBlockPos();

        // Start at the surface, not the sky top (chunk-local heightmap; +1 = Level.getHeight parity).
        int top = Math.min(maxY, chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1);
        // 1.7 wrote each change into the world at once, so the next (lower) cell saw the column as already
        // transformed: ash lands on the ground under a canopy the sweep just burned away. The scan only
        // emits writes, so it carries the cell it last processed forward instead of re-reading the chunk.
        int lastY = Integer.MIN_VALUE;
        BlockState lastEffective = null;
        // The column as the sweep has left it, for the cells above y that the collapse re-reads.
        BlockState[] effectiveAbove = new BlockState[MAX_SOLID_DEPTH + 1];
        int effectiveTopY = Integer.MIN_VALUE;
        for (int y = top; y >= bottomY; y--) {
            if (solidDepth >= MAX_SOLID_DEPTH) return;

            pos.set(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            Block block = state.getBlock();
            if (state.isAir() || block == ModFalloutBlocks.fallout.get()) continue;

            // 1.7 stomp:164-167: a volcano core turns into its radioactive twin (same behaviour) and the
            // column walk carries on below it without counting it
            if (block instanceof com.hbm.blocks.bomb.BlockVolcano volcano && !volcano.radioactive) {
                scan.addStateWrite(pos.asLong(),
                        com.hbm.blocks.ModTerrainBlocks.volcanoCore(volcano.mode.id(true)).defaultBlockState());
                continue;
            }

            // fallout ash layer on the surface — chance peaks (0.1) at 70% of the scale
            BlockState stateUp = null;
            int upY = y + 1;
            boolean ash = false;
            if (upY <= maxY) {
                stateUp = lastY == upY ? lastEffective : chunk.getBlockState(posUp.set(x, upY, z));
            }
            if (solidDepth == 0 && stateUp != null
                    && BlockFallout.canOccupy(stateUp) && BlockFallout.canSupport(state, chunk, pos)) {
                double d = distPercent / 100.0;
                double chance = 0.1 - Math.pow(d - 0.7, 2.0);
                ash = chance >= rand.nextDouble();
            }

            // fires on flammables inside 65%; 1.7 had already set the ash, so the cell was no longer air
            if (distPercent < 65 && ((FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(state) > 0 && stateUp != null) {
                if (!ash && stateUp.isAir() && rand.nextInt(5) == 0) {
                    scan.addStateWrite(BlockPos.asLong(x, upY, z), Blocks.FIRE.defaultBlockState());
                }
            }

            // the transform table — first matching entry wins
            boolean transformed = false;
            BlockState effective = state;
            for (FalloutEntry entry : entries) {
                BlockState result = entry.eval(y, state, distPercent, rand, bottomY);
                if (result != null) {
                    scan.addStateWrite(pos.asLong(), result);
                    if (entry.isSolid()) solidDepth++;
                    transformed = true;
                    effective = result;
                    break;
                }
            }
            lastY = y;
            lastEffective = effective;
            // shift the remembered column down one cell
            if (effectiveTopY != y + 1) java.util.Arrays.fill(effectiveAbove, null);
            System.arraycopy(effectiveAbove, 0, effectiveAbove, 1, effectiveAbove.length - 1);
            effectiveAbove[0] = effective;
            effectiveTopY = y;
            boolean collapses = false;

            // 1.7 stomp:201-214: a soft block over air collapses, with the solid cells above it, as
            // falling blocks (no drops). 1.7 tests the ORIGINAL block's hardness (stone bricks' 6
            // resistance as the bar) and skips multiblock parts, then drops what is now in the world at
            // y .. y + depth, i.e. the transformed cells. The port had restricted this to untransformed cells.
            if (distPercent < 65 && y > bottomY && !(block instanceof com.hbm.blocks.multiblock.BlockMultiblockCore)
                    && !(block instanceof com.hbm.blocks.multiblock.BlockMultiblockCell)) {
                float hardnessHere = state.getDestroySpeed(chunk, pos);
                if (hardnessHere >= 0.0F && hardnessHere <= stonebrickRes) {
                    BlockState below = chunk.getBlockState(posUp.set(x, y - 1, z));
                    if (below.isAir()) {
                        for (int i = 0; i <= solidDepth; i++) {
                            int yy = y + i;
                            if (yy > maxY) break;
                            BlockPos p = new BlockPos(x, yy, z);
                            BlockState sAt = i < effectiveAbove.length && effectiveAbove[i] != null
                                    ? effectiveAbove[i] : chunk.getBlockState(p);
                            if (sAt.isAir()) continue;
                            float h = sAt.getDestroySpeed(chunk, p);
                            if (h >= 0.0F && h <= stonebrickRes) {
                                scan.addFallingMarker(p.asLong(), sAt);
                                if (i == 0) collapses = true;
                            }
                        }
                    }
                }
            }

            // The ash goes in last, against the support as the sweep leaves it. 1.7 set it first and let
            // onNeighborBlockChange silently remove it when the same pass burned the leaves / melted the snow
            // under it or dropped the sand; the end state is ash only on support that is still there.
            if (ash && !collapses && BlockFallout.canSupport(effective, chunk, pos)) {
                scan.addStateWrite(BlockPos.asLong(x, upY, z), ModFalloutBlocks.fallout.get().defaultBlockState());
            }

            if (!transformed && state.isCollisionShapeFullBlock(chunk, pos)) solidDepth++;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        chunkLoader.clearChunkLoader();
        claimedChunks.clear();
        claimRefs.clear();
        if (level() instanceof ServerLevel server) {
            for (it.unimi.dsi.fastutil.longs.LongIterator it = heldChunks.iterator(); it.hasNext(); ) {
                long packed = it.nextLong();
                dev.golem.core.world.Chunks.releaseForLoading(server,
                        ChunkPos.getX(packed), ChunkPos.getZ(packed));
            }
        }
        heldChunks.clear();
        super.remove(reason);
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void loadData(DataIn input) {
        chunkLoader.markRestoredFromNbt();
        setScale(input.getIntOr("scale", 1));
        chunksToProcess = fromPairs(input.getIntArray("chunks").orElse(new int[0]));
        outerChunksToProcess = fromPairs(input.getIntArray("outerChunks").orElse(new int[0]));
        gathered = input.getBooleanOr("gathered", !chunksToProcess.isEmpty() || !outerChunksToProcess.isEmpty());
    }

    @Override
    protected void saveData(DataOut output) {
        output.putInt("scale", scale);
        output.putIntArray("chunks", toPairs(chunksToProcess));
        output.putIntArray("outerChunks", toPairs(outerChunksToProcess));
        output.putBoolean("gathered", gathered);
    }

    private static LongArrayList fromPairs(int[] data) {
        LongArrayList list = new LongArrayList(data.length / 2);
        for (int i = 0; i + 1 < data.length; i += 2) list.add(ChunkPos.pack(data[i], data[i + 1]));
        return list;
    }

    private static int[] toPairs(LongArrayList coords) {
        int[] data = new int[coords.size() * 2];
        int i = 0;
        for (LongIterator it = coords.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            data[i++] = ChunkPos.getX(packed);
            data[i++] = ChunkPos.getZ(packed);
        }
        return data;
    }
}
