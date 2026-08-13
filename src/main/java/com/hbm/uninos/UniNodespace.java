package com.hbm.uninos;

import com.hbm.Constants;
import com.hbm.lib.DirPos;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Unified Nodespace, a Nodespace for all applications. "Nodespace" is an invisible "dimension" where
 * nodes exist; a node is the "soul" of a tile entity with networking capabilities. Tiles create
 * nodes at their position in nodespace, which handles connections (even in unloaded chunks), so a
 * node can act independently of its "body". Each {@link NodeNet} type has its own nodespace.
 *
 * <p>26.1 port: World -> Level; the 1.12.2 async/threaded path is replaced by a synchronous
 * {@link #updateNodespace(MinecraftServer)} driven from a per-loader server-tick hook, iterating
 * {@code server.getAllLevels()}. {@link #clearLevel(Level)} is new — the original leaked node maps on
 * world unload.
 *
 * @author hbm
 */
public final class UniNodespace {

    private static final Reference2ObjectOpenHashMap<INetworkProvider<?>, PerTypeNodeManager<?, ?, ?, ?>> managers = new Reference2ObjectOpenHashMap<>();
    private static int reapTimer = 0;

    private UniNodespace() {
    }

    public static <N extends NodeNet<R, P, L, N>, L extends GenNode<N>, R, P> L getNode(Level world, BlockPos pos, INetworkProvider<N> type) {
        return getManagerFor(type).getNode(world, pos);
    }

    public static <N extends NodeNet<R, P, L, N>, L extends GenNode<N>, R, P> void createNode(Level world, L node) {
        getManagerFor(node.networkProvider).createNode(world, node);
    }

    public static <N extends NodeNet<R, P, L, N>, L extends GenNode<N>, R, P> void destroyNode(Level world, BlockPos pos, INetworkProvider<N> type) {
        getManagerFor(type).destroyNode(world, pos);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <N extends NodeNet<R, P, L, N>, L extends GenNode<N>, R, P> void destroyNode(Level world, L node) {
        if (world == null || node == null) return;
        INetworkProvider<N> provider = node.networkProvider;
        if (provider != null) {
            PerTypeNodeManager<R, P, L, N> manager = (PerTypeNodeManager<R, P, L, N>) managers.get(provider);
            if (manager != null) {
                if (!manager.destroyNode(world, node)) {
                    Constants.LOG.warn("Failed to destroy node {}", node.getClass().getName());
                    Thread.dumpStack();
                }
                return;
            }
        }
        Constants.LOG.warn("[UniNodeSpace] Node {} attempts to be destroyed through mismatched type provider {}", node.getClass().getName(), provider);
        // Fallback: if provider is null/mismatched or manager isn't present yet, locate the node by identity.
        ObjectIterator<Reference2ObjectMap.Entry<INetworkProvider<?>, PerTypeNodeManager<?, ?, ?, ?>>> it = managers.reference2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            PerTypeNodeManager manager = it.next().getValue();
            if (manager.destroyNode(world, node)) return;
        }
        if (node.net != null) node.net.destroy();
        node.expired = true;
    }

    /**
     * Tick the entire nodespace once. Driven synchronously on the server thread per server tick, from
     * the loader-specific server-tick hook. Iterates every loaded level.
     */
    public static void updateNodespace(MinecraftServer server) {
        boolean any = false;
        for (ServerLevel level : server.getAllLevels()) {
            any = true;
            ObjectIterator<Reference2ObjectMap.Entry<INetworkProvider<?>, PerTypeNodeManager<?, ?, ?, ?>>> iterator = managers.reference2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                iterator.next().getValue().updateForWorld(level);
            }
        }
        if (!any) return;
        updateNetworks();
    }

    /** Removes all nodes for a level (call on level unload to avoid leaking the node maps). */
    public static void clearLevel(Level level) {
        for (PerTypeNodeManager<?, ?, ?, ?> manager : managers.values()) {
            manager.worlds.remove(level);
        }
    }

    static void removeActiveNet(NodeNet<?, ?, ?, ?> net) {
        if (net.links.isEmpty()) {
            ObjectIterator<Reference2ObjectMap.Entry<INetworkProvider<?>, PerTypeNodeManager<?, ?, ?, ?>>> iterator = managers.reference2ObjectEntrySet().fastIterator();
            while (iterator.hasNext()) {
                PerTypeNodeManager<?, ?, ?, ?> manager = iterator.next().getValue();
                manager.removeActiveNet(net);
            }
            return;
        }
        GenNode<?> node = net.links.first();
        PerTypeNodeManager<?, ?, ?, ?> manager = managers.get(node.networkProvider);
        if (manager != null) manager.removeActiveNet(net);
    }

    private static void updateNetworks() {
        for (PerTypeNodeManager<?, ?, ?, ?> manager : managers.values()) manager.resetTrackers();
        for (PerTypeNodeManager<?, ?, ?, ?> manager : managers.values()) manager.updateNetworks();
        reapNetworks();
    }

    private static void updateReapTimer() {
        if (reapTimer <= 0) reapTimer = 5 * 60 * 20;
        else reapTimer--;
    }

    private static void reapNetworks() {
        updateReapTimer();
        if (reapTimer > 0) return;
        for (PerTypeNodeManager<?, ?, ?, ?> manager : managers.values()) manager.reapLinksAndNets();
    }

    @SuppressWarnings("unchecked")
    private static <R, P, L extends GenNode<N>, N extends NodeNet<R, P, L, N>> PerTypeNodeManager<R, P, L, N> getManagerFor(
            INetworkProvider<N> provider) {
        PerTypeNodeManager<?, ?, ?, ?> manager = managers.get(provider);
        if (manager == null) {
            manager = new PerTypeNodeManager<>(provider);
            managers.put(provider, manager);
        }
        return (PerTypeNodeManager<R, P, L, N>) manager;
    }

    @SuppressWarnings("Java8CollectionRemoveIf")
    private static class PerTypeNodeManager<R, P, L extends GenNode<N>, N extends NodeNet<R, P, L, N>> {

        private final Reference2ObjectOpenHashMap<Level, UniNodeWorld<N, L>> worlds = new Reference2ObjectOpenHashMap<>();
        private final Set<N> activeNodeNets = new ReferenceOpenHashSet<>();
        private final INetworkProvider<N> provider;

        PerTypeNodeManager(INetworkProvider<N> provider) {
            this.provider = provider;
        }

        private static boolean checkConnection(GenNode<?> connectsTo, DirPos connectFrom, boolean skipSideCheck) {
            for (DirPos revCon : connectsTo.connections) {
                if (revCon.getPos().getX() - revCon.getDir().offsetX == connectFrom.getPos().getX() &&
                        revCon.getPos().getY() - revCon.getDir().offsetY == connectFrom.getPos().getY() &&
                        revCon.getPos().getZ() - revCon.getDir().offsetZ == connectFrom.getPos().getZ() &&
                        (revCon.getDir() == connectFrom.getDir().getOpposite() || skipSideCheck)) {
                    return true;
                }
            }
            return false;
        }

        private UniNodeWorld<N, L> getWorldManager(Level world) {
            return worlds.computeIfAbsent(world, key -> new UniNodeWorld<>());
        }

        L getNode(Level world, BlockPos pos) {
            UniNodeWorld<N, L> nodeWorld = worlds.get(world);
            return nodeWorld != null ? nodeWorld.getNode(pos) : null;
        }

        void createNode(Level world, L node) {
            getWorldManager(world).pushNode(node);
        }

        void destroyNode(Level world, BlockPos pos) {
            UniNodeWorld<N, L> nodeWorld = worlds.get(world);
            if (nodeWorld == null) return;

            L node = nodeWorld.getNode(pos);
            if (node != null) removeNode(nodeWorld, node);
        }

        boolean destroyNode(Level world, L node) {
            UniNodeWorld<N, L> nodeWorld = worlds.get(world);
            if (nodeWorld == null) return false;
            if (!nodeWorld.containsNode(node)) return false;
            removeNode(nodeWorld, node);
            return true;
        }

        void addActiveNet(N net) {
            activeNodeNets.add(net);
        }

        void removeActiveNet(Object net) {
            activeNodeNets.remove(net);
        }

        void updateForWorld(Level world) {
            UniNodeWorld<N, L> nodeWorld = worlds.get(world);
            if (nodeWorld == null) return;

            for (L node : nodeWorld.getAllNodes()) {
                if (node.expired) continue;
                if (!node.hasValidNet() || node.recentlyChanged) {
                    checkNodeConnection(world, node);
                    node.recentlyChanged = false;
                }
            }
        }

        void resetTrackers() {
            for (N net : activeNodeNets) {
                net.resetTrackers();
            }
        }

        void removeEmptyNets() {
            var it = activeNodeNets.iterator();
            while (it.hasNext()) {
                if (it.next().links.isEmpty()) it.remove();
            }
        }

        void reapLinksAndNets() {
            for (N net : activeNodeNets) {
                var it = net.links.iterator();
                while (it.hasNext()) {
                    if (it.next().expired) it.remove();
                }
            }
            removeEmptyNets();
        }

        void updateNetworks() {
            for (N net : activeNodeNets) {
                if (net.isValid()) net.update();
            }
        }

        private void checkNodeConnection(Level world, L node) {
            for (DirPos con : node.connections) {
                L conNode = getNode(world, con.getPos());
                if (conNode != null) {
                    if (conNode.hasValidNet() && conNode.net == node.net) continue;
                    if (checkConnection(conNode, con, false)) {
                        connectToNode(node, conNode);
                    }
                }
            }
            if (node.net == null || !node.net.isValid()) {
                N newNet = provider.get();
                this.addActiveNet(newNet);
                newNet.joinLink(node);
            }
        }

        private void connectToNode(L origin, L connection) {
            if (origin.hasValidNet() && connection.hasValidNet()) { // both have nets but different, join networks
                if (origin.net.links.size() > connection.net.links.size()) {
                    origin.net.joinNetworks(connection.net);
                } else {
                    connection.net.joinNetworks(origin.net);
                }
            } else if (!origin.hasValidNet() && connection.hasValidNet()) { // origin has no net, connection does
                connection.net.joinLink(origin);
            } else if (origin.hasValidNet() && !connection.hasValidNet()) { // ...and vice versa
                origin.net.joinLink(connection);
            }
        }

        private void removeNode(UniNodeWorld<N, L> nodeWorld, L node) {
            N oldNet = node.net;
            nodeWorld.popNode(node);
            if (oldNet != null) oldNet.links.remove(node);

            if (oldNet == null || !oldNet.isValid()) {
                node.setNet(null);
                return;
            }
            node.setNet(null);
            resetNetMembership(oldNet);

            if (oldNet.links.isEmpty()) {
                oldNet.destroy();
                return;
            }

            splitNetIfNecessary(nodeWorld, node, oldNet);
        }

        /**
         * Removing a single node can only split the graph along the removed node's incident edges.
         * Recompute connected components locally instead of invalidating the entire net.
         */
        private void splitNetIfNecessary(UniNodeWorld<N, L> nodeWorld, L removedNode, N oldNet) {
            List<L> seeds = collectAdjacentLinks(nodeWorld, removedNode, oldNet);
            if (seeds.size() <= 1) return;

            List<ReferenceOpenHashSet<L>> components = new ArrayList<>();
            ReferenceOpenHashSet<L> visited = new ReferenceOpenHashSet<>();

            for (L seed : seeds) {
                if (seed.expired || visited.contains(seed)) continue;
                components.add(collectComponent(nodeWorld, seed, oldNet, visited));
            }

            for (L link : oldNet.links) {
                if (link.expired || visited.contains(link)) continue;
                components.add(collectComponent(nodeWorld, link, oldNet, visited));
            }

            if (components.size() <= 1) return;

            int keepIndex = 0;
            int keepSize = components.get(0).size();

            for (int i = 1; i < components.size(); i++) {
                int componentSize = components.get(i).size();
                if (componentSize > keepSize) {
                    keepIndex = i;
                    keepSize = componentSize;
                }
            }

            ReferenceOpenHashSet<L> keep = components.get(keepIndex);

            var oldLinks = oldNet.links.iterator();
            while (oldLinks.hasNext()) {
                L link = oldLinks.next();
                if (!keep.contains(link)) oldLinks.remove();
            }

            for (L link : keep) {
                link.recentlyChanged = false;
            }

            for (int i = 0; i < components.size(); i++) {
                if (i == keepIndex) continue;

                N splitNet = provider.get();
                addActiveNet(splitNet);
                resetNetMembership(splitNet);

                for (L link : components.get(i)) {
                    splitNet.forceJoinLink(link);
                    link.recentlyChanged = false;
                }
            }
        }

        private List<L> collectAdjacentLinks(UniNodeWorld<N, L> nodeWorld, L node, N expectedNet) {
            List<L> seeds = new ArrayList<>();
            ReferenceOpenHashSet<L> seen = new ReferenceOpenHashSet<>();
            if (node.connections == null) return seeds;

            for (DirPos con : node.connections) {
                L conNode = nodeWorld.getNode(con.getPos());
                if (conNode == null || conNode.expired || conNode.net != expectedNet) continue;
                if (!checkConnection(conNode, con, false) || !seen.add(conNode)) continue;
                seeds.add(conNode);
            }

            return seeds;
        }

        private ReferenceOpenHashSet<L> collectComponent(UniNodeWorld<N, L> nodeWorld, L seed, N expectedNet, ReferenceOpenHashSet<L> visited) {
            ReferenceOpenHashSet<L> component = new ReferenceOpenHashSet<>();
            ArrayDeque<L> queue = new ArrayDeque<>();
            queue.add(seed);
            visited.add(seed);

            while (!queue.isEmpty()) {
                L current = queue.removeFirst();
                component.add(current);

                if (current.connections == null) continue;

                for (DirPos con : current.connections) {
                    L conNode = nodeWorld.getNode(con.getPos());
                    if (conNode == null || conNode.expired || conNode.net != expectedNet) continue;
                    if (!checkConnection(conNode, con, false) || !visited.add(conNode)) continue;
                    queue.addLast(conNode);
                }
            }

            return component;
        }

        private void resetNetMembership(N net) {
            net.receiverEntries.clear();
            net.providerEntries.clear();
            net.resetTrackers();
        }
    }

    /** Holds all nodes of a single network type for a specific Level. */
    private static class UniNodeWorld<N extends NodeNet<?, ?, L, N>, L extends GenNode<N>> {
        private final Object2ObjectLinkedOpenHashMap<BlockPos, L> nodesByPosition = new Object2ObjectLinkedOpenHashMap<>();

        L getNode(BlockPos pos) {
            return nodesByPosition.get(pos);
        }

        /** Adds a node at all its positions to the nodespace. */
        void pushNode(L node) {
            for (BlockPos pos : node.positions) {
                nodesByPosition.put(pos, node);
            }
        }

        /** Removes the specified node from all positions from nodespace. */
        void popNode(L node) {
            for (BlockPos pos : node.positions) {
                nodesByPosition.remove(pos, node);
            }
            node.expired = true;
        }

        boolean containsNode(GenNode<?> node) {
            if (node == null) return false;
            BlockPos[] positions = node.positions;
            if (positions == null) return false;
            for (BlockPos pos : positions) {
                if (nodesByPosition.get(pos) == node) return true;
            }
            return false;
        }

        /** @return a view of all nodes in this world, do not modify */
        ObjectCollection<L> getAllNodes() {
            return nodesByPosition.values();
        }
    }
}
