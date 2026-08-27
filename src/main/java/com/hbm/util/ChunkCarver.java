package com.hbm.util;

import dev.golem.core.world.Compat;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Clears a bitset's worth of blocks out of one chunk by writing straight into its
 * {@link LevelChunkSection}s, then repairing the per-chunk state that vanilla would have maintained
 * along the way — heightmaps, sky-light sources, section light status and the clients' copy of the
 * chunk — <b>once for the whole chunk</b> instead of once per block.
 *
 * <p><b>Why this exists.</b> A Fat Man clears millions of blocks. Through {@code Level.setBlock} each
 * one pays a palette write, a block-entity lookup, a POI update, four heightmap updates, a light-engine
 * {@code checkBlock} and a change notification to every tracking player's chunk holder. Only the palette
 * write is actually per-block work; everything else is per-chunk work being done a million times. The
 * {@code nuke_write_throughput} GameTest measures the raw gap between the two write paths at
 * <b>2–6×</b>, and that understates it — the lighting backlog the per-block path queues up is what a
 * player feels as lag after the blast has "finished".
 *
 * <p>Both references do the same thing in their default (parallelised) crater algorithm: carve a
 * section wholesale, then run one {@code updateSectionStatus} / {@code propagateLightSources} / chunk
 * resend per chunk. Their carve happens off-thread against a copy that is CAS-swapped into the chunk;
 * this one runs on the server thread and writes in place, which is the same work without the
 * concurrency machinery — nothing else can observe a half-carved section from the server thread.
 *
 * <p><b>What is deliberately not done,</b> matching both the reference and the per-block path this
 * replaces: no neighbour updates, no shape updates and no drops. A nuke suppresses all three (the old
 * path passed {@code UPDATE_KNOWN_SHAPE | UPDATE_SUPPRESS_DROPS}), so a carve that skipped them changes
 * nothing.
 *
 * <p><b>Block entities are not carved.</b> A raw section write would leave the block entity attached to
 * the chunk with no block under it, and its POI record behind. They are collected during the walk and
 * cleared through {@code Level.setBlock} in {@link #finish()}, which is what the reference does too;
 * there are a handful per crater, so the per-block cost of that path does not matter.
 *
 * <p>Bit layout is the caller's: {@code ((maxY - y) << 8) | ((x & 15) << 4) | (z & 15)}, which walks
 * top-down and so visits each section's cells contiguously.
 */
public final class ChunkCarver {

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** Flags for the block-entity cells, identical to what the per-block destruction path used. */
    private static final int BE_CLEAR_FLAGS =
            Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /**
     * The heightmaps a {@link LevelChunk} keeps live. Re-primed in one pass per chunk rather than
     * updated per block: a crater removes the surface block of most of its columns, which is the case
     * where {@code Heightmap.update} stops being cheap and rescans the column anyway.
     */
    private static final Set<Heightmap.Types> CARVED_HEIGHTMAPS = EnumSet.of(
            Heightmap.Types.MOTION_BLOCKING,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            Heightmap.Types.OCEAN_FLOOR,
            Heightmap.Types.WORLD_SURFACE);

    /** Blocks between wall-clock checks. The clock read is the expensive part of a tight carve loop. */
    private static final int CLOCK_INTERVAL = 1024;

    private final ServerLevel level;
    private final LevelChunk chunk;
    private final BitSet hits;
    private final int chunkX;
    private final int chunkZ;
    private final int maxY;
    private final int minSectionY;

    /** One flag per section index; the light engine is told about exactly these at the end. */
    private final boolean[] touched;
    private final LongArrayList blockEntityCells = new LongArrayList();
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private int cursor;
    private int carved;

    public ChunkCarver(ServerLevel level, LevelChunk chunk, BitSet hits, int chunkX, int chunkZ, int maxY) {
        this.level = level;
        this.chunk = chunk;
        this.hits = hits;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.maxY = maxY;
        this.minSectionY = level.getMinSectionY();
        this.touched = new boolean[chunk.getSections().length];
        this.cursor = hits.nextSetBit(0);
    }

    /** Blocks cleared so far, block entities included. */
    public int carved() {
        return carved;
    }

    /**
     * Carves until the bitset runs out or {@code deadlineMs} passes. Returns true when the chunk's
     * bitset is exhausted and {@link #finish()} should run.
     */
    public boolean carveUntil(long deadlineMs) {
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        // The section is resolved once per section, not once per block: the bit order is top-down, so a
        // section's ~4096 cells arrive as one contiguous run.
        int sectionY = Integer.MIN_VALUE;
        LevelChunkSection section = null;
        int sinceClock = 0;

        while (cursor >= 0) {
            int y = maxY - (cursor >>> 8);
            if ((y >> 4) != sectionY) {
                sectionY = y >> 4;
                int index = level.getSectionIndex(y);
                section = index >= 0 && index < sections.length ? sections[index] : null;
                if (section != null) touched[index] = true;
            }

            if (section != null) {
                int localX = (cursor >> 4) & 15;
                int localZ = cursor & 15;
                BlockState old = section.getBlockState(localX, y & 15, localZ);
                if (!old.isAir()) {
                    if (old.hasBlockEntity()) {
                        blockEntityCells.add(BlockPos.asLong(baseX | localX, y, baseZ | localZ));
                    } else {
                        // useLocks = false: the server thread owns this section, and the whole point of
                        // the carve is to skip the per-write bookkeeping.
                        section.setBlockState(localX, y & 15, localZ, AIR, false);
                        carved++;
                    }
                }
            }

            cursor = hits.nextSetBit(cursor + 1);

            if (++sinceClock >= CLOCK_INTERVAL) {
                sinceClock = 0;
                if (System.currentTimeMillis() >= deadlineMs) return cursor < 0;
            }
        }
        return true;
    }

    /**
     * Repairs everything the raw section writes bypassed, once for the whole chunk, and ships the result
     * to the players tracking it once the light engine has caught up.
     */
    public void finish() {
        // Block entities first, through the level, so the block entity and its POI record are torn down
        // properly and the relight below sees the finished shape.
        for (int i = 0; i < blockEntityCells.size(); i++) {
            long packed = blockEntityCells.getLong(i);
            pos.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
            level.setBlock(pos, AIR, BE_CLEAR_FLAGS);
            carved++;
        }
        blockEntityCells.clear();

        repairAndResend(level, chunk, chunkX, chunkZ, touched);
    }

    /**
     * Repairs one chunk after <b>any</b> bulk section write and resends it to trackers <b>once the light
     * engine has actually run</b>. Shared with the fallout sweep, which writes through the sections for
     * the same reason the crater does.
     *
     * <p><b>The relight itself is small, and that is a measured result rather than an assumption.</b>
     * A carve only ever removes blocks, so light can only ever increase, and both increase paths are
     * already seeded per chunk: {@code propagateLightSources} re-seeds sky light from the
     * {@code ChunkSkyLightSources} that {@link #finish()} has just refilled, and its block-light half
     * re-enqueues every emitter in the chunk. There is no decrease to drive, which is the only thing
     * vanilla's per-block {@code checkBlock} would add — and {@code checkBlock} on the server allocates
     * a task and submits it to the light thread's dispatcher <b>per call</b>, which is unaffordable at
     * crater scale and the reason the carve exists at all.
     *
     * <p>A full wipe-and-relight (the {@code ThreadedLevelLightEngine.updateChunkStatus} sequence) was
     * written, run against {@code nuke_crater_lighting} and then <b>removed</b>: it changed nothing the
     * test could see, on a real blast, while costing a whole-chunk relight per crater chunk.
     *
     * <p><b>What was actually broken was the resend.</b> Building the packet straight after queueing the
     * relight ships the light as it stands, which for a crater interior is the solid rock's zero — a
     * pitch-black crater on the client while the server thinks it is lit. Vanilla's own follow-up
     * ({@code ChunkHolder.sectionLightChanged} → {@code ClientboundLightUpdatePacket}) does not save it:
     * that path gives up when {@code getTickingChunk()} is null, which is exactly the state a crater
     * chunk held open by the blast's own chunk ticket can be in. So the packet has to carry the finished
     * light itself.
     *
     * <p>{@code lightChunk(chunk, false)} is vanilla's own "light this chunk" call and returns a future
     * that completes as a post-update task — i.e. after {@code runLightUpdates} — which is the exact
     * "this chunk's light is settled" signal, and the one piece of this that exists identically on every
     * supported version ({@code waitForPendingTasks}, which the fork uses, does not exist on 1.20.1).
     */
    public static void repairAndResend(ServerLevel level, LevelChunk chunk, int chunkX, int chunkZ,
                                       boolean[] touched) {
        int minSectionY = level.getMinSectionY();
        LevelChunkSection[] sections = chunk.getSections();

        Heightmap.primeHeightmaps(chunk, CARVED_HEIGHTMAPS);
        // The lowest sky-light source per column moves wherever a roof opened or closed. This is what the
        // sky-light seeding below reads, so it has to be current before the relight is queued.
        // Replacement light engines can intentionally discard vanilla's source map and intercept this hook.
        chunk.initializeLightSources();

        ThreadedLevelLightEngine lightEngine = level.getChunkSource().getLightEngine();
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        for (int index = 0; index < touched.length; index++) {
            if (!touched[index]) continue;
            boolean empty = sections[index].hasOnlyAir();
            if (empty) Compat.onSectionEmptinessChanged(level, chunkX, index + minSectionY, chunkZ, true);
            // The storage keeps a has-data flag and a neighbour count per section, and both are wrong
            // until it is told.
            lightEngine.updateSectionStatus(SectionPos.of(chunkPos, index + minSectionY), empty);
        }

        // Nothing else will: a section write does not touch the chunk's dirty flag, so unsaved work
        // would be silently rolled back by the next chunk load.
        Compat.markUnsaved(chunk);

        MinecraftServer server = level.getServer();
        lightEngine.lightChunk(chunk, false)
                .thenAcceptAsync(ignored -> resend(level, chunkX, chunkZ, chunkPos), server);
    }

    /**
     * Sends the finished chunk to whoever is tracking it — the same way vanilla tells a client about a
     * heavily-changed chunk. ({@code ChunkHolder} resends the whole chunk as soon as more than 64 blocks
     * change in one section, which every crater chunk does by a wide margin.)
     */
    private static void resend(ServerLevel level, int chunkX, int chunkZ, ChunkPos chunkPos) {
        // Re-resolved rather than captured: this runs some ticks after the write and the chunk may have
        // been unloaded in between.
        LevelChunk current = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (current == null) return;
        List<ServerPlayer> players = level.getChunkSource().chunkMap.getPlayers(chunkPos, false);
        if (players.isEmpty()) return;
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(
                current, level.getChunkSource().getLightEngine(), null, null);
        for (ServerPlayer player : players) player.connection.send(packet);
    }
}
