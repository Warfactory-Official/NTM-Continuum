package com.hbm.entity.logic;

/**
 * Contract for entity-backed chunk loaders, carried over from 1.12.2 along with its hard-won rules.
 *
 * <p>1. {@link #initChunkLoading()} is a bind/refresh hook, not the place to decide whether the entity
 * should chunkload at all. In 1.12.2 it took the {@code ForgeChunkManager.Ticket}, fresh or restored;
 * here there is no handback, so it takes nothing and obtains a
 * {@link com.hbm.world.chunk.ChunkLoaderTicket}.
 *
 * <p>2. Do <b>not</b> request a ticket while reading NBT. That old 1.7 pattern ran during
 * deserialization too, so a persisted entity could request a ticket before the restored one arrived.
 *
 * <p>3. Request lazily, once the entity is actually live on the server, from the first server-side
 * tick, or from an explicit opt-in. An entity read from NBT gives the restore path a grace period
 * first (see {@link EntityChunkLoader}).
 *
 * <p>4. Never leak a claim. Upstream released a duplicate ticket immediately rather than holding two;
 * here a ticket that is not released pins its chunks for the rest of the session.
 *
 * <p>5. On death, removal or level shutdown, unforce everything and release the ticket.
 *
 * <p>6. Persist only whether chunkloading should be on. Never persist the ticket.
 */
public interface IChunkLoader {

    /** Binds a chunk-loading ticket for this entity if it does not have one yet. */
    void initChunkLoading();

    /** Moves the claim to follow the entity, given the chunk it is now over. */
    void loadNeighboringChunks(int chunkX, int chunkZ);

    /** Drops the claim entirely. */
    void clearChunkLoader();
}
