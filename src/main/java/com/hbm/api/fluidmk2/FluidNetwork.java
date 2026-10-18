package com.hbm.api.fluidmk2;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.tileentity.MachineCore;
import com.hbm.capability.MachineCaps;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.Distribution;
import com.hbm.uninos.graph.EdgeFaces;
import com.hbm.uninos.graph.EndpointRegistry;
import com.hbm.uninos.graph.GraphNode;
import com.hbm.uninos.graph.GraphSegment;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.PortNet;
import com.hbm.uninos.PortNode;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.networkproviders.FluidNetProvider;
import dev.golem.core.platform.Lifecycle;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * One round of fluid over the node graph: find the machines at the net's edge faces, serve them by
 * {@linkplain FluidBus the same law the network always used}, and charge the round back to whoever
 * funded it. The energy side's {@code PowerNetwork} is the sibling to read this against; the two
 * differ only where fluid does.
 *
 * <p>They differ in two places. A net is keyed to <b>one fluid</b>: UNINOS gave every
 * {@link FluidType} a nodespace of its own, and the graph gets the same partition from
 * {@link FluidGraphProvider#dataCompatible}, so one save holds the whole level's plumbing and
 * a steam duct never joins an oil duct. And a round is run <b>per pressure band</b>, 0 through
 * {@link IFluidUserMK2#HIGHEST_VALID_PRESSURE}, because a provider at one pressure cannot serve a
 * receiver at another.
 *
 * <p>What does not differ is discovery. It is pull: a duct probes the faces of its own cells and asks what
 * is on the other side. A machine with fluid ports is left to them ({@link PortNodes}).
 */
public final class FluidNetwork {

    private static final Direction[] DIRS = Direction.values();
    private static final int PRIORITIES = IEnergyHandlerMK2.ConnectionPriority.VALUES.length;
    private static final int PRESSURES = IFluidUserMK2.HIGHEST_VALID_PRESSURE + 1;
    private static final int FLUID = MachineCaps.FLUID_IN | MachineCaps.FLUID_OUT;

    /**
     * A face answers PRESENT on anything that could ever be an endpoint for this duct's fluid, not on
     * what it holds this tick: a tank that is full now is still a tank, and answering ABSENT would park
     * the face until something invalidated it.
     */
    private static final LevelNodeGraph.FaceProbe<DuctData> FACE_PROBE =
            (level, graph, cellPos, faces, faceOrdinal) -> {
                DuctData data = graph.dataAt(cellPos);
                if (data == null || data.isNone()) return LevelNodeGraph.Probe.ABSENT;
                BlockPos neighbour = neighbourOf(cellPos, faceOrdinal);
                if (!resident(level, neighbour)) return LevelNodeGraph.Probe.ABSENT;
                // An exhaust cell carries every smoke, so its face is present for a machine taking any one of them.
                for (FluidType type : data.types()) {
                    if (userAt(level, neighbour, faceOrdinal, type) != null) return LevelNodeGraph.Probe.PRESENT;
                }
                return LevelNodeGraph.Probe.ABSENT;
            };

    private FluidNetwork() {}

    /**
     * The chunk and level hooks the graph needs are registered once by
     * {@link com.hbm.api.energymk2.PowerNetwork#init()} and cover every graph in the level, so this
     * adds only its own round.
     */
    public static void init() {
        Lifecycle.onServerTick(FluidNetwork::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) distributeAll(level);
    }

    /** Every net in one level, once: per fluid, the duct nets joined with the machine ports that reach them. */
    public static void distributeAll(ServerLevel level) {
        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        Collection<INetworkProvider> portTypes = PortNodes.types(level);
        if (graph.nodeCount() == 0 && portTypes.isEmpty()) return;
        graph.applyCompiledSwaps();
        Reference2ObjectLinkedOpenHashMap<FluidType, List<NodeNetwork<DuctData>>> byType = new Reference2ObjectLinkedOpenHashMap<>();
        for (NodeNetwork<DuctData> net : graph.networks()) {
            net.tracker = 0L;
            for (FluidType type : typesOf(net)) byType.computeIfAbsent(type, k -> new ArrayList<>()).add(net);
        }
        for (INetworkProvider type : portTypes) {
            if (type instanceof FluidNetProvider fluid) byType.computeIfAbsent(fluid.type, k -> new ArrayList<>());
        }
        Reference2ObjectOpenHashMap<NodeNetwork<DuctData>, ReferenceOpenHashSet<GraphNode<DuctData>>> faces = new Reference2ObjectOpenHashMap<>();
        byType.forEach((type, nets) -> {
            for (PortNet<DuctData> net : PortNodes.roundNets(level, graph, () -> nets, FluidNetProvider.of(type), data -> data.carries(type))) {
                round(level, graph, net, type, faces);
            }
        });
    }

    /**
     * One duct net's rounds on their own: one per fluid it carries, each with the ports that reach it. A net holding an
     * exhaust cell ({@link DuctData#SMOKE_ANY}) is one round per smoke over the same faces, which is what 1.7 got from
     * the exhaust sitting in three nodespaces at once; the net's tracker sums the three.
     */
    public static void distribute(ServerLevel level, LevelNodeGraph<DuctData> graph, NodeNetwork<DuctData> net) {
        FluidType[] types = typesOf(net);
        if (types.length == 0) return;
        net.tracker = 0L;
        Reference2ObjectOpenHashMap<NodeNetwork<DuctData>, ReferenceOpenHashSet<GraphNode<DuctData>>> faces = new Reference2ObjectOpenHashMap<>();
        for (FluidType type : types) {
            for (PortNet<DuctData> portNet : PortNodes.nets(level, graph, List.of(net), FluidNetProvider.of(type), data -> data.carries(type))) {
                if (portNet.nets.contains(net)) round(level, graph, portNet, type, faces);
            }
        }
    }

    private static void round(ServerLevel level, LevelNodeGraph<DuctData> graph, PortNet<DuctData> net, FluidType type,
                              Reference2ObjectOpenHashMap<NodeNetwork<DuctData>, ReferenceOpenHashSet<GraphNode<DuctData>>> faceCache) {
        Gather gather = net.nets.isEmpty() ? gatherOf(net) : gatherOf(net.nets.get(0));
        gather.clear();
        for (NodeNetwork<DuctData> ducts : net.nets) {
            FluidType[] carried = typesOf(ducts);
            ReferenceOpenHashSet<GraphNode<DuctData>> faces = faceCache.get(ducts);
            if (faces == null) faceCache.put(ducts, faces = graph.drainProbes(ducts, level, FACE_PROBE));
            for (GraphNode<DuctData> node : faces) {
                EdgeFaces ef = node.ef;
                if (ef == null) continue;
                for (int d = 0; d <= EdgeFaces.SELF; d++) {
                    if ((ef.activeMask & (1 << d)) == 0) continue;
                    BlockPos at = neighbourOf(node.posKey, d);
                    if (!resident(level, at)) continue;
                    BlockEntity be = userAt(level, at, d, type);
                    if (be == null) {
                        // A face on a smoke net is present for ANY of its smokes: a chimney taking only SMOKE is
                        // no reason to re-probe the face on the SMOKE_LEADED pass. Stale only when no carried fluid finds it.
                        if (carried.length == 1 || userAtAny(level, at, d, carried) == null) graph.markFaceStale(ducts, node, ef, d);
                        continue;
                    }
                    ForgeDirection side = d == EdgeFaces.SELF ? ForgeDirection.UNKNOWN
                            : ForgeDirection.getOrientation(DIRS[d].getOpposite());
                    // a machine with several users (IFluidMultiUserMK2) is served once per user its faces reach
                    IFluidUserMK2 user = IFluidMultiUserMK2.resolve(be, at, d == EdgeFaces.SELF ? null : DIRS[d].getOpposite());
                    // a user with fluid ports is connected by them alone
                    if (user == null || PortNodes.isPorted(level, user, false)) continue;
                    gather.offer(net, user, type, side);
                }
            }
        }
        for (PortNode port : net.ports) {
            for (Object owner : port.providers) gather.offerPort(net, owner, type, true);
            for (Object owner : port.receivers) gather.offerPort(net, owner, type, false);
        }

        // 1.7 FluidNetMK2: the tracker is what the providers held before the round less what they hold after it
        long before = 0L;
        for (Object entry : net.providerEntries) {
            IFluidProviderMK2 provider = (IFluidProviderMK2) entry;
            int[] range = provider.getProvidingPressureRange(type);
            for (int p = range[0]; p <= range[1]; p++) before += provider.getFluidAvailable(type, p);
        }

        int[] receiverCursors = net.receiverCursors(PRESSURES * PRIORITIES);
        int[] providerCursors = net.providerCursors(PRESSURES);
        for (int p = 0; p < PRESSURES; p++) {
            if (gather.supply[p] <= 0L || gather.providers[p].isEmpty()) continue;
            FluidBus.transfer(type, p, gather.supply[p], gather.providers[p], gather.receivers[p],
                    gather.demand[p], receiverCursors, p * PRIORITIES, providerCursors, p);
        }

        long after = 0L;
        for (Object entry : net.providerEntries) {
            for (int p = 0; p < PRESSURES; p++) after += ((IFluidProviderMK2) entry).getFluidAvailable(type, p);
        }
        net.tracker = net.providerEntries.isEmpty() || net.receiverEntries.isEmpty() ? 0L : before - after;
        for (NodeNetwork<DuctData> ducts : net.nets) {
            ducts.tracker += net.tracker;
            ducts.portNet = net;
        }
    }

    /**
     * Is the chunk holding this face already resident? See {@code PowerNetwork.resident}: probing runs from
     * the server tick, and the block lookups below load a chunk synchronously when it is absent.
     */
    private static boolean resident(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /**
     * The machine a face looks at, through the same three routes the energy net uses: the endpoint
     * registry answers without touching the chunk, a block entity sitting on the cell answers for
     * itself, and a folded footprint cell answers through the block, which alone knows where the core is.
     * A machine's own {@code canConnect} gates both the side and the fluid, so a duct carrying the wrong
     * fluid is refused here rather than at delivery.
     */
    private static @Nullable BlockEntity userAt(ServerLevel level, BlockPos pos, int faceOrdinal, FluidType type) {
        return userAtFace(level, pos, faceOrdinal == EdgeFaces.SELF ? null : DIRS[faceOrdinal].getOpposite(), type);
    }

    /**
     * The fluid user a duct touching {@code pos} through its {@code face} side would find for {@code type}, or null
     * ({@code face} null: the cell's own machine). Public so the loader fluid capabilities
     * ({@link com.hbm.capability.ForeignFluids}) open exactly the faces a duct connects to, never a second rule.
     */
    public static @Nullable BlockEntity userAtFace(ServerLevel level, BlockPos pos, @Nullable Direction face, FluidType type) {
        BlockEntity declared = EndpointRegistry.of(level).resolve(level, pos.asLong(), face, FLUID);
        if (accepts(declared, type, face)) return declared;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IFluidUserMK2) return accepts(be, type, face) ? be : null;
        if (face == null) return null;

        BlockState state = level.getBlockState(pos);
        ForgeDirection side = ForgeDirection.getOrientation(face);
        if (!(state.getBlock() instanceof IFluidConnectorBlockMK2 connector)
                || !connector.canConnect(type, level, pos.getX(), pos.getY(), pos.getZ(), side)) {
            return null;
        }
        BlockEntity core = MachineCore.at(level, pos, state);
        return core instanceof IFluidUserMK2 ? core : null;
    }

    /**
     * The pseudo-face is exempt from the side check, being the cell's own machine rather than a side of
     * it; a duct is never an endpoint of its own net.
     */
    private static boolean accepts(@Nullable BlockEntity be, FluidType type, @Nullable Direction face) {
        if (!(be instanceof IFluidUserMK2 user) || be instanceof IFluidPipeMK2) return false;
        return face == null || user.canConnect(type, ForgeDirection.getOrientation(face));
    }

    private static @Nullable BlockEntity userAtAny(ServerLevel level, BlockPos pos, int faceOrdinal, FluidType[] types) {
        for (FluidType type : types) {
            BlockEntity be = userAt(level, pos, faceOrdinal, type);
            if (be != null) return be;
        }
        return null;
    }

    private static final FluidType[] NO_TYPES = new FluidType[0];

    /**
     * The fluids a net rounds: its one fluid, or the three smokes when any member is an exhaust cell.
     * Every typed cell of a net carries the same fluid (that is what {@link FluidGraphProvider#dataCompatible}
     * partitions on), but an exhaust cell is compatible with each smoke, so the whole membership has to be
     * looked at once; the answer is cached on the net's {@code attachment}, which the graph nulls on every
     * membership edit, so a surviving value is current by construction.
     */
    private static FluidType[] typesOf(NodeNetwork<DuctData> net) {
        if (net.attachment instanceof FluidType[] cached) return cached;
        FluidType[] types = NO_TYPES;
        DuctData first = null;
        for (GraphNode<DuctData> node : net.nodes) {
            if (node.data.isSmokeAny()) { types = node.data.types(); break; }
            if (first == null) first = node.data;
        }
        if (types == NO_TYPES) {
            for (GraphSegment<DuctData> segment : net.segments) {
                if (segment.data.isSmokeAny()) { types = segment.data.types(); break; }
                if (first == null) first = segment.data;
            }
        }
        if (types == NO_TYPES && first != null && !first.isNone()) types = first.types();
        net.attachment = types;
        return types;
    }

    @SuppressWarnings("unchecked")
    private static Gather gatherOf(NodeNetwork<DuctData> net) {
        if (net.fluidGather == null) net.fluidGather = new Gather();
        return (Gather) net.fluidGather;
    }

    private static Gather gatherOf(PortNet<DuctData> net) {
        if (net.fluidGather == null) net.fluidGather = new Gather();
        return (Gather) net.fluidGather;
    }

    private static BlockPos neighbourOf(long cellPos, int faceOrdinal) {
        BlockPos cell = BlockPos.of(cellPos);
        return faceOrdinal == EdgeFaces.SELF ? cell : cell.relative(DIRS[faceOrdinal]);
    }

    /**
     * One round's endpoints, split by pressure band and then by priority tier. Held on the net and
     * cleared rather than reallocated, so a steady-state tick allocates nothing.
     */
    private static final class Gather {

        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IFluidProviderMK2>>[] providers = new List[PRESSURES];
        @SuppressWarnings("unchecked")
        final List<Distribution.Share<IFluidReceiverMK2>>[][] receivers = new List[PRESSURES][PRIORITIES];
        final long[] supply = new long[PRESSURES];
        final long[][] demand = new long[PRESSURES][PRIORITIES];
        /** Users are deduplicated per role, so a machine whose intake and outlet face the same net keeps both. */
        final ReferenceOpenHashSet<IFluidUserMK2> seenProviding = new ReferenceOpenHashSet<>();
        final ReferenceOpenHashSet<IFluidUserMK2> seenReceiving = new ReferenceOpenHashSet<>();

        Gather() {
            for (int p = 0; p < PRESSURES; p++) {
                providers[p] = new ArrayList<>();
                for (int t = 0; t < PRIORITIES; t++) receivers[p][t] = new ArrayList<>();
            }
        }

        /** One face's user: each role once per round, by whichever of the user's faces first carries it. */
        void offer(PortNet<?> net, IFluidUserMK2 user, FluidType type, ForgeDirection side) {
            if (!user.isLoaded()) return;
            // A directional user (the flow-control pump) carries one role per face; see IFluidDirectionalMK2.
            IFluidDirectionalMK2 directional = user instanceof IFluidDirectionalMK2 d ? d : null;

            if (user instanceof IFluidProviderMK2 provider
                    && (directional == null || directional.providesOn(side))
                    && !seenProviding.contains(user)
                    && supplies(provider, type)) {
                seenProviding.add(user);
                net.providerEntries.add(user);
                int[] range = provider.getProvidingPressureRange(type);
                for (int p = Math.max(0, range[0]); p <= Math.min(range[1], PRESSURES - 1); p++) {
                    long available = Math.min(provider.getFluidAvailable(type, p), provider.getProviderSpeed(type, p));
                    if (available <= 0L) continue;
                    providers[p].add(new Distribution.Share<>(provider, available));
                    supply[p] += available;
                }
            }

            if (user instanceof IFluidReceiverMK2 receiver
                    && (directional == null || directional.receivesOn(side))
                    && !seenReceiving.contains(user)
                    && demands(receiver, type)) {
                seenReceiving.add(user);
                net.receiverEntries.add(user);
                int[] range = receiver.getReceivingPressureRange(type);
                int tier = receiver.getFluidPriority().ordinal();
                for (int p = Math.max(0, range[0]); p <= Math.min(range[1], PRESSURES - 1); p++) {
                    long required = Math.min(receiver.getDemand(type, p), receiver.getReceiverSpeed(type, p));
                    if (required <= 0L) continue;
                    receivers[p][tier].add(new Distribution.Share<>(receiver, required));
                    demand[p][tier] += required;
                }
            }
        }

        /** A port's role holder, each role once per round, as 1.7's {@code NodeNet} entry sets were. */
        void offerPort(PortNet<?> net, Object owner, FluidType type, boolean provide) {
            if (!(owner instanceof IFluidUserMK2 user) || !user.isLoaded()) return;
            if (provide) {
                if (!(user instanceof IFluidProviderMK2 provider) || !net.providerEntries.add(user) || !seenProviding.add(user)) return;
                int[] range = provider.getProvidingPressureRange(type);
                for (int p = Math.max(0, range[0]); p <= Math.min(range[1], PRESSURES - 1); p++) {
                    long available = Math.min(provider.getFluidAvailable(type, p), provider.getProviderSpeed(type, p));
                    if (available <= 0L) continue;
                    providers[p].add(new Distribution.Share<>(provider, available));
                    supply[p] += available;
                }
            } else {
                if (!(user instanceof IFluidReceiverMK2 receiver) || !net.receiverEntries.add(user) || !seenReceiving.add(user)) return;
                int[] range = receiver.getReceivingPressureRange(type);
                int tier = receiver.getFluidPriority().ordinal();
                for (int p = Math.max(0, range[0]); p <= Math.min(range[1], PRESSURES - 1); p++) {
                    long required = Math.min(receiver.getDemand(type, p), receiver.getReceiverSpeed(type, p));
                    if (required <= 0L) continue;
                    receivers[p][tier].add(new Distribution.Share<>(receiver, required));
                    demand[p][tier] += required;
                }
            }
        }

        private static boolean supplies(IFluidProviderMK2 provider, FluidType type) {
            int[] range = provider.getProvidingPressureRange(type);
            for (int p = Math.max(0, range[0]); p <= Math.min(range[1], PRESSURES - 1); p++) {
                if (Math.min(provider.getFluidAvailable(type, p), provider.getProviderSpeed(type, p)) > 0L) return true;
            }
            return false;
        }

        private static boolean demands(IFluidReceiverMK2 receiver, FluidType type) {
            int[] range = receiver.getReceivingPressureRange(type);
            for (int p = Math.max(0, range[0]); p <= Math.min(range[1], PRESSURES - 1); p++) {
                if (Math.min(receiver.getDemand(type, p), receiver.getReceiverSpeed(type, p)) > 0L) return true;
            }
            return false;
        }

        void clear() {
            seenProviding.clear();
            seenReceiving.clear();
            for (int p = 0; p < PRESSURES; p++) {
                providers[p].clear();
                supply[p] = 0L;
                for (int t = 0; t < PRIORITIES; t++) {
                    receivers[p][t].clear();
                    demand[p][t] = 0L;
                }
            }
        }
    }
}
