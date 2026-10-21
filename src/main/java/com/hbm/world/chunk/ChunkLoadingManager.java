package com.hbm.world.chunk;

import com.hbm.Constants;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.Chunks;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * HBM's forced-chunk bookkeeping: reference-counted claims on a chunk-system ticket of HBM's own
 * ({@link Chunks#claimTicking}), plus the restart reconciliation that Forge's ticket system used to provide.
 *
 * <p><b>Why not the forced set.</b> {@code ServerLevel.setChunkForced} is a flat boolean set shared with
 * /forceload, GameTest plots and other mods, and its ticket is shared too: removing it removes the other
 * user's. HBM's tickets are separate, refcounts give ownership among HBM's holders, and
 * {@link ForcedChunkData} plus the grace window below close the crash gap.
 *
 * <p><b>The grace window.</b> On the first server tick after a level comes up, every chunk HBM had
 * recorded is re-forced under an internal ticket released {@value #RESTORE_GRACE_TICKS} ticks later, like
 * Forge handing a persisted ticket back through {@code LoadingCallback}. The chunk stays up long enough for
 * the entity inside it to tick and claim it, and anything unclaimed at the end (its owner died, or the server
 * crashed mid-flight) is dropped.
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
        final Set<ChunkLoaderTicket> tickets = Collections.newSetFromMap(new IdentityHashMap<>());
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
     * {@link ChunkLoaderTicket#force} something. Upstream's contract note 3: request lazily, from the
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
            // A claim is a ticket of HBM's own, not a registration of the level's forced set: a chunk that is
            // forced too (a /forceload, a GameTest plot) stays forced when this claim goes. The ticket is not a
            // setChunkForced either, which would generate the chunk on the server thread, per chunk of a fallout disc.
            Chunks.claimTicking(level, ChunkPos.getX(packed), ChunkPos.getZ(packed));
            ForcedChunkData.get(level).add(packed);
        }
    }

    static void releaseChunk(ServerLevel level, long packed) {
        LevelState state = STATES.get(level.dimension());
        if (state == null) return;
        int remaining = state.refCounts.addTo(packed, -1) - 1;
        if (remaining <= 0) {
            state.refCounts.remove(packed);
            Chunks.releaseTicking(level, ChunkPos.getX(packed), ChunkPos.getZ(packed));
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
