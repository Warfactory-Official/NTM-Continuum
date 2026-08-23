package com.hbm.handler.neutron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Per-world queued streams and cached nodes. */
public final class NeutronNodeWorld {
    private NeutronNodeWorld() {}
    public static final HashMap<Level, StreamWorld> streamWorlds = new HashMap<>();
    public static NeutronNode getNode(Level world, BlockPos pos) { StreamWorld state = streamWorlds.get(world); return state == null ? null : state.nodeCache.get(pos); }
    public static void removeNode(Level world, BlockPos pos) { StreamWorld state = streamWorlds.get(world); if (state != null) state.removeNode(pos); }
    public static StreamWorld getOrAddWorld(Level world) { return streamWorlds.computeIfAbsent(world, ignored -> new StreamWorld()); }
    public static void removeAllWorlds() { streamWorlds.clear(); }
    public static void removeEmptyWorlds() { streamWorlds.values().removeIf(state -> state.streams.isEmpty()); }
    public static class StreamWorld {
        private final List<NeutronStream> streams = new ArrayList<>();
        private final HashMap<BlockPos, NeutronNode> nodeCache = new HashMap<>();
        public void runStreamInteractions(Level world) { for (NeutronStream stream : streams) stream.runStreamInteraction(world, this); }
        public void addStream(NeutronStream stream) { streams.add(stream); }
        public void removeAllStreams() { streams.clear(); }
        public void cleanNodes() { List<BlockPos> remove = new ArrayList<>(); for (NeutronNode node : nodeCache.values()) if (node.type == NeutronStream.NeutronType.RBMK) remove.addAll(((RBMKNeutronHandler.RBMKNeutronNode) node).checkNode(this)); for (BlockPos pos : remove) nodeCache.remove(pos); }
        public NeutronNode getNode(BlockPos pos) { return nodeCache.get(pos); }
        public void addNode(NeutronNode node) { nodeCache.put(node.pos, node); }
        public void removeNode(BlockPos pos) { nodeCache.remove(pos); }
    }
}
