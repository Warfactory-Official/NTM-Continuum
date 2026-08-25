package com.hbm.world.chunk;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * One holder's claim on a set of chunks — the port's replacement for {@code ForgeChunkManager.Ticket}.
 *
 * <p>A ticket is obtained from {@link ChunkLoadingManager#requestTicket}, told which chunks it wants
 * ({@link #force}/{@link #setChunks}), and {@link #release}d when its owner dies. Chunks are
 * reference-counted by the manager, so two missiles over the same chunk do not unforce it from under
 * each other — which is the one thing vanilla's flat forced-chunk set cannot express on its own.
 *
 * <p>Releasing a ticket twice is a no-op, and a released ticket ignores further {@code force} calls:
 * upstream's contract note 4 ("if init receives a duplicate ticket, release it rather than leaking it")
 * exists because leaked tickets silently exhausted the mod's budget. There is no budget here, but a
 * leaked claim would pin a chunk forever, which is worse.
 */
public final class ChunkLoaderTicket {

    private final ServerLevel level;
    /** Purely for logging when a ticket is dropped or reconciled. */
    private final String owner;
    private final LongOpenHashSet chunks = new LongOpenHashSet();
    private boolean released;

    ChunkLoaderTicket(ServerLevel level, String owner) {
        this.level = level;
        this.owner = owner;
    }

    public ServerLevel level() {
        return level;
    }

    public String owner() {
        return owner;
    }

    public boolean isReleased() {
        return released;
    }

    public LongOpenHashSet chunks() {
        return chunks;
    }

    /** Adds one chunk to this ticket's claim. Idempotent. */
    public void force(int chunkX, int chunkZ) {
        if (released) return;
        long packed = ChunkPos.pack(chunkX, chunkZ);
        if (chunks.add(packed)) ChunkLoadingManager.acquire(level, packed);
    }

    /** Drops one chunk from this ticket's claim. Idempotent. */
    public void unforce(int chunkX, int chunkZ) {
        if (released) return;
        long packed = ChunkPos.pack(chunkX, chunkZ);
        if (chunks.remove(packed)) ChunkLoadingManager.releaseChunk(level, packed);
    }

    /**
     * Replaces the claim with exactly {@code wanted}, acquiring and releasing only the difference.
     *
     * <p>Upstream's {@code loadNeighboringChunks} unforced everything and re-forced the new set every
     * tick. Doing that literally here would drop each chunk's refcount to zero for an instant and let
     * the chunk actually unload mid-flight, so the set is diffed instead — same end state, without the
     * gap.
     */
    public void setChunks(LongOpenHashSet wanted) {
        if (released) return;

        LongIterator it = chunks.iterator();
        while (it.hasNext()) {
            long packed = it.nextLong();
            if (!wanted.contains(packed)) {
                it.remove();
                ChunkLoadingManager.releaseChunk(level, packed);
            }
        }
        for (LongIterator wantedIt = wanted.iterator(); wantedIt.hasNext(); ) {
            long packed = wantedIt.nextLong();
            if (chunks.add(packed)) ChunkLoadingManager.acquire(level, packed);
        }
    }

    /** Drops every chunk this ticket holds and retires it. */
    public void release() {
        if (released) return;
        released = true;
        for (LongIterator it = chunks.iterator(); it.hasNext(); ) {
            ChunkLoadingManager.releaseChunk(level, it.nextLong());
        }
        chunks.clear();
        ChunkLoadingManager.retire(this);
    }
}
