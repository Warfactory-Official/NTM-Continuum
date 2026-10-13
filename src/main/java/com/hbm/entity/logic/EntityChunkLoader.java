package com.hbm.entity.logic;

import com.hbm.world.chunk.ChunkLoaderTicket;
import com.hbm.world.chunk.ChunkLoadingManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

/**
 * The chunk-loading half of a chunkloading entity, held by composition.
 *
 * <p>1.12.2 spelled this logic out three times: once in {@code EntityExplosionChunkloading}, once in
 * {@code EntityMissileBaseNT} and once in {@code EntityMissileAntiBallistic}, because all three
 * needed a different superclass. Java still only has one superclass, so the shared part lives here and
 * each entity holds one, forwarding its {@link IChunkLoader} methods.
 *
 * <p>The {@code radius} selects the claim envelope: stationary logic entities use their own chunk
 * (radius 0), while moving missiles use the 3×3 around themselves (radius 1) so asynchronous modern
 * tickets request the next border before crossing it.
 */
public final class EntityChunkLoader implements IChunkLoader {

    private final Entity entity;
    private final int radius;
    private ChunkLoaderTicket ticket;
    /**
     * Set when the entity was read from NBT. The first request after a load is skipped so the restore
     * ticket (which already holds this chunk) gets a tick to be counted first, the direct analogue of
     * upstream's {@code awaitingTicketRestore}, which waited for Forge's {@code LoadingCallback}.
     */
    private boolean awaitingRestore;

    public EntityChunkLoader(Entity entity, int radius) {
        this.entity = entity;
        this.radius = radius;
    }

    /** Call from {@code loadData}: this entity came back from disk. */
    public void markRestoredFromNbt() {
        this.awaitingRestore = true;
    }

    /**
     * Call once per server tick, before movement. Requests a ticket if needed, then moves the claim to
     * the entity's current chunk.
     */
    public void tick() {
        if (!(entity.level() instanceof ServerLevel)) return;
        initChunkLoading();
        loadNeighboringChunks(
                Math.floorDiv((int) Math.floor(entity.getX()), 16),
                Math.floorDiv((int) Math.floor(entity.getZ()), 16));
    }

    @Override
    public void initChunkLoading() {
        if (ticket != null || !(entity.level() instanceof ServerLevel level)) return;
        if (awaitingRestore) {
            awaitingRestore = false;
            return;
        }
        ticket = ChunkLoadingManager.requestTicket(level, entity.getType().toString());
    }

    @Override
    public void loadNeighboringChunks(int chunkX, int chunkZ) {
        if (ticket == null || ticket.isReleased()) return;
        LongOpenHashSet wanted = new LongOpenHashSet((radius * 2 + 1) * (radius * 2 + 1));
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                wanted.add(ChunkPos.pack(chunkX + i, chunkZ + j));
            }
        }
        ticket.setChunks(wanted);
    }

    @Override
    public void clearChunkLoader() {
        if (ticket == null) return;
        ticket.release();
        ticket = null;
    }

    /** Whether a ticket is currently held. Diagnostics and tests. */
    public boolean isLoading() {
        return ticket != null && !ticket.isReleased();
    }
}
