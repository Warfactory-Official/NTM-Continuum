package com.hbm.uninos;

import dev.golem.core.graph.LevelNodeGraph;
import dev.golem.core.graph.NodeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.uninos.networkproviders.PneumaticNetwork;

/**
 * 1.7 {@code UniNodespace} + {@code PneumaticNetworkProvider}: the pneumatic tube graph of one level. Every
 * tube, storage bank, access terminal, importer and exporter is a node with all six faces open
 * (1.7 {@code setStandardConnections}); the connected component is the network.
 */
public final class PneumaticGraph {

    private PneumaticGraph() {
    }

    public static LevelNodeGraph<PneumaticData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, PneumaticGraphProvider.INSTANCE, com.hbm.Constants.MOD_ID + ":pneumatic_graph");
    }

    /** 1.7 {@code new PneumaticNode(pos).setStandardConnections(...)}: every face open. Idempotent. */
    public static void addNode(ServerLevel level, BlockPos pos) {
        LevelNodeGraph<PneumaticData> graph = get(level);
        long key = pos.asLong();
        if (!graph.containsCell(key)) graph.addNode(key, PneumaticData.INSTANCE, 0x3F);
    }

    public static void removeNode(ServerLevel level, BlockPos pos) {
        get(level).removeNode(pos.asLong());
    }

    /** @see com.hbm.network.fluid.FluidGraph#dropCells(BlockEntity, BlockPos) */
    public static void dropCell(BlockEntity owner, BlockPos pos) {
        if (!(owner.getLevel() instanceof ServerLevel server) || !LevelNodeGraph.blockWentWithIt(owner)) return;
        removeNode(server, pos);
    }

    /**
     * 1.7 {@code PneumaticNetwork.addReceiver}: the receiver is any container at {@code ejectionDir}, node or
     * not. Edge faces aim at node-less positions, so the destination is declared at the tube itself.
     */
    public static void setEndpoint(ServerLevel level, BlockPos pos, boolean on) {
        get(level).setSelfEndpoint(pos.asLong(), on);
    }

    public static @Nullable NodeNetwork<PneumaticData> netAt(ServerLevel level, BlockPos pos) {
        return get(level).networkAt(pos.asLong());
    }
}
