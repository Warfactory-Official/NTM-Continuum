package com.hbm.util;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.GraphSegment;
import com.hbm.uninos.graph.NodeNetwork;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Set;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The counts 1.7's net diagnostics printed ({@code links}, {@code providerEntries}, {@code receiverEntries}),
 * read off the port's node graph.
 *
 * <p>1.7 nets kept subscriber lists that machines re-joined every tick. The graph pulls instead and keeps no
 * such list, so the endpoints are counted the way the distributor finds them: every machine touching a cell of
 * the net, once each, a multiblock by its core. A machine that both provides and receives (a battery) counts
 * in both columns, as it subscribed to both lists in 1.7.
 */
public final class NetworkDiagnostics {

    private NetworkDiagnostics() {}

    public static void forEachCell(NodeNetwork<?> net, LongConsumer cell) {
        for (GraphNode<?> node : net.nodes) cell.accept(node.posKey);
        for (GraphSegment<?> segment : net.segments) {
            for (int i = 0; i < segment.cellCount(); i++) cell.accept(segment.cellAt(i));
        }
    }

    /** The distinct machines (by core) next to the net's cells that {@code filter} accepts. */
    public static int countEndpoints(ServerLevel level, NodeNetwork<?> net, Predicate<BlockEntity> filter) {
        LongOpenHashSet cells = new LongOpenHashSet();
        forEachCell(net, cells::add);
        Set<BlockEntity> seen = new java.util.HashSet<>();
        cells.forEach((long key) -> {
            BlockPos cell = BlockPos.of(key);
            for (Direction dir : Direction.values()) {
                BlockPos at = cell.relative(dir);
                if (cells.contains(at.asLong())) continue;
                if (level.getChunkSource().getChunkNow(at.getX() >> 4, at.getZ() >> 4) == null) continue;
                BlockEntity be = MultiblockSurface.coreEntity(level, at);
                if (be == null) be = level.getBlockEntity(at);
                if (be != null && filter.test(be)) seen.add(be);
            }
        });
        return seen.size();
    }
}
