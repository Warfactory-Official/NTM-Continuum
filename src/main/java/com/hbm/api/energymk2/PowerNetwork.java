package com.hbm.api.energymk2;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.MachineCore;
import com.hbm.capability.MachineCaps;
import com.hbm.config.GeneralConfig;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.Distribution;
import com.hbm.uninos.graph.EdgeFaces;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.uninos.graph.PortClaims;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.world.ChunkPromotions;
import dev.golem.core.world.EnergyCompat;
import dev.golem.core.world.EnergyExposure;
import dev.golem.core.world.EnergyView;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * One round of energy over the node graph: find the machines at the net's edge faces, serve them by
 * {@linkplain EnergyBus the 1.7 law}, and charge the round back to whoever funded it.
 *
 * <p>This is smaller than the upstream it answers to. Upstream reaches every endpoint through a registered
 * capability, so it carries a capability service, a role enum and a folded-multiblock resolver. Continuum
 * owns its machines: the endpoint registry hands back the owning block entity and
 * {@code instanceof IEnergyHandlerMK2} answers the question. A capability lookup is only needed for
 * machines that are somebody else's.
 *
 * <p>Discovery is pull: a conductor probes the faces of its own cells and asks what is on the other side.
 * The role a machine found there plays is 1.7's per-port answer, claimed by its own {@code tryProvide} /
 * {@code trySubscribe} calls ({@link PortClaims}). The three things that can make a parked face live again are a
 * neighbour notification on the conductor, a block entity announcing itself through
 * {@link com.hbm.tileentity.GraphResident} as its chunk loads, and a chunk reaching FULL.
 */
public final class PowerNetwork {

    private static final Direction[] DIRS = Direction.values();
    private static final int PRIORITIES = IEnergyHandlerMK2.ConnectionPriority.VALUES.length;
    private static final int POWER = MachineCaps.POWER_IN | MachineCaps.POWER_OUT;

    /**
     * A net gets a bridge the first time something asks for its FE face, and only then: a world with no
     * foreign mod in it never allocates one, and {@link #distribute} skips the whole re-entrancy dance.
     */
    private static final Reference2ObjectOpenHashMap<NodeNetwork<CableData>, ForeignEnergyBridge> BRIDGES =
            new Reference2ObjectOpenHashMap<>();

    /**
     * A face answers PRESENT on anything that could ever be an endpoint, not on what it holds this
     * tick: a machine that is full now is still a machine, and answering ABSENT would park the face
     * until something invalidated it.
     */
    private static final LevelNodeGraph.FaceProbe<CableData> FACE_PROBE =
            (level, graph, cellPos, faces, faceOrdinal) -> {
                BlockPos neighbour = neighbourOf(cellPos, faceOrdinal);
                if (!resident(level, neighbour)) return LevelNodeGraph.Probe.ABSENT;
                if (handlerAt(level, neighbour, faceOrdinal) != null) return LevelNodeGraph.Probe.PRESENT;
                return foreignAt(level, neighbour, faceOrdinal) == null
                        ? LevelNodeGraph.Probe.ABSENT : LevelNodeGraph.Probe.PRESENT;
            };

    private PowerNetwork() {}

    public static void init() {
        EndpointRegistry.init();
        EnergyExposure.expose(PowerEnergySource.INSTANCE,
                () -> new Block[]{
                        ModMachineBlocks.red_cable.get()});
        ChunkPromotions.onRise(LevelNodeGraph::onChunkFull);
        Lifecycle.onLevelUnload((server, level) -> {
            LevelNodeGraph.clearLevel(level);
            EndpointRegistry.clearLevel(level);
            PortClaims.clearLevel(level);
        });
        Lifecycle.onServerTick(PowerNetwork::onServerTick);
    }

    /** The FE face of the net a conductor belongs to, minted on demand. */
    public static @Nullable ForeignEnergyBridge bridgeAt(ServerLevel level, BlockPos conductorPos) {
        NodeNetwork<CableData> net = PowerGraph.get(level).networkAt(conductorPos.asLong());
        return net == null ? null : BRIDGES.computeIfAbsent(net, k -> new ForeignEnergyBridge());
    }

    /**
     * A merge discards the drained net's object and its bridge; the pools it carries are position-blind,
     * so adding them onto the survivor's is exact conservation.
     */
    public static void mergeBridges(NodeNetwork<CableData> keep, NodeNetwork<CableData> drain) {
        if (BRIDGES.isEmpty()) return;
        ForeignEnergyBridge drained = BRIDGES.remove(drain);
        if (drained == null) return;
        BRIDGES.computeIfAbsent(keep, k -> new ForeignEnergyBridge()).absorb(drained);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            LevelNodeGraph<CableData> graph = PowerGraph.get(level);
            if (graph.nodeCount() == 0) continue;
            graph.applyCompiledSwaps();
            for (NodeNetwork<CableData> net : graph.networks()) distribute(level, graph, net);
        }
        // A net emptied by the last cable coming out takes its bridge with it. A merge does not come
        // through here: the graph tells the provider, which folds the drained pool into the survivor.
        if (!BRIDGES.isEmpty()) BRIDGES.keySet().removeIf(net -> net.size() == 0);
    }

    /**
     * One net's round, under a re-entrancy brake whenever it has a foreign face: a push can reach a peer
     * whose own handler calls straight back into the capability we expose.
     */
    public static void distribute(ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net) {
        ForeignEnergyBridge bridge = BRIDGES.isEmpty() ? null : BRIDGES.get(net);
        if (bridge == null) {
            round(level, graph, net, null);
            return;
        }
        if (!bridge.beginDistribute()) return;
        try {
            round(level, graph, net, bridge);
        } finally {
            bridge.endDistribute();
        }
    }

    private static void round(ServerLevel level, LevelNodeGraph<CableData> graph, NodeNetwork<CableData> net,
                              @Nullable ForeignEnergyBridge bridge) {
        net.tracker = 0L;
        long gameTime = level.getGameTime();
        ReferenceOpenHashSet<GraphNode<CableData>> faces = graph.drainProbes(net, level, FACE_PROBE);

        Gather gather = new Gather();
        for (GraphNode<CableData> node : faces) {
            EdgeFaces ef = node.ef;
            if (ef == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                if ((ef.activeMask & (1 << d)) == 0) continue;
                BlockPos at = neighbourOf(node.posKey, d);
                if (!resident(level, at)) continue;
                BlockEntity be = handlerAt(level, at, d);
                if (be instanceof IEnergyHandlerMK2 handler) {
                    if (handler.isLoaded()) {
                        // 1.7: a role only through a port the machine claimed it at (tryProvide / trySubscribe),
                        // each role once per round whichever of its faces carries it; see PortClaims.
                        ForgeDirection side = d == EdgeFaces.SELF ? ForgeDirection.UNKNOWN
                                : ForgeDirection.getOrientation(DIRS[d].getOpposite());
                        // asked only with something to move: PortClaims learns a class from what it does then
                        boolean provide = handler instanceof IEnergyProviderMK2
                                && !gather.seenProviding.contains(handler)
                                && Math.min(handler.getPower(), handler.getProviderSpeed()) > 0L
                                && PortClaims.allows(level, node.posKey, side, handler, PortClaims.POWER, PortClaims.PROVIDE)
                                && gather.seenProviding.add(handler);
                        boolean receive = handler instanceof IEnergyReceiverMK2
                                && !gather.seenReceiving.contains(handler)
                                && Math.min(handler.getMaxPower() - handler.getPower(), handler.getReceiverSpeed()) > 0L
                                && PortClaims.allows(level, node.posKey, side, handler, PortClaims.POWER, PortClaims.SUBSCRIBE)
                                && gather.seenReceiving.add(handler);
                        if (provide || receive) gather.offer(handler, provide, receive);
                    }
                    continue;
                }
                EnergyView view = foreignAt(level, at, d);
                if (view == null) {
                    graph.markFaceStale(net, node, ef, d);
                    continue;
                }
                if (!gather.seenForeign.add(view.handlerIdentity())) continue;
                ForeignEnergyEndpoint peer = peerAt(ef, d, view);
                peer.refresh();
                gather.offerForeign(peer, bridge != null && bridge.latched(at.asLong(), gameTime));
            }
        }

        long injected = bridge == null ? 0L : bridge.injected();
        if (injected > 0L) gather.offer(bridge.injectedSource(), true, false);
        gather.fileForeignDemand();

        long supply = gather.totalSupply();
        long delivered = 0L;
        long toTransfer = Math.min(supply, gather.totalDemand);
        if (toTransfer > 0L) {
            delivered = toTransfer >= gather.totalDemand
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
        long spent = owed;
        if (bridge != null) owed += bridge.drainOwed();
        long unpaid = settle(gather, owed, net);
        if (bridge != null) {
            bridge.carryOwed(unpaid);
            // What a peer may take is what this round did not spend; what it may give is what our own
            // machines asked for and did not get. Foreign receivers are left out of the second number
            // on purpose: advertising a re-export appetite is how two nets circulate energy forever.
            long toNative = Math.min(delivered, gather.nativeDemand);
            bridge.publish(Math.max(0L, gather.totalSupply() - spent),
                    Math.max(0L, gather.nativeDemand - toNative), gameTime);
        }
    }

    /**
     * Charge the round back. Real providers pay first and buffers only cover the shortfall, lowest
     * tier first. A buffer's priority reads "how full I want this one", so the overflow tank empties
     * before the main bank.
     *
     * @return what no provider could cover, which a foreign debt must keep rather than write off.
     */
    private static long settle(Gather gather, long owed, NodeNetwork<CableData> net) {
        if (owed <= 0L) return 0L;
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
            long tierSupply = gather.bufferSupply[tier];
            if (tierSupply <= 0L) continue;
            long before = charged[0];
            Distribution.debit(gather.bufferProviders[tier], Math.min(remaining, tierSupply), tierSupply,
                    net.providerCursors, 1 + tier, apply);
            remaining -= charged[0] - before;
        }
        return Math.max(0L, remaining);
    }

    private static long deliver(IEnergyHandlerMK2 receiver, long amount) {
        return amount - receiver.transferPower(amount, false);
    }

    /** The foreign endpoint behind a face, kept on the face so its sub-unit remainder survives the round. */
    private static ForeignEnergyEndpoint peerAt(EdgeFaces ef, int faceOrdinal, EnergyView view) {
        Object held = ef.attachments[faceOrdinal];
        if (held instanceof ForeignEnergyEndpoint peer && peer.looksAt(view)) return peer;
        ForeignEnergyEndpoint peer = new ForeignEnergyEndpoint(view);
        ef.attachments[faceOrdinal] = peer;
        return peer;
    }

    private static @Nullable EnergyView foreignAt(ServerLevel level, BlockPos pos, int faceOrdinal) {
        if (!EnergyCompat.supported() || faceOrdinal == EdgeFaces.SELF) return null;
        return EnergyCompat.find(level, pos, DIRS[faceOrdinal].getOpposite());
    }

    /**
     * The machine a face looks at, through three routes in order of cost. The endpoint registry
     * answers without touching the chunk; a block entity sitting on the cell answers for itself; and a
     * footprint cell answers through the block, which is the only one of the three that knows where the
     * core is. {@link MachineCore} keeps that honest for both multiblock systems. A machine's own
     * {@code canConnect} gates the side, so a face it refuses is a face the net cannot reach it on. The
     * pseudo-face is exempt, being the cell's own machine rather than a side of it.
     */
    /**
     * Is the chunk holding this face already resident? Probing runs from the server tick over every face of
     * every net, and both {@code getBlockEntity} and {@code getBlockState} load an absent chunk synchronously,
     * so a net whose run reaches into unloaded ground would make the tick load chunks, widening the loaded
     * set and giving the next tick more faces to probe. A face that is not resident is skipped rather than
     * marked stale, so it is probed again for free once something else loads it.
     */
    private static boolean resident(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static @Nullable BlockEntity handlerAt(ServerLevel level, BlockPos pos, int faceOrdinal) {
        Direction face = faceOrdinal == EdgeFaces.SELF ? null : DIRS[faceOrdinal].getOpposite();
        BlockEntity declared = EndpointRegistry.of(level).resolve(level, pos.asLong(), face, POWER);
        if (declared instanceof IEnergyHandlerMK2) return declared;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IEnergyHandlerMK2 handler) {
            return face == null || handler.canConnect(ForgeDirection.getOrientation(face)) ? be : null;
        }
        if (face == null) return null;

        BlockState state = level.getBlockState(pos);
        ForgeDirection side = ForgeDirection.getOrientation(face);
        if (!(state.getBlock() instanceof IEnergyConnectorBlock connector)
                || !connector.canConnect(level, pos, side)) {
            return null;
        }
        BlockEntity core = MachineCore.at(level, pos, state);
        return core instanceof IEnergyHandlerMK2 ? core : null;
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
        final ReferenceOpenHashSet<IEnergyHandlerMK2> seenProviding = new ReferenceOpenHashSet<>();
        final ReferenceOpenHashSet<IEnergyHandlerMK2> seenReceiving = new ReferenceOpenHashSet<>();
        final ReferenceOpenHashSet<Object> seenForeign = new ReferenceOpenHashSet<>();
        final long[] demand = new long[PRIORITIES];
        final long[] bufferHeadroom = new long[PRIORITIES];
        final long[] bufferSupply = new long[PRIORITIES];
        long powerAvailable;
        long bufferPower;
        long totalDemand;
        long nativeDemand;
        long bufferDemand;
        @Nullable List<Distribution.Share<IEnergyHandlerMK2>> foreignDemand;

        Gather() {
            for (int i = 0; i < PRIORITIES; i++) {
                receivers[i] = new ArrayList<>();
                bufferReceivers[i] = new ArrayList<>();
                bufferProviders[i] = new ArrayList<>();
            }
            Arrays.fill(demand, 0L);
        }

        void offer(IEnergyHandlerMK2 handler, boolean provide, boolean receive) {
            int tier = handler.getPriority().ordinal();
            boolean buffer = handler.bufferedEndpoint();

            // 1.7 PowerNetMK2 kept providers and receivers in two lists, filled only by IEnergyProviderMK2.tryProvide
            // and IEnergyReceiverMK2.trySubscribe: a consumer's stored power is never supply, and a generator's
            // headroom is never demand, whatever its speeds say: a battery must not drain a machine.
            long supply = provide && handler instanceof IEnergyProviderMK2
                    ? Math.min(handler.getPower(), handler.getProviderSpeed()) : 0L;
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

            long headroom = receive && handler instanceof IEnergyReceiverMK2
                    ? Math.min(handler.getMaxPower() - handler.getPower(), handler.getReceiverSpeed()) : 0L;
            if (headroom > 0L) {
                if (buffer) {
                    bufferReceivers[tier].add(new Distribution.Share<>(handler, headroom));
                    bufferHeadroom[tier] += headroom;
                    bufferDemand += headroom;
                } else {
                    receivers[tier].add(new Distribution.Share<>(handler, headroom));
                    demand[tier] += headroom;
                    totalDemand += headroom;
                    nativeDemand += headroom;
                }
            }
        }

        /**
         * A foreign peer is gathered apart from our own machines: as a provider it joins the untiered
         * pool at once, but as a receiver it waits for {@link #fileForeignDemand}, which is where the
         * export reserve decides how much of it competes with our machines rather than following them.
         */
        void offerForeign(ForeignEnergyEndpoint peer, boolean latched) {
            if (peer.servesAsProvider()) {
                long supply = Math.min(peer.getPower(), peer.getProviderSpeed());
                providers.add(new Distribution.Share<>(peer, supply));
                powerAvailable += supply;
            }
            if (peer.servesAsReceiver() && !latched) {
                if (foreignDemand == null) foreignDemand = new ArrayList<>();
                foreignDemand.add(new Distribution.Share<>(peer, peer.getReceiverSpeed()));
            }
        }

        void fileForeignDemand() {
            if (foreignDemand == null) return;
            totalDemand += ForeignEnergyEndpoint.fileForeignDemand(foreignDemand, powerAvailable + bufferPower,
                    GeneralConfig.foreignExportReserve, nativeDemand, receivers, demand);
        }

        long totalSupply() {
            return powerAvailable + bufferPower;
        }
    }
}
