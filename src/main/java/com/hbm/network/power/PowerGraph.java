package com.hbm.network.power;

import dev.golem.core.graph.LevelNodeGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The per-level cable topology: one {@code hbm:power_graph} save per dimension. */
public final class PowerGraph {

    private PowerGraph() {}

    public static LevelNodeGraph<CableData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, PowerGraphProvider.INSTANCE, com.hbm.Constants.MOD_ID + ":power_graph");
    }

    /**
     * Drops the cells a block entity owns, from its own removal, and only if its block went with it.
     *
     * <p>The block's removal hook would be the obvious place and is the wrong one: vanilla calls it
     * only when the caller asked for a neighbour update. Everything a player does passes that flag; a
     * structure placement, a fill, or a gametest clearing its plot does not, and a cell whose block
     * left that way would outlive it for good. {@link LevelNodeGraph#blockWentWithIt} is what lets the
     * block entity answer the question the hook would have answered.
     *
     * @return whether the cells were dropped, so a caller with more to withdraw can ask once.
     */
    public static boolean dropCells(BlockEntity owner, long... cells) {
        if (!(owner.getLevel() instanceof ServerLevel server) || !LevelNodeGraph.blockWentWithIt(owner)) {
            return false;
        }
        LevelNodeGraph<CableData> graph = get(server);
        for (long cell : cells) graph.removeNode(cell);
        return true;
    }

    public static boolean dropCells(BlockEntity owner, BlockPos cell) {
        return dropCells(owner, cell.asLong());
    }
}
