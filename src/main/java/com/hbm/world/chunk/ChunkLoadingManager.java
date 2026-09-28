package com.hbm.world.chunk;

import com.hbm.Constants;
import dev.golem.core.platform.Lifecycle;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * HBM's forced-chunk bookkeeping: reference-counted chunk claims on top of vanilla's
 * {@code ServerLevel.setChunkForced}, plus the restart reconciliation that Forge's ticket system used
 * to provide.
 *
 * <p><b>Why vanilla and not a loader API.</b> {@code setChunkForced} exists unchanged on every node
 * from 1.20.1 to 26.1 and gives a fully ticking chunk, which is rung 1 of the mixin ladder — NeoForge's
 * {@code TicketController} and Fabric's absence of one would have meant two implementations behind a
 * facade for no behavioural gain. What vanilla does <em>not</em> give is ownership: the forced set is a
 * flat boolean set, so two holders over one chunk would clobber each other and a crash would pin
 * chunks forever. Both gaps are closed here — refcounts for the first, {@link ForcedChunkData} plus
 * the grace window below for the second.
 *
 * <p><b>The grace window.</b> On the first server tick after a level comes up, every chunk HBM had
 * recorded is re-forced under an internal ticket which is released {@value #RESTORE_GRACE_TICKS} ticks
 * later. That is the analogue of Forge handing a persisted ticket back through {@code LoadingCallback}:
 * the chunk stays up long enough for the entity inside it to tick and claim it for itself, and anything
 * unclaimed at the end of the window (its owner died, or the server crashed mid-flight) is dropped
 * instead of leaking.
 *
 * <p>Server-thread only, like every chunk operation.
 */
public final class ChunkLoadingManager {

    /** How long a restored claim survives without an owner. 10 s at 20 tps. */
    public static final int RESTORE_GRACE_TICKS = 200;

    private ChunkLoadingManager() {}

    private static final Map<ResourceKey<Level>, LevelState> STATES = new HashMap<>();

    private static final class LevelState {
        final Long2IntOpenHashMap refCounts = new Long2IntOpenHashMap();
        /**
         * Chunks that were already in the level's forced set when HBM first claimed them. The set is one flag
         * per chunk, not a refcount (1.7's ForgeChunkManager gave every mod its own tickets instead), so HBM
         * leaves the flag alone for these when its last claim goes: clearing it would unload a chunk someone
         * else forced — a /forceload, or a GameTest plot whose test had not started yet and then never did.
         */
        final it.unimi.dsi.fastutil.longs.LongOpenHashSet borrowed = new it.unimi.dsi.fastutil.longs.LongOpenHashSet();
        final Set<ChunkLoaderTicket> tickets = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        ChunkLoaderTicket restoreTicket;
        long restoreExpiresAt = -1L;
        boolean restored;
    }

    /** Registers the lifecycle hooks. Called once from mod init, on every loader. */
    public static void init() {
        Lifecycle.onServerTick(server -> {
            for (ServerLevel level : server.getAllLevels()) tickLevel(level);
        });
        Lifecycle.onLevelUnload((server, level) -> forget(level));
        Lifecycle.onChunkLoad(PendingDetonations::onChunkLoaded);
    }

    // --- ticket API ---

    /**
     * Hands out a fresh ticket for {@code level}. The ticket holds nothing until it is told to
     * {@link ChunkLoaderTicket#force} something — upstream's contract note 3: request lazily, from the
     * first server-side update, never from entity construction or NBT reading.
     */
    public static ChunkLoaderTicket requestTicket(ServerLevel level, String owner) {
        ChunkLoaderTicket ticket = new ChunkLoaderTicket(level, owner);
        state(level).tickets.add(ticket);
        return ticket;
    }

    /** Whether HBM currently holds {@code (chunkX, chunkZ)} in {@code level}. */
    public static boolean isForced(ServerLevel level, int chunkX, int chunkZ) {
        LevelState state = STATES.get(level.dimension());
        return state != null && state.refCounts.get(ChunkPos.pack(chunkX, chunkZ)) > 0;
    }

    /** How many chunks HBM holds in {@code level}. Diagnostics and tests. */
    public static int forcedChunkCount(ServerLevel level) {
        LevelState state = STATES.get(level.dimension());
        return state == null ? 0 : state.refCounts.size();
    }

    // --- refcounting (package-private: driven by ChunkLoaderTicket) ---

    static void acquire(ServerLevel level, long packed) {
        LevelState state = state(level);
        int count = state.refCounts.addTo(packed, 1);
        if (count == 0) {
            // updateChunkForced, NOT setChunkForced: the latter is this call plus a blocking
            // getChunk(x, z), so claiming a wide area — a fallout disc is thousands of chunks — pays a
            // full chunk generation on the server thread per chunk before any work starts. A real Fat
            // Man measured 20.6 s of server thread on 1,057 such chunks (§19z-j). Claiming is a
            // registration; callers here all poll for the chunk anyway.
            int chunkX = ChunkPos.getX(packed), chunkZ = ChunkPos.getZ(packed);
            // A chunk still in HBM's own save record is HBM's from before a restart, not someone else's.
            boolean external = dev.golem.core.world.Compat.isChunkForced(level, chunkX, chunkZ)
                    && !ForcedChunkData.get(level).chunks().contains(packed);
            if (external) state.borrowed.add(packed);
            else dev.golem.core.world.Compat.updateChunkForced(level, chunkX, chunkZ, true);
            ForcedChunkData.get(level).add(packed);
        }
    }

    static void releaseChunk(ServerLevel level, long packed) {
        LevelState state = STATES.get(level.dimension());
        if (state == null) return;
        int remaining = state.refCounts.addTo(packed, -1) - 1;
        if (remaining <= 0) {
            state.refCounts.remove(packed);
            if (!state.borrowed.remove(packed)) {
                dev.golem.core.world.Compat.updateChunkForced(level, ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
            }
            ForcedChunkData.get(level).remove(packed);
        }
    }

    static void retire(ChunkLoaderTicket ticket) {
        LevelState state = STATES.get(ticket.level().dimension());
        if (state != null) state.tickets.remove(ticket);
    }

    // --- lifecycle ---

    private static void tickLevel(ServerLevel level) {
        LevelState state = state(level);

        if (!state.restored) {
            state.restored = true;
            restore(level, state);
        }

        if (state.restoreTicket != null && level.getGameTime() >= state.restoreExpiresAt) {
            int dropped = state.restoreTicket.chunks().size();
            state.restoreTicket.release();
            state.restoreTicket = null;
            if (dropped > 0) {
                Constants.LOG.debug("Dropped {} unclaimed forced chunk(s) in {} after the restore grace window",
                        dropped, level.dimension());
            }
        }
        PendingDetonations.tick(level);
    }

    /**
     * Re-forces everything the save says HBM had forced, under a ticket that expires. Chunks whose
     * owner is still alive get claimed by that owner within a tick or two and survive the expiry
     * because the refcount stays above zero.
     */
    private static void restore(ServerLevel level, LevelState state) {
        LongOpenHashSet saved = new LongOpenHashSet(ForcedChunkData.get(level).chunks());
        if (saved.isEmpty()) return;

        ChunkLoaderTicket ticket = requestTicket(level, "restore");
        for (LongIterator it = saved.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            ticket.force(ChunkPos.getX(packed), ChunkPos.getZ(packed));
        }
        state.restoreTicket = ticket;
        state.restoreExpiresAt = level.getGameTime() + RESTORE_GRACE_TICKS;
        Constants.LOG.info("Restored {} forced chunk(s) in {} for {} ticks", saved.size(),
                level.dimension(), RESTORE_GRACE_TICKS);
    }

    /**
     * Drops the in-memory state for an unloading level <em>without</em> unforcing anything or touching
     * the save record. The chunks are going away with the level anyway, and keeping the record is what
     * lets {@link #restore} bring the same set back next time the level loads.
     */
    private static void forget(ServerLevel level) {
        PendingDetonations.forget(level);
        STATES.remove(level.dimension());
    }

    private static LevelState state(ServerLevel level) {
        return STATES.computeIfAbsent(level.dimension(), key -> new LevelState());
    }
}
