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
import dev.golem.core.platform.Lifecycle;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One round of fluid over the node graph: find the machines at the net's edge faces, serve them by
 * {@linkplain FluidBus the same law the network always used}, and charge the round back to whoever
 * funded it. The energy side's {@code PowerNetwork} is the sibling to read this against — the two
 * differ only where fluid genuinely does.
 *
 * <p>They differ in two places. A net is keyed to <b>one fluid</b>: UNINOS gave every
 * {@link FluidType} a nodespace of its own, and the graph gets the same partition out of
 * {@link FluidGraphProvider#dataCompatible} instead, so one save holds the whole level's plumbing and
 * a steam duct still never joins an oil duct. And a round is run <b>per pressure band</b>, 0 through
 * {@link IFluidUserMK2#HIGHEST_VALID_PRESSURE}, because a machine advertises a range of them and a
 * provider at one pressure cannot serve a receiver at another.
 *
 * <p>What does not differ is discovery. It is entirely pull: a duct probes the faces of its own cells
 * and asks what is on the other side. Nothing subscribes, and there is no timeout to evict what stopped
 * subscribing, because nothing ever did.
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
                if (data == null || data.typeId() == Fluids.NONE.getID()) return LevelNodeGraph.Probe.ABSENT;
                BlockPos neighbour = neighbourOf(cellPos, faceOrdinal);
                return userAt(level, neighbour, faceOrdinal, data.type()) == null
                        ? LevelNodeGraph.Probe.ABSENT : LevelNodeGraph.Probe.PRESENT;
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

    /** Every net in one level, once. */
    public static void distributeAll(ServerLevel level) {
        LevelNodeGraph<DuctData> graph = FluidGraph.get(level);
        if (graph.nodeCount() == 0) return;
        graph.applyCompiledSwaps();
        for (NodeNetwork<DuctData> net : graph.networks()) distribute(level, graph, net);
    }

    public static void distribute(ServerLevel level, LevelNodeGraph<DuctData> graph, NodeNetwork<DuctData> net) {
        FluidType type = typeOf(net);
        if (type == null || type == Fluids.NONE) return;
        round(level, graph, net, type);
    }

    private static void round(ServerLevel level, LevelNodeGraph<DuctData> graph, NodeNetwork<DuctData> net,
                              FluidType type) {
        net.tracker = 0L;
        ReferenceOpenHashSet<GraphNode<DuctData>> faces = graph.drainProbes(net, level, FACE_PROBE);

        Gather gather = gatherOf(net);
        gather.clear();
        for (GraphNode<DuctData> node : faces) {
            EdgeFaces ef = node.ef;
            if (ef == null) continue;
            for (int d = 0; d <= EdgeFaces.SELF; d++) {
                if ((ef.activeMask & (1 << d)) == 0) continue;
                BlockPos at = neighbourOf(node.posKey, d);
                BlockEntity be = userAt(level, at, d, type);
                if (be == null) {
                    graph.markFaceStale(net, node, ef, d);
                    continue;
                }
                gather.offer((IFluidUserMK2) be, type);
            }
        }

        if (net.receiverCursors == null) net.receiverCursors = new int[PRESSURES * PRIORITIES];
        if (net.providerCursors == null) net.providerCursors = new int[PRESSURES];
        for (int p = 0; p < PRESSURES; p++) {
            if (gather.supply[p] <= 0L || gather.providers[p].isEmpty()) continue;
            net.tracker += FluidBus.transfer(type, p, gather.supply[p], gather.providers[p], gather.receivers[p],
                    gather.demand[p], net.receiverCursors, p * PRIORITIES, net.providerCursors, p);
        }
    }

    /**
     * The machine a face looks at, through the same three routes the energy net uses: the endpoint
     * registry answers without touching the chunk, a block entity sitting on the cell answers for
     * itself, and a folded footprint cell answers through the block, which is the only one of the three
     * that knows where the core is. A machine's own {@code canConnect} gates both the side and the
     * fluid, so a duct carrying the wrong fluid is refused here rather than at delivery.
     */
    private static @Nullable BlockEntity userAt(ServerLevel level, BlockPos pos, int faceOrdinal, FluidType type) {
        Direction face = faceOrdinal == EdgeFaces.SELF ? null : DIRS[faceOrdinal].getOpposite();
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

    /** Every cell of a net carries the same fluid, so the first member answers for all of them. */
    private static @Nullable FluidType typeOf(NodeNetwork<DuctData> net) {
        for (GraphNode<DuctData> node : net.nodes) return node.data.type();
        for (GraphSegment<DuctData> segment : net.segments) return segment.data.type();
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Gather gatherOf(NodeNetwork<DuctData> net) {
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
        final ReferenceOpenHashSet<IFluidUserMK2> seen = new ReferenceOpenHashSet<>();

        Gather() {
            for (int p = 0; p < PRESSURES; p++) {
                providers[p] = new ArrayList<>();
                for (int t = 0; t < PRIORITIES; t++) receivers[p][t] = new ArrayList<>();
            }
        }

        void offer(IFluidUserMK2 user, FluidType type) {
            if (!user.isLoaded() || !seen.add(user)) return;

            if (user instanceof IFluidProviderMK2 provider) {
                int[] range = provider.getProvidingPressureRange(type);
                for (int p = Math.max(0, range[0]); p <= Math.min(range[1], PRESSURES - 1); p++) {
                    long available = Math.min(provider.getFluidAvailable(type, p), provider.getProviderSpeed(type, p));
                    if (available <= 0L) continue;
                    providers[p].add(new Distribution.Share<>(provider, available));
                    supply[p] += available;
                }
            }

            if (user instanceof IFluidReceiverMK2 receiver) {
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

        void clear() {
            seen.clear();
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
