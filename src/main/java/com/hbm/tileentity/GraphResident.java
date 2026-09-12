package com.hbm.tileentity;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What a block entity tells the graph when it arrives: the faces around it may have changed, and if it
 * is an endpoint in its own right it says so.
 *
 * <p>Called from {@code clearRemoved}, which fires on every chunk load, so both halves are idempotent
 * by construction.
 */
public interface GraphResident {

    static void onLoad(BlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel server)) return;
        LevelNodeGraph.invalidateEndpointsAround(server, be.getBlockPos());
        if (!(be.getBlockState().getBlock() instanceof BlockMultiblockCore)) {
            EndpointRegistry.declareSelfIfEndpoint(server, be);
        }
    }
}
