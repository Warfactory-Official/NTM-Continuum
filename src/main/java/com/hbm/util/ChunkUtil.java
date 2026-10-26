package com.hbm.util;

import com.hbm.world.chunk.ChunkLoaderTicket;
import com.hbm.world.chunk.ChunkLoadingManager;
import dev.golem.core.world.Chunks;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * The per-chunk anchor/claim surface the copied ntm-next entities were written against
 * ({@code EntityExplosionChunkloading}, planes, Soyuz, drones, TOM, the laser death blast).
 *
 * <p>ntm-next backed these with its own registered {@code TicketType}s and a refcount beside the vanilla
 * ticket storage. This port already owns forced chunks through {@link ChunkLoadingManager}: persisted,
 * reconciled on restart, reference-counted, so the same six calls are answered from one manager ticket
 * per level instead of a second ticket registry. The refcount semantics are ntm-next's: a chunk is held
 * while any anchor or claim on it is outstanding, and the last release drops it.
 */
public final class ChunkUtil {

    private ChunkUtil() {}

    private static final class LevelClaims {
        final Long2IntOpenHashMap anchors = new Long2IntOpenHashMap();
        final Long2IntOpenHashMap forced = new Long2IntOpenHashMap();
        ChunkLoaderTicket ticket;
    }

    private static final Map<ResourceKey<Level>, LevelClaims> CLAIMS = new HashMap<>();

    private static LevelClaims claims(ServerLevel level) {
        LevelClaims claims = CLAIMS.computeIfAbsent(level.dimension(), ignored -> new LevelClaims());
        if (claims.ticket == null || claims.ticket.isReleased() || claims.ticket.level() != level) {
            claims.ticket = ChunkLoadingManager.requestTicket(level, "hbm:chunk_util");
        }
        return claims;
    }

    private static void hold(LevelClaims claims, long chunkPos) {
        if (claims.anchors.get(chunkPos) + claims.forced.get(chunkPos) > 0) return;
        claims.ticket.force(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
    }

    private static void drop(LevelClaims claims, long chunkPos) {
        if (claims.anchors.get(chunkPos) + claims.forced.get(chunkPos) > 0) return;
        claims.ticket.unforce(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
    }

    /** Holds {@code chunkPos} loaded and ticking for one more owner. Server thread. */
    public static void addAnchor(ServerLevel level, long chunkPos) {
        LevelClaims claims = claims(level);
        hold(claims, chunkPos);
        claims.anchors.addTo(chunkPos, 1);
    }

    /** Drops one anchor on {@code chunkPos}; the chunk is unforced once no anchor or claim remains. */
    public static void releaseAnchor(ServerLevel level, long chunkPos) {
        LevelClaims claims = CLAIMS.get(level.dimension());
        if (claims == null) return;
        int remaining = claims.anchors.get(chunkPos);
        if (remaining <= 0) return;
        if (remaining > 1) {
            claims.anchors.put(chunkPos, remaining - 1);
            return;
        }
        claims.anchors.remove(chunkPos);
        drop(claims, chunkPos);
    }

    /** Waits for the caller's held chunk to tick entities and finish loading its stored entities. */
    public static void loadEntityTickingChunk(ServerLevel level, long chunkPos) {
        Chunks.loadEntityTicking(level, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
    }

    /** Force-loads {@code chunkPos} for one of our own holders; always ours, so always {@code true}. */
    public static boolean claimForced(ServerLevel level, long chunkPos) {
        LevelClaims claims = claims(level);
        hold(claims, chunkPos);
        claims.forced.addTo(chunkPos, 1);
        return true;
    }

    /** Drops one forced claim on {@code chunkPos}. */
    public static void releaseForced(ServerLevel level, long chunkPos) {
        LevelClaims claims = CLAIMS.get(level.dimension());
        if (claims == null) return;
        int remaining = claims.forced.get(chunkPos);
        if (remaining <= 0) return;
        if (remaining > 1) {
            claims.forced.put(chunkPos, remaining - 1);
            return;
        }
        claims.forced.remove(chunkPos);
        drop(claims, chunkPos);
    }

    /** Holds a blast's chunk loaded without simulating it; paired with {@link #releaseChunkAsync}. */
    public static void addBlastTicket(ServerLevel level, long chunkPos) {
        Chunks.claimForLoading(level, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
    }

    /** Releases one loading-only blast claim without waiting for a chunk to unload. */
    public static void releaseChunkAsync(ServerLevel level, long chunkPos) {
        Chunks.releaseForLoading(level, ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
    }

    /** Forgets every claim; the manager already released the tickets with the level. */
    public static void onServerStopped() {
        CLAIMS.clear();
    }
}
