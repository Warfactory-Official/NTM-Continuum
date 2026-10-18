package com.hbm.uninos;

import com.hbm.lib.ForgeDirection;
import com.hbm.uninos.graph.LevelNodeGraph;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.uninos.networkproviders.PowerNetProvider;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 1.7 {@code UniNodespace} for machine ports, per level. Ducts and cables live in the level graph, so a round's net
 * is a graph net plus the port nodes whose connections reach it (a duct cell open towards the port), joined through
 * a port that touches several graph nets and through two ports aimed at each other.
 *
 * <p>A machine that owns a port of a kind (power, or any fluid) is never found by a duct probing its faces for that
 * kind: its ports alone connect it, as in 1.7 where the per-tick subscriptions are gone. Server thread only.
 */
public final class PortNodes {

    private static final Reference2ObjectOpenHashMap<Level, PortNodes> LEVELS = new Reference2ObjectOpenHashMap<>();

    private final Reference2ObjectOpenHashMap<INetworkProvider, Long2ObjectOpenHashMap<PortNode>> byPos = new Reference2ObjectOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<INetworkProvider, ReferenceLinkedOpenHashSet<PortNode>> nodes = new Reference2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<ArrayList<PortNode>> byConnection = new Long2ObjectOpenHashMap<>();
    private final Set<Object> portedPower = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<Object> portedFluid = Collections.newSetFromMap(new WeakHashMap<>());
    /** Per kind, bumped whenever one of its nodes is created or destroyed. */
    private final Reference2LongOpenHashMap<INetworkProvider> portVersions = new Reference2LongOpenHashMap<>();
    private final Reference2ObjectOpenHashMap<INetworkProvider, Joined<?>> joined = new Reference2ObjectOpenHashMap<>();

    private PortNodes() {}

    private static PortNodes of(Level level) {
        return LEVELS.computeIfAbsent(level, k -> new PortNodes());
    }

    public static void clearLevel(Level level) {
        LEVELS.remove(level);
    }

    public static @Nullable PortNode getNode(Level level, BlockPos pos, INetworkProvider type) {
        PortNodes space = LEVELS.get(level);
        if (space == null) return null;
        Long2ObjectOpenHashMap<PortNode> map = space.byPos.get(type);
        return map == null ? null : map.get(pos.asLong());
    }

    /** Adds a node at all its positions. */
    public static void createNode(Level level, PortNode node) {
        PortNodes space = of(level);
        Long2ObjectOpenHashMap<PortNode> map = space.byPos.computeIfAbsent(node.type, k -> new Long2ObjectOpenHashMap<>());
        for (long pos : node.positions) map.put(pos, node);
        space.nodes.computeIfAbsent(node.type, k -> new ReferenceLinkedOpenHashSet<>()).add(node);
        for (long cell : node.conCells) space.byConnection.computeIfAbsent(cell, k -> new ArrayList<>(1)).add(node);
        space.portVersions.addTo(node.type, 1L);
    }

    /** Removes a node from all its positions; its roles go with it. */
    public static void destroyNode(Level level, PortNode node) {
        node.expired = true;
        PortNodes space = LEVELS.get(level);
        if (space == null) return;
        space.portVersions.addTo(node.type, 1L);
        Long2ObjectOpenHashMap<PortNode> map = space.byPos.get(node.type);
        if (map != null) for (long pos : node.positions) map.remove(pos, node);
        ReferenceLinkedOpenHashSet<PortNode> set = space.nodes.get(node.type);
        if (set != null) set.remove(node);
        for (long cell : node.conCells) {
            ArrayList<PortNode> list = space.byConnection.get(cell);
            if (list == null) continue;
            list.remove(node);
            if (list.isEmpty()) space.byConnection.remove(cell);
        }
    }

    /** Records that {@code owner} connects through ports for this kind, so no duct finds it by its faces. */
    public static void markPorted(Level level, Object owner, INetworkProvider type) {
        PortNodes space = of(level);
        (type == PowerNetProvider.THE_PROVIDER ? space.portedPower : space.portedFluid).add(owner);
    }

    /** Whether a duct probing a face should leave {@code user} (or the machine it fronts) to its ports. */
    public static boolean isPorted(Level level, Object user, boolean power) {
        PortNodes space = LEVELS.get(level);
        if (space == null) return false;
        Set<Object> set = power ? space.portedPower : space.portedFluid;
        if (set.isEmpty()) return false;
        return set.contains(user) || set.contains(owner(user));
    }

    public static Collection<PortNode> nodes(Level level, INetworkProvider type) {
        PortNodes space = LEVELS.get(level);
        if (space == null) return List.of();
        ReferenceLinkedOpenHashSet<PortNode> set = space.nodes.get(type);
        return set == null ? List.of() : set;
    }

    /** Every kind with at least one live node. */
    public static Collection<INetworkProvider> types(Level level) {
        PortNodes space = LEVELS.get(level);
        if (space == null) return List.of();
        List<INetworkProvider> types = new ArrayList<>();
        space.nodes.forEach((type, set) -> { if (!set.isEmpty()) types.add(type); });
        return types;
    }

    /**
     * Whether something standing at {@code cell}, touching {@code user} on its side {@code side}, is one of its ports
     * for {@code type} in that role. A user without ports of the kind answers by its faces alone.
     */
    public static boolean allows(Level level, long cell, ForgeDirection side, Object user, INetworkProvider type, boolean subscribe) {
        boolean power = type == PowerNetProvider.THE_PROVIDER;
        if (!isPorted(level, user, power)) return true;
        PortNodes space = LEVELS.get(level);
        ArrayList<PortNode> list = space.byConnection.get(cell);
        if (list == null) return false;
        Object owner = owner(user);
        for (PortNode node : list) {
            if (node.type != type || !node.connectsTo(cell, side)) continue;
            if ((subscribe ? node.receivers : node.providers).contains(owner)) return true;
        }
        return false;
    }

    /**
     * {@link #nets} over all of the graph's nets of this kind, joined again only after the graph or this kind's ports
     * changed: the last round's nets come back with their entry sets emptied. {@code graphNets} is asked only then.
     */
    public static <D> List<PortNet<D>> roundNets(Level level, LevelNodeGraph<D> graph, Supplier<? extends Collection<NodeNetwork<D>>> graphNets,
                                              INetworkProvider type, Predicate<D> carries) {
        PortNodes space = of(level);
        long ports = space.portVersions.getLong(type);
        Joined<?> last = space.joined.get(type);
        if (last != null && last.graph == graph && last.graphVersion == graph.version() && last.portVersion == ports) {
            @SuppressWarnings("unchecked")
            List<PortNet<D>> nets = ((Joined<D>) last).nets;
            for (PortNet<D> net : nets) {
                net.receiverEntries.clear();
                net.providerEntries.clear();
                for (PortNode port : net.ports) port.net = net;
            }
            return nets;
        }
        List<PortNet<D>> nets = nets(level, graph, graphNets.get(), type, carries);
        space.joined.put(type, new Joined<>(graph, graph.version(), ports, nets));
        return nets;
    }

    private record Joined<D>(LevelNodeGraph<D> graph, long graphVersion, long portVersion, List<PortNet<D>> nets) {}

    /**
     * The nets of one round for one kind: every graph net in {@code graphNets}, each joined with the ports that reach
     * it and with whatever those ports reach. A cell answers for {@code type} when {@code carries} accepts its datum.
     */
    public static <D> List<PortNet<D>> nets(Level level, LevelNodeGraph<D> graph, Collection<NodeNetwork<D>> graphNets,
                                         INetworkProvider type, Predicate<D> carries) {
        PortNodes space = LEVELS.get(level);
        ReferenceLinkedOpenHashSet<PortNode> ports = space == null ? null : space.nodes.get(type);
        int netCount = graphNets.size();
        int portCount = ports == null ? 0 : ports.size();
        List<PortNet<D>> out = new ArrayList<>(netCount + portCount);
        if (portCount == 0) {
            for (NodeNetwork<D> net : graphNets) {
                PortNet<D> single = new PortNet<>(type);
                single.nets.add(net);
                out.add(single);
            }
            return out;
        }

        List<Object> members = new ArrayList<>(netCount + portCount);
        Reference2IntOpenHashMap<Object> index = new Reference2IntOpenHashMap<>(netCount + portCount);
        index.defaultReturnValue(-1);
        for (NodeNetwork<D> net : graphNets) {
            index.put(net, members.size());
            members.add(net);
        }
        for (PortNode port : ports) {
            index.put(port, members.size());
            members.add(port);
        }
        int[] parent = new int[members.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;

        Long2ObjectOpenHashMap<PortNode> portAt = space.byPos.get(type);
        for (PortNode port : ports) {
            int self = index.getInt(port);
            for (int k = 0; k < port.conCells.length; k++) {
                long cell = port.conCells[k];
                ForgeDirection dir = port.connections[k].getDir();
                if (graph.containsCell(cell)) {
                    D data = graph.dataAt(cell);
                    if (data == null || !carries.test(data) || !graph.isOpenAt(cell, dir.getOpposite().toDirection())) continue;
                    NodeNetwork<D> net = graph.networkAt(cell);
                    int other = net == null ? -1 : index.getInt(net);
                    if (other >= 0) union(parent, self, other);
                    continue;
                }
                PortNode facing = portAt == null ? null : portAt.get(cell);
                if (facing == null || facing == port) continue;
                long origin = LevelNodeGraph.offset(cell, dir.getOpposite().toDirection());
                if (facing.connectsTo(origin, dir.getOpposite())) union(parent, self, index.getInt(facing));
            }
        }

        @SuppressWarnings("unchecked")
        PortNet<D>[] byRoot = new PortNet[members.size()];
        for (int i = 0; i < members.size(); i++) {
            int root = find(parent, i);
            PortNet<D> net = byRoot[root];
            if (net == null) {
                net = byRoot[root] = new PortNet<>(type);
                out.add(net);
            }
            Object member = members.get(i);
            if (member instanceof PortNode port) {
                net.ports.add(port);
                port.net = net;
            } else {
                @SuppressWarnings("unchecked")
                NodeNetwork<D> graphNet = (NodeNetwork<D>) member;
                net.nets.add(graphNet);
            }
        }
        return out;
    }

    private static int find(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void union(int[] parent, int a, int b) {
        int ra = find(parent, a);
        int rb = find(parent, b);
        if (ra == rb) return;
        if (ra < rb) parent[rb] = ra;
        else parent[ra] = rb;
    }

    /** The object whose ports count for {@code user}: a {@link Delegate}'s machine, else the user itself. */
    public static Object owner(Object user) {
        if (user instanceof Delegate delegate) {
            Object owner = delegate.portOwner();
            if (owner != null) return owner;
        }
        return user;
    }

    /**
     * A block entity or object that only fronts another machine (1.7's {@code TileEntityProxyCombo}: the custom
     * machine's port, the PWR port block, the ICF port cell, the chemical factory's coolant delegate).
     */
    public interface Delegate {
        @Nullable Object portOwner();
    }
}
