package com.hbm.handler.radiation;

import dev.golem.core.world.ChunkSidecar;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm.config.RadiationConfig;
import com.hbm.interfaces.IRadResistantBlock;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * The chunk radiation engine — a faithful port of the <i>behaviour</i> of 1.12.2's {@code RadiationSystemNT}
 * (the pocket model). Radiation is a per-block density (RAD/s) stored per <b>pocket</b>: a maximal
 * flood-filled region of non-{@link IRadResistantBlock} blocks within a 16³ section. Rad-resistant blocks
 * are walls, so a sealed lead room holds its radiation separately from the rest of its section. Radiation
 * diffuses between pockets across section faces (mass-conserving exchange toward equilibrium, weighted by
 * open contact area) and decays exponentially (t½ = {@link RadiationConfig#radHalfLifeSeconds}).
 *
 * <p>A step is in two halves and the split is the safety property, not an optimisation. {@link
 * WorldRadiationData#prepare()} builds every section the sweep will touch <b>on the server thread</b>,
 * because building reads block state; the sweep that follows runs on {@link #RAD_POOL} and touches
 * nothing but plain numeric arrays. The fork flood-fills off-thread and this deliberately does not.
 *
 * <p>The sweeps are parity-bucketed exactly as the fork's are — in a pass only chunks of one coordinate
 * parity exchange with their {@code +axis} neighbour, so every chunk is written by at most one pair and
 * the pairs need no locking. The server thread joins before returning, so no other tick logic races the
 * density arrays.
 *
 * <p>Radiation is stored per chunk, in region files of its own beside the dimension's {@code region},
 * {@code entities} and {@code poi} ({@link ChunkSidecar}, carrying a {@link RadiationPayload}). It is
 * read when a chunk arrives and written when it leaves or the level saves, so a save costs what the
 * radiation costs rather than what the world does.
 *
 * <p><b>Deviations from the fork.</b> The payload is a sidecar rather than extra fields on the chunk's
 * own NBT — the same per-chunk granularity without a mixin on three different chunk serializers — and
 * there is no {@code sun.misc.Unsafe} and no SIMD: the sweep is plain Java over {@code double[]}. The
 * observable gameplay — spread through air, sealing by resistant blocks, the exact decay and diffusivity
 * constants — matches.
 */
public final class RadiationSystemNT {

    private RadiationSystemNT() {}

    static final double RAD_EPSILON = 1.0e-5D;
    static final double RAD_MAX = Double.MAX_VALUE / 2.0D;
    static final short WALL = -1;

    /** The fork's {@code sanitize}: NaN → 0, |v| below epsilon → 0, clamp to [0, RAD_MAX]. */
    static double sanitize(double v) {
        if (Double.isNaN(v) || Math.abs(v) < RAD_EPSILON) return 0.0D;
        return Math.max(Math.min(v, RAD_MAX), 0.0D);
    }

    private static final Map<ServerLevel, WorldRadiationData> worldMap = new ConcurrentHashMap<>(4);

    /** The sidecar's folder, one per dimension, alongside vanilla's own chunk stores. */
    private static final String SIDECAR_NAME = "hbm_radiation";

    /** The fingerprint of a chunk the sidecar holds nothing for. */
    static final long NOTHING_STORED = 0L;

    /**
     * The diffusion pool, mirroring the fork's {@code RAD_POOL = ForkJoinPool.commonPool()}. Each tick the
     * server thread launches the parallel diffusion sweep here and <b>joins</b> — so the server thread is
     * quiescent during the parallel phase and no other tick logic races the density arrays. Section
     * building (which reads block state) is done on the server thread <i>before</i> the sweep, so the
     * off-thread phase only touches plain numeric arrays (safer than the fork, which flood-fills off-thread).
     */
    static final java.util.concurrent.ForkJoinPool RAD_POOL = java.util.concurrent.ForkJoinPool.commonPool();

    // ---- tuning (derived from RadiationConfig, mirroring the fork's updateConfigValues) ----
    static double dT() { return RadiationConfig.radTickRate / 20.0D; }
    /** {@code exp(ln0.5 · dT/halfLife)} — the fork's retention factor per simulation step. */
    static double retentionDt() { return Math.pow(0.5D, dT() / RadiationConfig.radHalfLifeSeconds); }
    static double diffusionDt() { return RadiationConfig.radDiffusivity * dT(); }

    static long ticks;
    /** Increments once per executed simulation step — selects the sweep permutation + pass flips (fork's workEpoch). */
    static long workEpoch;

    // ================= public API (called via ChunkRadiationManager.proxy) =================

    public static double getRadForCoord(ServerLevel level, BlockPos pos) {
        WorldRadiationData data = worldMap.get(level);
        if (data == null) return 0D;
        return data.getDensity(pos);
    }

    public static void setRadForCoord(ServerLevel level, BlockPos pos, double amount) {
        get(level).setDensity(pos, Math.max(0D, amount));
    }

    public static void incrementRad(ServerLevel level, BlockPos pos, double amount, double max) {
        get(level).incrementDensity(pos, amount, max);
    }

    public static void decrementRad(ServerLevel level, BlockPos pos, double amount) {
        get(level).incrementDensity(pos, -amount, Double.MAX_VALUE);
    }

    /** A rad-resistant block was placed/removed: rebuild that section's pockets on next access. */
    public static void markSectionForRebuild(ServerLevel level, BlockPos pos) {
        WorldRadiationData data = worldMap.get(level);
        if (data != null) data.markDirty(pos);
    }

    // ================= lifecycle (wired from the loader entrypoints) =================

    /**
     * Ticks every loaded level's radiation once per server tick (subject to {@code radTickRate}). Faithful
     * to the fork's structure: build sections on the server thread, then launch the parallel diffusion +
     * decay on {@link #RAD_POOL} and <b>join</b> before returning (the server thread blocks for the whole
     * parallel phase).
     */
    public static void tickServer(MinecraftServer server) {
        if (!com.hbm.config.GeneralConfig.enableRads) return;
        ticks++;

        // World block decay: drain ONE queued high-radiation pocket per level per tick — like the fork,
        // this runs every world tick, NOT behind the radTickRate gate (the queue is fed by the decay pass).
        if (RadiationConfig.worldRadEffects) {
            for (ServerLevel level : server.getAllLevels()) {
                WorldRadiationData data = worldMap.get(level);
                if (data != null) RadiationWorldHandler.handleWorldDestruction(data);
            }
        }

        // The fork's gate: (ticks + 17) % tickDelay == 0.
        if ((ticks + 17) % Math.max(1, RadiationConfig.radTickRate) != 0) return;
        workEpoch++;

        java.util.List<WorldRadiationData> worlds = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            WorldRadiationData data = worldMap.get(level);
            if (data != null) worlds.add(data);
        }
        if (worlds.isEmpty()) return;

        // 1) server thread: build sections (reads block state) for active + frontier.
        for (WorldRadiationData data : worlds) data.prepare();

        // 2) parallel phase (joined): the density-exchange sweeps + decay — pure array math, no MC access.
        for (WorldRadiationData data : worlds) {
            if (!data.runParallelSimulation(workEpoch)) {
                // The failed step was rolled back. Leave the world live and retry on the next gate;
                // reporting failure here is important because a joined worker failure is not success.
                com.hbm.Constants.LOG.error("[HBM] radiation step {} failed; retrying", workEpoch);
            }
        }
    }

    /** Submits a parallel task to {@link #RAD_POOL} and blocks the (server) thread until it completes. */
    static void submitJoin(Runnable task) {
        try {
            RAD_POOL.submit(task).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("radiation worker interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error error) throw error;
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new RuntimeException("radiation worker failed", cause);
        }
    }

    public static void onChunkLoaded(ServerLevel level, LevelChunk chunk) {
        get(level).onChunkLoaded(chunk.getPos());
    }

    public static void onChunkUnloaded(ServerLevel level, LevelChunk chunk) {
        WorldRadiationData data = worldMap.get(level);
        if (data != null) data.onChunkUnloaded(chunk.getPos());
    }

    /**
     * Writes every loaded chunk whose radiation changed, before the level serialises (mixin on save).
     * {@code flush} waits for the disk, exactly as vanilla's own chunk save does with the same flag.
     */
    public static void flushForSave(ServerLevel level, boolean flush) {
        WorldRadiationData data = worldMap.get(level);
        if (data != null) data.flushForSave(flush);
    }

    /**
     * The level is going away: persist what is loaded, then release the region files. What is on disk
     * stays there — this ends a session, not the world's radiation.
     */
    public static void closeWorld(ServerLevel level) {
        WorldRadiationData data = worldMap.remove(level);
        if (data != null) data.close();
    }

    /**
     * Erases the level's radiation, live and stored. Split from {@link #closeWorld} because the two were
     * one method: while the store was a blob that only held unloaded chunks, dropping the live map was
     * indistinguishable from erasing radiation. It is not, once a chunk can read its own back.
     */
    public static void clearWorld(ServerLevel level) {
        WorldRadiationData data = worldMap.get(level);
        if (data != null) data.clearRadiation();
    }

    private static WorldRadiationData get(ServerLevel level) {
        return worldMap.computeIfAbsent(level, WorldRadiationData::new);
    }

    // ============================================================================
    // Per-level state
    // ============================================================================
    static final class WorldRadiationData {

        final ServerLevel level;
        final int minSectionY;
        final ChunkSidecar sidecar;
        /** Live per-chunk data, loaded chunks only. */
        final Long2ObjectMap<ChunkRad> chunks = new Long2ObjectOpenHashMap<>();
        /**
         * Sidecar reads started when a chunk arrived and not yet taken. Deferring the take to first use
         * gives the read a tick of other work to hide behind instead of stalling the chunk-load callback.
         */
        private final Long2ObjectMap<CompletableFuture<byte[]>> pendingReads = new Long2ObjectOpenHashMap<>();

        /**
         * High-radiation pockets queued for world block decay ({@code {cx, sy, cz, pocketIndex}}).
         * Offered from RAD_POOL workers during the joined decay pass (pure numeric phase, mirroring the
         * fork's destructionQueue), drained one per tick on the server thread by
         * {@link RadiationWorldHandler#handleWorldDestruction}.
         */
        final java.util.concurrent.ConcurrentLinkedQueue<int[]> destructionQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
        final java.util.concurrent.atomic.AtomicInteger destructionQueueSize = new java.util.concurrent.atomic.AtomicInteger();
        private static final int DESTRUCTION_QUEUE_CAP = 512;

        WorldRadiationData(ServerLevel level) {
            this.level = level;
            this.minSectionY = level.getMinSectionY();
            this.sidecar = ChunkSidecar.open(level, SIDECAR_NAME);
            migrateSavedData();
        }

        /**
         * Moves a pre-sidecar world's parked radiation into the per-chunk store, once. The blob only ever
         * held unloaded chunks, so every row in it is a chunk the sidecar does not have.
         */
        private void migrateSavedData() {
            RadiationSavedData.get(level).drainTo((cx, cz, densities) -> {
                byte[] payload = RadiationPayload.encode(densities);
                if (payload != null) sidecar.write(new ChunkPos(cx, cz), payload);
            });
        }

        void queueDestruction(int cx, int sy, int cz, int pocketIndex) {
            if (destructionQueueSize.get() >= DESTRUCTION_QUEUE_CAP) return; // soft cap; drops are harmless
            destructionQueueSize.incrementAndGet();
            destructionQueue.offer(new int[] {cx, sy, cz, pocketIndex});
        }

        int[] pollDestruction() {
            int[] key = destructionQueue.poll();
            if (key != null) destructionQueueSize.decrementAndGet();
            return key;
        }

        // ---- coordinate helpers ----
        private ChunkRad chunkAt(int cx, int cz, boolean create) {
            long key = ChunkPos.pack(cx, cz);
            ChunkRad cr = chunks.get(key);
            if (cr == null && create) {
                cr = new ChunkRad(this, cx, cz);
                chunks.put(key, cr);
            }
            if (cr != null && !pendingReads.isEmpty()) settle(key, cr);
            return cr;
        }

        /**
         * Takes the read started when this chunk arrived, if it has one outstanding.
         *
         * <p>This is the only place that waits for the disk, and it is reached only when something asks
         * this chunk about its radiation — which is the moment the answer is actually needed. An
         * outstanding read is therefore also a proof: every route to a chunk's radiation comes through
         * {@link #chunkAt}, so a chunk whose read is still outstanding is a chunk nothing has touched
         * since it arrived, and what the sidecar holds for it is still what it should hold.
         */
        private void settle(long key, ChunkRad cr) {
            CompletableFuture<byte[]> pending = pendingReads.remove(key);
            if (pending == null) return;
            byte[] payload = pending.join();
            if (payload == null) return;
            double[][] densities = RadiationPayload.decode(payload);
            if (densities == null) {
                com.hbm.Constants.LOG.warn("[HBM] discarding unreadable stored radiation for chunk [{}, {}]",
                        cr.cx, cr.cz);
                return;
            }
            cr.applyPending(densities);
            cr.storedFingerprint = RadiationPayload.fingerprint(payload);
        }

        /**
         * Writes a chunk's radiation, or clears its entry — and does neither where the sidecar already
         * holds exactly this. A save then costs what the radiation that <i>moved</i> costs, rather than
         * what all the radiation costs, and a chunk that has none is never written at all.
         */
        private void persist(ChunkPos pos, ChunkRad cr) {
            byte[] payload = RadiationPayload.encode(cr.snapshot());
            long fingerprint = payload == null ? NOTHING_STORED : RadiationPayload.fingerprint(payload);
            if (fingerprint == cr.storedFingerprint) return;
            sidecar.write(pos, payload);
            cr.storedFingerprint = fingerprint;
        }

        double getDensity(BlockPos pos) {
            ServerChunkCache cache = level.getChunkSource();
            if (cache.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return 0D;
            ChunkRad cr = chunkAt(pos.getX() >> 4, pos.getZ() >> 4, false);
            if (cr == null) return 0D;
            return cr.getDensity(pos);
        }

        void setDensity(BlockPos pos, double amount) {
            ChunkRad cr = loadedChunk(pos);
            if (cr != null) cr.setDensity(pos, amount);
        }

        void incrementDensity(BlockPos pos, double amount, double max) {
            ChunkRad cr = loadedChunk(pos);
            if (cr != null) cr.incrementDensity(pos, amount, max);
        }

        private ChunkRad loadedChunk(BlockPos pos) {
            ServerChunkCache cache = level.getChunkSource();
            if (cache.getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) return null;
            return chunkAt(pos.getX() >> 4, pos.getZ() >> 4, true);
        }

        void markDirty(BlockPos pos) {
            ChunkRad cr = chunkAt(pos.getX() >> 4, pos.getZ() >> 4, false);
            if (cr != null) cr.markDirty(pos.getY());
        }

        void onChunkLoaded(ChunkPos pos) {
            int cx = Compat.chunkX(pos), cz = Compat.chunkZ(pos);
            ChunkRad cr = chunkAt(cx, cz, true);
            if (cr.hasRadiation()) return;
            pendingReads.put(ChunkPos.pack(cx, cz), sidecar.read(pos));
        }

        void onChunkUnloaded(ChunkPos pos) {
            long key = ChunkPos.pack(Compat.chunkX(pos), Compat.chunkZ(pos));
            ChunkRad cr = chunks.remove(key);
            if (pendingReads.remove(key) != null) return;
            if (cr != null) persist(pos, cr);
        }

        void flushForSave(boolean flush) {
            for (Long2ObjectMap.Entry<ChunkRad> e : chunks.long2ObjectEntrySet()) {
                if (pendingReads.containsKey(e.getLongKey())) continue;
                ChunkRad cr = e.getValue();
                persist(new ChunkPos(cr.cx, cr.cz), cr);
            }
            if (flush) sidecar.flush();
        }

        void close() {
            flushForSave(false);
            sidecar.close();
        }

        /** Drops the live radiation, and the stored radiation of every chunk this level has loaded. */
        void clearRadiation() {
            for (ChunkRad cr : chunks.values()) {
                if (cr.storedFingerprint != NOTHING_STORED) sidecar.write(new ChunkPos(cr.cx, cr.cz), null);
            }
            chunks.clear();
            pendingReads.clear();
            destructionQueue.clear();
            destructionQueueSize.set(0);
        }

        LevelChunkSection section(int cx, int sy, int cz) {
            ServerChunkCache cache = level.getChunkSource();
            LevelChunk chunk = cache.getChunkNow(cx, cz);
            if (chunk == null) return null;
            int idx = sy - minSectionY;
            if (idx < 0 || idx >= chunk.getSections().length) return null;
            return chunk.getSection(idx);
        }

        /** Snapshot of the live chunks taken on the server thread; the parallel sweep iterates this (never
         * the live map, which stays fixed during the join since chunk load/unload runs on the server thread). */
        private transient List<ChunkRad> snapshot = List.of();

        // ---- server-thread pre-pass: build the sections the sweep will touch (reads block state) ----
        void prepare() {
            snapshot = new ArrayList<>(chunks.values());
            for (ChunkRad cr : snapshot) {
                for (int i = 0; i < cr.sections.length; i++) {
                    SectionRad sr = cr.sections[i];
                    if (sr == null || sr.maxDensity <= RAD_EPSILON) continue;
                    // build this section + its 6 neighbours (in loaded chunks) so the parallel sweep can
                    // push radiation into them without any block-state access off-thread.
                    ensureBuilt(cr.cx, cr.cz, i);
                    ensureBuilt(cr.cx, cr.cz, i + 1);
                    ensureBuilt(cr.cx, cr.cz, i - 1);
                    ensureBuilt(cr.cx + 1, cr.cz, i);
                    ensureBuilt(cr.cx - 1, cr.cz, i);
                    ensureBuilt(cr.cx, cr.cz + 1, i);
                    ensureBuilt(cr.cx, cr.cz - 1, i);
                }
            }
            // re-snapshot in case building created new frontier chunks
            snapshot = new ArrayList<>(chunks.values());
        }

        private void ensureBuilt(int cx, int cz, int sectionIndex) {
            if (sectionIndex < 0 || sectionIndex >= level.getSectionsCount()) return;
            if (level.getChunkSource().getChunkNow(cx, cz) == null) return;
            ChunkRad cr = chunkAt(cx, cz, true);
            SectionRad sr = cr.section(sectionIndex, true);
            if (sr != null) sr.ensureBuilt();
        }

        // ---- parallel phase: parity-bucketed exchange sweeps + decay, launched on RAD_POOL and joined ----
        /**
         * One simulation step, mirroring the fork's {@code runExactExchangeSweeps} exactly: the three axis
         * sweeps run in one of <b>6 permutations</b> selected by {@code workEpoch % 6}, with the pass-order
         * flips {@code fx = epoch&1}, {@code fz = epoch&2} (which parity bucket sweeps first per horizontal
         * axis) and {@code yPar = epoch&4} (the Y sweep's starting parity) — then the decay pass
         * ({@code postSweepDecayAndEffects}).
         */
        boolean runParallelSimulation(long epoch) {
            List<ChunkRad> all = snapshot;
            if (all.isEmpty()) return true;

            // A worker can fail after earlier disjoint passes have already changed densities. Keep the
            // whole simulation step transactional so the next gate retries the old state, not a partial
            // step with a misleadingly successful result.
            List<double[][]> before = new ArrayList<>(all.size());
            for (ChunkRad cr : all) {
                double[][] sections = new double[cr.sections.length][];
                for (int i = 0; i < cr.sections.length; i++) {
                    SectionRad sr = cr.sections[i];
                    if (sr != null && sr.density != null) sections[i] = sr.density.clone();
                }
                before.add(sections);
            }
            List<int[]> queuedBefore = new ArrayList<>(destructionQueue);

            try {
                int s = (int) epoch;
                boolean fx = (s & 1) != 0, fz = (s & 2) != 0;
                int yPar = (s & 4) != 0 ? 1 : 0;
                int perm = s % 6;
                if (perm < 0) perm += 6;

                switch (perm) {
                    case 0 -> { sweepAxis(all, Axis.X, fx); sweepAxis(all, Axis.Z, fz); sweepY(all, yPar); }
                    case 1 -> { sweepAxis(all, Axis.X, fx); sweepY(all, yPar); sweepAxis(all, Axis.Z, fz); }
                    case 2 -> { sweepY(all, yPar); sweepAxis(all, Axis.Z, fz); sweepAxis(all, Axis.X, fx); }
                    case 3 -> { sweepY(all, yPar); sweepAxis(all, Axis.X, fx); sweepAxis(all, Axis.Z, fz); }
                    case 4 -> { sweepAxis(all, Axis.Z, fz); sweepAxis(all, Axis.X, fx); sweepY(all, yPar); }
                    default -> { sweepAxis(all, Axis.Z, fz); sweepY(all, yPar); sweepAxis(all, Axis.X, fx); }
                }

                // decay (per-chunk disjoint) — postSweepDecayAndEffects.
                double retention = retentionDt();
                submitJoin(() -> all.parallelStream().forEach(cr -> cr.decay(retention)));
                return true;
            } catch (RuntimeException | Error failure) {
                for (int c = 0; c < all.size(); c++) {
                    ChunkRad cr = all.get(c);
                    double[][] sections = before.get(c);
                    for (int i = 0; i < cr.sections.length; i++) {
                        SectionRad sr = cr.sections[i];
                        if (sr != null && sections[i] != null) {
                            sr.density = sections[i];
                            sr.recomputeMax();
                        }
                    }
                }
                destructionQueue.clear();
                destructionQueue.addAll(queuedBefore);
                destructionQueueSize.set(queuedBefore.size());
                com.hbm.Constants.LOG.error("[HBM] radiation worker failed; rolled back simulation step", failure);
                return false;
            }
        }

        /**
         * One horizontal axis, two parity passes ({@code flip} reverses which parity goes first — the fork's
         * {@code fx}/{@code fz}). In a pass, only chunks whose {@code cx}/{@code cz} has the given parity
         * exchange with their +axis neighbour, so every chunk is written by at most one pair → the pairs are
         * disjoint and run in parallel with no locking (the fork's parity-bucketing).
         */
        private void sweepAxis(List<ChunkRad> all, Axis axis, boolean flip) {
            for (int pass = 0; pass < 2; pass++) {
                final int p = (flip ? 1 - pass : pass);
                submitJoin(() -> all.parallelStream().forEach(a -> {
                    int coord = (axis == Axis.X) ? a.cx : a.cz;
                    if ((coord & 1) != p) return;
                    ChunkRad b = (axis == Axis.X)
                            ? chunks.get(ChunkPos.pack(a.cx + 1, a.cz))
                            : chunks.get(ChunkPos.pack(a.cx, a.cz + 1));
                    if (b == null) return;
                    for (int i = 0; i < a.sections.length; i++) {
                        exchangeFace(a.sections[i], b.sections[i], axis);
                    }
                }));
            }
        }

        /** Y sweep: internal to each chunk → whole chunks are disjoint, run them all in parallel.
         * {@code startParity} = the fork's {@code yPar} (which section-index parity exchanges first). */
        private void sweepY(List<ChunkRad> all, int startParity) {
            submitJoin(() -> all.parallelStream().forEach(cr -> cr.exchangeY(startParity)));
        }
    }

    // ============================================================================
    // Per-chunk data: one SectionRad per section that radiation has reached
    // ============================================================================
    static final class ChunkRad {

        final WorldRadiationData world;
        final int cx, cz;
        final SectionRad[] sections;
        /** {@link RadiationPayload#fingerprint} of what the sidecar holds for this chunk, if anything. */
        long storedFingerprint = NOTHING_STORED;

        ChunkRad(WorldRadiationData world, int cx, int cz) {
            this.world = world;
            this.cx = cx;
            this.cz = cz;
            this.sections = new SectionRad[world.level.getSectionsCount()];
        }

        private int index(int worldY) {
            return (worldY >> 4) - world.minSectionY;
        }

        private SectionRad section(int sectionIndex, boolean create) {
            if (sectionIndex < 0 || sectionIndex >= sections.length) return null;
            SectionRad sr = sections[sectionIndex];
            if (sr == null && create) {
                int sy = sectionIndex + world.minSectionY;
                sr = new SectionRad(world, cx, sy, cz);
                sections[sectionIndex] = sr;
            }
            return sr;
        }

        double getDensity(BlockPos pos) {
            SectionRad sr = section(index(pos.getY()), false);
            if (sr == null) return 0D;
            return sr.densityAt(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        }

        void setDensity(BlockPos pos, double amount) {
            SectionRad sr = section(index(pos.getY()), true);
            if (sr != null) sr.setAt(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, amount);
        }

        void incrementDensity(BlockPos pos, double amount, double max) {
            SectionRad sr = section(index(pos.getY()), true);
            if (sr != null) sr.incrementAt(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, amount, max);
        }

        void markDirty(int worldY) {
            int i = index(worldY);
            if (i >= 0 && i < sections.length && sections[i] != null) sections[i].dirty = true;
        }

        boolean hasRadiation() {
            for (SectionRad sr : sections) if (sr != null && sr.maxDensity > RAD_EPSILON) return true;
            return false;
        }

        /** Y-axis exchange, internal to this chunk (section i ↔ i+1). Two parity passes over the section
         * index so no section is written twice in one pass — the vertical analogue of the horizontal
         * parity-bucketing; {@code startParity} = the fork's alternating {@code yPar}. Runs on a pool
         * worker; only this chunk's own sections are touched. */
        void exchangeY(int startParity) {
            for (int pass = 0; pass < 2; pass++) {
                int parity = startParity ^ pass;
                for (int i = parity; i + 1 < sections.length; i += 2) {
                    exchangeFace(sections[i], sections[i + 1], Axis.Y);
                }
            }
        }

        void decay(double retention) {
            for (SectionRad sr : sections) if (sr != null) sr.decay(retention);
        }

        /** Snapshot per-section pocket densities for persistence, or null if empty. */
        double[][] snapshot() {
            double[][] out = null;
            for (int i = 0; i < sections.length; i++) {
                SectionRad sr = sections[i];
                if (sr == null || sr.maxDensity <= RAD_EPSILON) continue;
                if (out == null) out = new double[sections.length][];
                out[i] = sr.density.clone();
            }
            return out;
        }

        void applyPending(double[][] pending) {
            for (int i = 0; i < pending.length && i < sections.length; i++) {
                if (pending[i] == null) continue;
                SectionRad sr = section(i, true);
                sr.ensureBuilt();
                double[] d = pending[i];
                for (int p = 0; p < d.length && p < sr.density.length; p++) sr.density[p] = d[p];
                sr.recomputeMax();
            }
        }
    }

    private enum Axis { X, Y, Z }

    /**
     * Exchanges radiation across the positive-{@code axis} face between two sections' pockets — the exact
     * 2-node analytical solution from the old {@code SectionRef.exchangeWith*} family:
     * <pre>
     *   e     = exp(−(area / distSum) · (1/vA + 1/vB) · diffusionDt)
     *   rStar = (ra·(1/vB) + rb·(1/vA)) / (1/vA + 1/vB)      // mass-weighted equilibrium
     *   ra'   = rStar + (ra − rStar)·e ;  rb' likewise
     * </pre>
     * where {@code area} = open block-face pairs between the two pockets on the shared face, and
     * {@code distSum} = both pockets' centre-of-mass distance to that face (uniform full section = 8.0,
     * reproducing the old uniform-uniform {@code UU_E = exp(−diffusionDt/128)} exactly). Pocket pairs are
     * processed sequentially in face-scan discovery order (the old edge-list order), reading fresh
     * densities per pair like the old Gauss–Seidel loops. Rad-resistant walls contribute no area, so
     * sealed pockets never exchange.
     */
    private static void exchangeFace(SectionRad a, SectionRad b, Axis axis) {
        // Runs on a pool worker during the joined parallel phase — must NOT touch block state. Sections
        // are pre-built on the server thread in WorldRadiationData.prepare(); skip any that isn't
        // (old: KIND_NONE → no exchange).
        if (a == null || b == null || !a.built || !b.built) return;
        if (a.maxDensity <= RAD_EPSILON && b.maxDensity <= RAD_EPSILON) return;
        if (a.pocketCount == 0 || b.pocketCount == 0) return;

        // Old SectionRef face ordinals for the two sides of the shared +axis face.
        final int faceA, faceB;
        switch (axis) {
            case X -> { faceA = 5; faceB = 4; }   // A's EAST face ↔ B's WEST face
            case Y -> { faceA = 1; faceB = 0; }   // A's UP ↔ B's DOWN
            default -> { faceA = 3; faceB = 2; }  // A's SOUTH ↔ B's NORTH
        }

        // Collect (pa, pb) pairs with their contact areas, in first-discovery face-scan order.
        int[] pairKeys = new int[64];
        int[] pairAreas = new int[64];
        int pairCount = 0;
        for (int u = 0; u < 16; u++) {
            for (int v = 0; v < 16; v++) {
                int ia, ib;
                switch (axis) {
                    case X -> { ia = idx(15, u, v); ib = idx(0, u, v); }
                    case Y -> { ia = idx(u, 15, v); ib = idx(u, 0, v); }
                    default -> { ia = idx(u, v, 15); ib = idx(u, v, 0); }
                }
                short pa = a.pocketOf[ia], pb = b.pocketOf[ib];
                if (pa == WALL || pb == WALL) continue;
                int key = (pa << 11) | pb;
                int found = -1;
                for (int i = 0; i < pairCount; i++) {
                    if (pairKeys[i] == key) { found = i; break; }
                }
                if (found >= 0) {
                    pairAreas[found]++;
                } else {
                    if (pairCount == pairKeys.length) {
                        pairKeys = java.util.Arrays.copyOf(pairKeys, pairCount * 2);
                        pairAreas = java.util.Arrays.copyOf(pairAreas, pairCount * 2);
                    }
                    pairKeys[pairCount] = key;
                    pairAreas[pairCount] = 1;
                    pairCount++;
                }
            }
        }
        if (pairCount == 0) return;

        double diffusionDt = diffusionDt();
        boolean changed = false;
        for (int i = 0; i < pairCount; i++) {
            int pa = pairKeys[i] >>> 11, pb = pairKeys[i] & 0x7FF;
            double ra = a.density[pa], rb = b.density[pb];
            if (ra == rb) continue;

            double invVa = 1.0D / a.volume[pa];
            double invVb = 1.0D / b.volume[pb];
            double denomInv = invVa + invVb;
            double distSum = a.faceDist[pa * 6 + faceA] + b.faceDist[pb * 6 + faceB];
            if (distSum <= 0.0D) continue;

            double e = Math.exp(-((pairAreas[i] / distSum) * denomInv * diffusionDt));
            double rStar = (ra * invVb + rb * invVa) / denomInv;

            a.density[pa] = rStar + (ra - rStar) * e;
            b.density[pb] = rStar + (rb - rStar) * e;
            changed = true;
        }
        if (changed) {
            a.recomputeMax();
            b.recomputeMax();
        }
    }

    static int idx(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    // ============================================================================
    // Per-section pocket data
    // ============================================================================
    static final class SectionRad {

        final WorldRadiationData world;
        final int cx, sy, cz;

        boolean built;
        boolean dirty;
        short[] pocketOf;   // 4096, pocket index per block, WALL for rad-resistant
        int pocketCount;
        double[] density;   // per pocket
        int[] volume;       // per pocket
        float[] faceDist;   // per pocket × 6 faces: centre-of-mass distance to the face (old SectionRef)
        double maxDensity;

        SectionRad(WorldRadiationData world, int cx, int sy, int cz) {
            this.world = world;
            this.cx = cx;
            this.sy = sy;
            this.cz = cz;
        }

        void ensureBuilt() {
            if (built && !dirty) return;
            double[] oldDensity = built ? density : null;
            short[] oldPocketOf = built ? pocketOf : null;
            build();
            // Preserve radiation across a wall-change rebuild by carrying each old pocket's density to the
            // new pocket its first block maps to (best-effort; walls rarely change under active radiation).
            if (oldDensity != null && oldPocketOf != null) {
                double[] carried = new double[pocketCount];
                boolean[] set = new boolean[pocketCount];
                for (int i = 0; i < SECTION_BLOCKS; i++) {
                    short np = pocketOf[i];
                    short op = oldPocketOf[i];
                    if (np == WALL || op == WALL) continue;
                    if (!set[np]) { carried[np] = oldDensity[op]; set[np] = true; }
                }
                density = carried;
                recomputeMax();
            }
            dirty = false;
        }

        private void build() {
            LevelChunkSection sec = world.section(cx, sy, cz);
            pocketOf = new short[SECTION_BLOCKS];
            if (sec == null || sec.hasOnlyAir()) {
                // one pocket covering the whole section (fast path for sky/air) — centre of mass is the
                // exact section centre (7.5), so every face distance is 8.0 (the old code's uniform 8.0d).
                java.util.Arrays.fill(pocketOf, (short) 0);
                pocketCount = 1;
                density = density != null && density.length == 1 ? density : new double[] {0D};
                volume = new int[] {SECTION_BLOCKS};
                faceDist = new float[] {8f, 8f, 8f, 8f, 8f, 8f};
                built = true;
                return;
            }
            // mark walls
            boolean[] wall = new boolean[SECTION_BLOCKS];
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                BlockState state = sec.getBlockState(x, y, z);
                if (state.getBlock() instanceof IRadResistantBlock r
                        && r.isRadResistant(world.level, new BlockPos((cx << 4) + x, (sy << 4) + y, (cz << 4) + z))) {
                    wall[idx(x, y, z)] = true;
                }
            }
            // flood fill non-wall blocks into pockets, accumulating each pocket's centre-of-mass sums
            java.util.Arrays.fill(pocketOf, WALL);
            int[] stack = new int[SECTION_BLOCKS];
            List<Integer> vols = new ArrayList<>();
            List<long[]> sums = new ArrayList<>(); // per pocket: {sumX, sumY, sumZ}
            short pocket = 0;
            for (int start = 0; start < SECTION_BLOCKS; start++) {
                if (wall[start] || pocketOf[start] != WALL) continue;
                int sp = 0;
                stack[sp++] = start;
                pocketOf[start] = pocket;
                int vol = 0;
                long sx = 0, sy2 = 0, sz = 0;
                while (sp > 0) {
                    int cur = stack[--sp];
                    vol++;
                    int x = cur & 15, y = (cur >> 8) & 15, z = (cur >> 4) & 15;
                    sx += x; sy2 += y; sz += z;
                    if (x > 0) sp = push(stack, sp, idx(x - 1, y, z), wall, pocket);
                    if (x < 15) sp = push(stack, sp, idx(x + 1, y, z), wall, pocket);
                    if (z > 0) sp = push(stack, sp, idx(x, y, z - 1), wall, pocket);
                    if (z < 15) sp = push(stack, sp, idx(x, y, z + 1), wall, pocket);
                    if (y > 0) sp = push(stack, sp, idx(x, y - 1, z), wall, pocket);
                    if (y < 15) sp = push(stack, sp, idx(x, y + 1, z), wall, pocket);
                }
                vols.add(vol);
                sums.add(new long[] {sx, sy2, sz});
                pocket++;
            }
            pocketCount = pocket;
            volume = new int[pocketCount];
            faceDist = new float[pocketCount * 6];
            for (int i = 0; i < pocketCount; i++) {
                int vol = vols.get(i);
                volume[i] = vol;
                long[] s = sums.get(i);
                // Per-pocket centre-of-mass distance to each face — the old SectionRef face ordinals:
                // 0=DOWN, 1=UP, 2=NORTH(-z), 3=SOUTH(+z), 4=WEST(-x), 5=EAST(+x); ±0.5 as in getFaceDist.
                double comX = (double) s[0] / vol, comY = (double) s[1] / vol, comZ = (double) s[2] / vol;
                faceDist[i * 6] = (float) (comY + 0.5D);
                faceDist[i * 6 + 1] = (float) (15.5D - comY);
                faceDist[i * 6 + 2] = (float) (comZ + 0.5D);
                faceDist[i * 6 + 3] = (float) (15.5D - comZ);
                faceDist[i * 6 + 4] = (float) (comX + 0.5D);
                faceDist[i * 6 + 5] = (float) (15.5D - comX);
            }
            density = new double[pocketCount];
            built = true;
        }

        private int push(int[] stack, int sp, int i, boolean[] wall, short pocket) {
            if (!wall[i] && pocketOf[i] == WALL) {
                pocketOf[i] = pocket;
                stack[sp++] = i;
            }
            return sp;
        }

        double densityAt(int x, int y, int z) {
            ensureBuilt();
            short p = pocketOf[idx(x, y, z)];
            return p == WALL ? 0D : density[p];
        }

        void setAt(int x, int y, int z, double amount) {
            ensureBuilt();
            short p = pocketOf[idx(x, y, z)];
            if (p == WALL) return;
            double d = sanitize(amount);
            density[p] = d;
            if (d > maxDensity) maxDensity = d;
        }

        void incrementAt(int x, int y, int z, double amount, double max) {
            if (Math.abs(amount) < RAD_EPSILON) return; // fork: increments below epsilon are dropped
            ensureBuilt();
            short p = pocketOf[idx(x, y, z)];
            if (p == WALL) return;
            double current = density[p];
            if (amount > 0 && current >= max) return;   // fork: at/above max → no-op
            double next = current + amount;
            if (next > max) next = max;
            next = sanitize(next);
            density[p] = next;
            if (next > maxDensity) maxDensity = next;
        }

        void decay(double retention) {
            if (!built || maxDensity <= RAD_EPSILON) { maxDensity = 0; return; }
            double max = 0;
            for (int i = 0; i < density.length; i++) {
                double d = sanitize(density[i] * retention);
                density[i] = d;
                if (d > max) max = d;

                // World block decay roll — the fork's postSweep effects: pockets holding >= 5 RAD have a
                // 1-in-100 chance per executed decay step to be queued for surface destruction (the
                // hardcoded fork threshold; RadiationConfig.worldRadThreshold is unused in the NT path
                // there too). Runs on a pool worker — only queues a numeric key, no MC access.
                // TODO(rad-fog): the fork also rolled fogRad/fogCh ambient rad-fog spawns here — needs
                // the HBM particle system.
                if (d >= 5.0D && RadiationConfig.worldRadEffects
                        && java.util.concurrent.ThreadLocalRandom.current().nextInt(100) == 0) {
                    world.queueDestruction(cx, sy, cz, i);
                }
            }
            maxDensity = max;
        }

        void recomputeMax() {
            double max = 0;
            if (density != null) for (double d : density) if (d > max) max = d;
            maxDensity = max;
        }

        static final int SECTION_BLOCKS = 4096;
    }
}
