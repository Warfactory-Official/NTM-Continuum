package com.hbm.tileentity.network;

import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.network.RequestNetwork.PathNode;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.hbm.packet.DroneLinePayload;

/**
 * 1.7 {@code TileEntityRequestNetwork} + {@code TileEntityRequestNetworkContainer}: a block entity that publishes
 * itself to the drone {@link RequestNetwork} once a second and tracks which of its neighbours it can see.
 *
 * <p>1.7 split this over two classes because the container half could not inherit the machine base; the machine
 * base already carries an inventory here, so the split collapses and a node with no inventory registers zero slots.
 */
public abstract class TileEntityRequestNetwork extends TileEntityMachineBase implements MenuProvider {

    /** 1.7: new nodes adopted per scan. */
    private static final int DISCOVERY_BUDGET = 5;
    private static final int LOCAL_CHUNK_RANGE = 2;
    private static final int SCAN_PERIOD = 20;

    /** Positions this node has met; a subset of them answers the line-of-sight test. */
    protected final Set<BlockPos> known = new HashSet<>();
    protected final Set<BlockPos> reachable = new HashSet<>();
    /** This node's own last snapshot, published by the scan below before anything else reads the index. */
    protected @Nullable PathNode published;

    protected TileEntityRequestNetwork(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots);
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    /** 1.7 getCoord: where a drone meets this node, one above the block. */
    public BlockPos nodePos() {
        return worldPosition.above();
    }

    protected abstract PathNode createNode(BlockPos pos);

    public void serverTick() {
        if (!(level instanceof ServerLevel server) || server.getGameTime() % SCAN_PERIOD != 0) return;

        BlockPos node = nodePos();
        PathNode fresh = createNode(node);
        fresh.active = !server.hasNeighborSignal(worldPosition);
        // push new node
        RequestNetwork.push(server, fresh);
        published = fresh;

        // remove known nodes that no longer exist; a sane number of nodes exist, so the full check runs every second
        Map<BlockPos, PathNode> local =
                new HashMap<>(RequestNetwork.localNodes(server, worldPosition, LOCAL_CHUNK_RANGE));
        local.remove(node);

        known.removeIf(pos -> {
            if (local.containsKey(pos)) return false;
            reachable.remove(pos);
            return true;
        });

        // draw debug crap
        for (BlockPos pos : known) {
            if (reachable.contains(pos)) DroneLinePayload.send(server, node, pos, 0.5D, 0x00ff00);
        }

        // rescan known nodes
        for (BlockPos pos : known) {
            if (RequestNetwork.hasPath(server, node, pos)) reachable.add(pos);
            else reachable.remove(pos);
        }

        // discover new nodes
        int budget = DISCOVERY_BUDGET;
        for (PathNode candidate : local.values()) {
            if (!areNodesConnectable(candidate, fresh)) continue;

            if (!known.contains(candidate.pos) && !candidate.pos.equals(node)) {
                budget--;
                known.add(candidate.pos);
                if (RequestNetwork.hasPath(server, node, candidate.pos)) reachable.add(candidate.pos);
            }

            if (budget <= 0) break;
        }
    }

    /** A dock, provider or requester reaches its peers only through a waypoint. */
    public static boolean areNodesConnectable(PathNode one, PathNode other) {
        return one.waypoint || other.waypoint;
    }
}
