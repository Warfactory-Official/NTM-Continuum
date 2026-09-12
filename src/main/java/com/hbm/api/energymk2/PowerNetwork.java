package com.hbm.api.energymk2;

import com.hbm.capability.MachineCaps;
import com.hbm.uninos.graph.Distribution;
import com.hbm.uninos.graph.EdgeFaces;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import dev.golem.core.platform.Lifecycle;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One round of energy over the node graph: find the machines at the net's edge faces, serve them by
 * {@linkplain EnergyBus the 1.7 law}, and charge the round back to whoever funded it.
 *
 * <p>This is deliberately smaller than the upstream it answers to, because Continuum does not need
 * most of what makes that one big. Upstream reaches every endpoint through a registered capability,
 * so it carries a capability service, a role enum and a folded-multiblock resolver to ask a machine
 * what it is. Continuum owns its machines: the endpoint registry hands back the owning block entity
 * and {@code instanceof IEnergyHandlerMK2} answers the same question with nothing in between. A
 * capability lookup is only needed for machines that are somebody else's, which is
 * {@link dev.golem.core.world.EnergyCompat} and belongs to the foreign bridge.
 *
 * <p><b>Inert as it stands.</b> Cables still mint UNINOS nodes and nothing calls
 * {@code graph.addNode}, so every net here is empty and this runs over nothing. Wiring the cables is
 * the commit that flips behaviour, and the acceptance suite is what gets to judge it.
 */
public final class PowerNetwork {

    private static final Direction[] DIRS = Direction.values();
    private static final int PRIORITIES = IEnergyHandlerMK2.ConnectionPriority.VALUES.length;
    private static final int POWER = MachineCaps.POWER_IN | MachineCaps.POWER_OUT;

    /**
     * A face answers PRESENT on anything that could ever be an endpoint, not on what it holds this
     * tick: a machine that is full now is still a machine, and answering ABSENT would park the face
     * until something invalidated it.
     */
    private static final LevelNodeGraph.FaceProbe<CableData> FACE_PROBE =
            (level, graph, cellPos, faces, faceOrdinal) -> {
                BlockPos neighbour = neighbourOf(cellPos, faceOrdinal);
                return handlerAt(level, neighbour, faceOrdinal) == null
                        ? LevelNodeGraph.Probe.ABSENT : LevelNodeGraph.Probe.PRESENT;
            };

    private PowerNetwork() {}

    public static void init() {
        EndpointRegistry.init();
        Lifecycle.onServerTick(PowerNetwork::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            LevelNodeGraph<CableData> graph = PowerGraph.get(level);
            if (graph.nodeCount() == 0) continue;
            graph.applyCompiledSwaps();
            for (NodeNetwork<CableData> net : graph.networks()) distribute(level, graph, net);
        }
    }

    public static void distribute(ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        graph.drainProbes(net, level, FACE_PROBE);

        Gather gather = new Gather();
        for (BlockEntity be : endpointsOf(level, graph, net)) {
            if (!(be instanceof IEnergyHandlerMK2 handler) || !handler.isLoaded()) continue;
            gather.offer(handler);
        }
        if (gather.totalSupply() <= 0L) return;

        long toTransfer = Math.min(gather.totalSupply(), gather.totalDemand());
        long delivered = 0L;
        if (toTransfer > 0L) {
            delivered = toTransfer >= gather.totalDemand()
                    ? EnergyBus.serveInFull(gather.receivers, toTransfer, PowerNetwork::deliver)
                    : EnergyBus.serveShortage(gather.receivers, gather.demand, toTransfer, PowerNetwork::deliver);
            net.tracker += delivered;
        }

        // The pool is the round's residual: it takes what the real providers still hold, or covers
        // what they could not, and the two are mutually exclusive by construction.
        long owed = delivered;
        long untaken = gather.powerAvailable - Math.min(owed, gather.powerAvailable);
        if (untaken > 0L && gather.bufferDemand > 0L) {
            long absorbed = EnergyBus.serveShortage(gather.bufferReceivers, gather.bufferHeadroom,
                    untaken, PowerNetwork::deliver);
            net.tracker += absorbed;
            owed += absorbed;
        }
        settle(gather, owed, net);
    }

    /**
     * Charge the round back. Real providers pay first and buffers only cover the shortfall, lowest
     * tier first — a buffer's priority reads "how full I want this one", so the overflow tank empties
     * before the main bank.
     */
    private static void settle(Gather gather, long owed, NodeNetwork<CableData> net) {
        if (owed <= 0L) return;
        long[] charged = {0L};
        Distribution.Apply<IEnergyHandlerMK2> apply = (provider, amount) -> {
            provider.usePower(amount);
            charged[0] += amount;
            return amount;
        };
        if (net.providerCursors == null) net.providerCursors = new int[1 + PRIORITIES];

        long fromReal = Math.min(owed, gather.powerAvailable);
        if (fromReal > 0L) {
            Distribution.debit(gather.providers, fromReal, gather.powerAvailable, net.providerCursors, 0, apply);
        }
        long remaining = owed - charged[0];
        for (int tier = 0; tier < PRIORITIES && remaining > 0L; tier++) {
            long supply = gather.bufferSupply[tier];
            if (supply <= 0L) continue;
            long before = charged[0];
            Distribution.debit(gather.bufferProviders[tier], Math.min(remaining, supply), supply,
                    net.providerCursors, 1 + tier, apply);
            remaining -= charged[0] - before;
        }
    }

    private static long deliver(IEnergyHandlerMK2 receiver, long amount) {
        return amount - receiver.transferPower(amount, false);
    }

    /** Every distinct machine the net's active faces look at, deduplicated across faces. */
    private static List<BlockEntity> endpointsOf(ServerLevel level, LevelNodeGraph<CableData> graph,
                                                 NodeNetwork<CableData> net) {
        List<BlockEntity> found = new ArrayList<>();
        ReferenceOpenHashSet<BlockEntity> seen = new ReferenceOpenHashSet<>();
        for (var node : graph.activeFaces(net)) {
            EdgeFaces faces = node.ef;
            if (faces == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                if ((faces.activeMask & (1 << d)) == 0) continue;
                BlockEntity be = handlerAt(level, neighbourOf(node.posKey, d), d);
                if (be != null && seen.add(be)) found.add(be);
            }
        }
        return found;
    }

    /**
     * The machine a face looks at. The endpoint registry is asked first because it answers without
     * touching the chunk; a machine that never declared still resolves, because a declaration is an
     * optimisation and never a gate.
     */
    private static @Nullable BlockEntity handlerAt(ServerLevel level, BlockPos pos, int faceOrdinal) {
        Direction face = faceOrdinal == EdgeFaces.SELF ? null : DIRS[faceOrdinal].getOpposite();
        BlockEntity declared = EndpointRegistry.of(level).resolve(level, pos.asLong(), face, POWER);
        if (declared instanceof IEnergyHandlerMK2) return declared;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof IEnergyHandlerMK2 ? be : null;
    }

    private static BlockPos neighbourOf(long cellPos, int faceOrdinal) {
        BlockPos cell = BlockPos.of(cellPos);
        return faceOrdinal == EdgeFaces.SELF ? cell : cell.relative(DIRS[faceOrdinal]);
    }

    /** One round's endpoints, split into the tiers the bus serves and the pool that settles it. */
    private static final class Gather {

        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IEnergyHandlerMK2>>[] receivers = new List[PRIORITIES];
        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IEnergyHandlerMK2>>[] bufferReceivers = new List[PRIORITIES];
        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IEnergyHandlerMK2>>[] bufferProviders = new List[PRIORITIES];
        final List<Distribution.Share<IEnergyHandlerMK2>> providers = new ArrayList<>();
        final long[] demand = new long[PRIORITIES];
        final long[] bufferHeadroom = new long[PRIORITIES];
        final long[] bufferSupply = new long[PRIORITIES];
        long powerAvailable;
        long bufferPower;
        long totalDemand;
        long bufferDemand;

        Gather() {
            for (int i = 0; i < PRIORITIES; i++) {
                receivers[i] = new ArrayList<>();
                bufferReceivers[i] = new ArrayList<>();
                bufferProviders[i] = new ArrayList<>();
            }
            Arrays.fill(demand, 0L);
        }

        void offer(IEnergyHandlerMK2 handler) {
            int tier = handler.getPriority().ordinal();
            boolean buffer = handler.bufferedEndpoint();

            long supply = Math.min(handler.getPower(), handler.getProviderSpeed());
            if (supply > 0L) {
                if (buffer) {
                    bufferProviders[tier].add(new Distribution.Share<>(handler, supply));
                    bufferSupply[tier] += supply;
                    bufferPower += supply;
                } else {
                    providers.add(new Distribution.Share<>(handler, supply));
                    powerAvailable += supply;
                }
            }

            long headroom = Math.min(handler.getMaxPower() - handler.getPower(), handler.getReceiverSpeed());
            if (headroom > 0L) {
                if (buffer) {
                    bufferReceivers[tier].add(new Distribution.Share<>(handler, headroom));
                    bufferHeadroom[tier] += headroom;
                    bufferDemand += headroom;
                } else {
                    receivers[tier].add(new Distribution.Share<>(handler, headroom));
                    demand[tier] += headroom;
                    totalDemand += headroom;
                }
            }
        }

        long totalSupply() {
            return powerAvailable + bufferPower;
        }

        long totalDemand() {
            return totalDemand;
        }
    }
}
