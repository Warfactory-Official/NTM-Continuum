package com.hbm.uninos.graph;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import com.hbm.util.ChunkPositionIndex;
import dev.golem.core.platform.Lifecycle;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Where a single-block machine declares that it is something a net can serve, so discovery never
 * depends on probing for content we own. Per level, not per graph.
 *
 * <p>Upstream reaches its per-level instance through a {@code hbm$endpoints()} mixin on
 * {@code ServerLevel}. Continuum holds them in a map keyed by level instead — one mixin fewer to
 * carry across three Minecraft versions and three loaders, and the same server-thread confinement
 * {@link com.hbm.uninos.UniNodespace} already relies on. {@link #clearLevel} is the counterpart of
 * that choice: without it the map would pin an unloaded level forever.
 */
public final class EndpointRegistry {

    /** The domains a net can probe for; items answer through the item route, which no net gathers. */
    private static final int NET_DOMAINS = MachineCaps.POWER_IN | MachineCaps.POWER_OUT
            | MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT | MachineCaps.FE;

    private static final Reference2ObjectOpenHashMap<Level, EndpointRegistry> LEVELS =
            new Reference2ObjectOpenHashMap<>();

    private final Long2ObjectOpenHashMap<Endpoint> byCell = new Long2ObjectOpenHashMap<>();
    private final ChunkPositionIndex byChunk = new ChunkPositionIndex();

    public static EndpointRegistry of(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, key -> new EndpointRegistry());
    }

    public static void init() {
        Lifecycle.onChunkUnload((level, chunk) ->
                of(level).onChunkUnload(ChunkPositionIndex.chunkOf(chunk.getPos().getWorldPosition().asLong())));
    }

    /** Drops a level's table on unload; nothing else removes a level from the map. */
    public static void clearLevel(Level level) {
        LEVELS.remove(level);
    }

    /**
     * Loses nothing: a block entity re-declares every endpoint its chunk carries when the chunk comes
     * back. Without this the table is append-only for the life of the level.
     */
    public void onChunkUnload(long chunkKey) {
        LongOpenHashSet cells = byChunk.take(chunkKey);
        if (cells == null) return;
        for (LongIterator it = cells.iterator(); it.hasNext(); ) byCell.remove(it.nextLong());
    }

    /**
     * Declares a single-block machine's own position on its own behalf, from the block entity's
     * {@code clearRemoved}. The entry is an optimisation, never a gate.
     */
    public static void declareSelfIfEndpoint(ServerLevel level, BlockEntity be) {
        if (!(be.getBlockState().getBlock() instanceof ICapabilityBlock declaring)) return;
        int domains = declaring.caps().declaredBits();
        if ((domains & NET_DOMAINS) != 0) {
            of(level).declare(be.getBlockPos(), be.getBlockPos(), MachineCaps.FACE_MASK_ALL, domains);
        }
    }

    /**
     * Is the memoised chunk still at FULL status? {@code getFullStatus} reads the holder's live
     * ticket level, which {@code !isRemoved()} on the block entity does not imply.
     */
    private static boolean stillFull(@Nullable WeakReference<LevelChunk> memo) {
        LevelChunk chunk = memo == null ? null : memo.get();
        return chunk != null && chunk.getFullStatus().isOrAfter(FullChunkStatus.FULL);
    }

    private static @Nullable LevelChunk readableChunk(ServerLevel level, int x, int z) {
        return level.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
    }

    /**
     * Declares {@code cell} as exposing {@code faceMask} on behalf of {@code owner}. Idempotent, since
     * a block entity re-declares on every chunk load.
     */
    public void declare(BlockPos cell, BlockPos owner, int faceMask, int domains) {
        long cellKey = cell.asLong();
        if (faceMask == 0) {
            byCell.remove(cellKey);
            byChunk.remove(cellKey);
            return;
        }
        byCell.put(cellKey, new Endpoint(owner.asLong(), faceMask, domains));
        byChunk.add(cellKey);
    }

    public void withdraw(BlockPos cell) {
        byCell.remove(cell.asLong());
        byChunk.remove(cell.asLong());
    }

    /** The raw declaration, unvalidated, for bookkeeping. Transfer paths want {@link #resolve}. */
    public @Nullable Endpoint declared(long cellKey) {
        return byCell.get(cellKey);
    }

    /**
     * The live owner exposed at {@code cellKey} on {@code face} for one of {@code domains}, or null.
     * The declaration is an optimisation, never a gate.
     */
    public @Nullable BlockEntity resolve(ServerLevel level, long cellKey, @Nullable Direction face, int domains) {
        Endpoint ep = byCell.get(cellKey);
        if (ep == null) return null;
        if ((ep.domains() & domains) == 0) return null;
        if (face != null && !ep.isOpen(face)) return null;
        WeakReference<BlockEntity> memo = ep.resolved;
        BlockEntity cached = memo == null ? null : memo.get();
        if (cached != null && !cached.isRemoved() && stillFull(ep.resolvedChunk)) return cached;
        BlockPos owner = BlockPos.of(ep.owner());
        LevelChunk chunk = readableChunk(level, owner.getX(), owner.getZ());
        if (chunk == null) {
            ep.resolved = null;
            ep.resolvedChunk = null;
            return null;
        }
        BlockEntity be = chunk.getBlockEntity(owner);
        if (be == null || be.isRemoved()) {
            ep.resolved = null;
            ep.resolvedChunk = null;
            return null;
        }
        ep.resolved = new WeakReference<>(be);
        ep.resolvedChunk = new WeakReference<>(chunk);
        return be;
    }

    public int size() {
        return byCell.size();
    }

    /**
     * One declared exposure: owner, a {@code 1 << Direction.ordinal()} face mask, and the
     * {@link MachineCaps} domains.
     */
    public static final class Endpoint {

        private final long owner;
        private final int faceMask;
        private final int domains;

        /**
         * The last owner that resolved, weak on purpose: a strong field would pin a removed block
         * entity, and its inventories, until something resolved that cell again — which can be never.
         */
        private @Nullable WeakReference<BlockEntity> resolved;

        /** The owner's chunk, memoised alongside it, because a live block entity can sit in an unreadable chunk. */
        private @Nullable WeakReference<LevelChunk> resolvedChunk;

        public Endpoint(long owner, int faceMask, int domains) {
            this.owner = owner;
            this.faceMask = faceMask;
            this.domains = domains;
        }

        public long owner() {
            return owner;
        }

        public int faceMask() {
            return faceMask;
        }

        public int domains() {
            return domains;
        }

        public boolean isOpen(Direction face) {
            return (faceMask & (1 << face.ordinal())) != 0;
        }
    }
}
