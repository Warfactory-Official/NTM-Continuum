package com.hbm.api.energymk2;

import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** The per-level cable topology: one {@code hbm:power_graph} save per dimension. */
public final class PowerGraph {

    private PowerGraph() {}

    public static LevelNodeGraph<CableData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, PowerGraphProvider.INSTANCE, "power_graph");
    }

    /**
     * Drops the cells a block entity owns, from its own removal, and only if its block went with it.
     * Two things make this the place rather than the block's removal hook.
     *
     * <p>A block entity is also removed when its chunk unloads, and dropping cells then would tear a
     * network in half whenever a player walked away from it; the graph parks unloaded cells and keeps
     * them in the save instead. The block state at the position is what tells the two apart — a removal
     * has already written the new state by the time the block entity hears about it, and an unload has
     * written nothing.
     *
     * <p>And vanilla calls a block's removal hook only when the caller asked for a neighbour update.
     * Everything a player does passes that flag; a structure placement, a fill, or a gametest clearing
     * its plot does not, and a cell whose block left that way would outlive it for good.
     *
     * <p>The state is read from the chunk we are handed rather than through the level, because an
     * unload runs this from inside the chunk's own teardown: {@code Level.getBlockState} would ask the
     * chunk source for a chunk that is on its way out, and the server thread would block for it
     * forever. A chunk that cannot be read without loading is an unload by definition, which is the
     * case that answers no anyway.
     *
     * @return whether the cells were dropped, so a caller with more to withdraw can ask once.
     */
    public static boolean dropCells(BlockEntity owner, long... cells) {
        if (!(owner.getLevel() instanceof ServerLevel server)) return false;
        BlockPos pos = owner.getBlockPos();
        LevelChunk chunk = server.getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        if (chunk == null || chunk.getBlockState(pos).is(owner.getBlockState().getBlock())) return false;
        LevelNodeGraph<CableData> graph = get(server);
        for (long cell : cells) graph.removeNode(cell);
        return true;
    }

    public static boolean dropCells(BlockEntity owner, BlockPos cell) {
        return dropCells(owner, cell.asLong());
    }
}
