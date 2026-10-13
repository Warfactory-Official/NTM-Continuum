package com.hbm.tileentity.network;

import dev.golem.core.world.Compat;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import com.hbm.module.ModulePatternMatcher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The delivery drones' node index: every drone dock, provider, requester and request waypoint republishes a
 * snapshot of itself here, and a node whose owner stops republishing expires.
 *
 * <p>Node identity is its position, so a republish replaces the previous snapshot - its offers, its requests
 * and the set of nodes it could see when it was taken.
 */
public final class RequestNetwork {

    /** 1.7: 2000 ms of wall clock. */
    public static final int MAX_AGE = 40;
    /** 1.7 TileEntityRequestNetwork.maxRange. */
    public static final double MAX_LINK_RANGE = 24;

    private static final Map<ResourceKey<Level>, Long2ObjectMap<Map<BlockPos, PathNode>>> NODES = new HashMap<>();

    private static long tick;

    private RequestNetwork() {
    }

    public static void push(ServerLevel level, PathNode node) {
        node.lease = tick;
        NODES.computeIfAbsent(level.dimension(), key -> new Long2ObjectOpenHashMap<>())
             .computeIfAbsent(chunkKey(node.pos), key -> new HashMap<BlockPos, PathNode>())
             .put(node.pos, node);
    }

    /** Every live node within {@code chunkRange} chunks of {@code centre}, keyed by position. */
    public static Map<BlockPos, PathNode> localNodes(ServerLevel level, BlockPos centre, int chunkRange) {
        Long2ObjectMap<Map<BlockPos, PathNode>> chunks = NODES.get(level.dimension());
        if (chunks == null) return Map.of();

        Map<BlockPos, PathNode> nodes = new HashMap<>();
        int x = SectionPos.blockToSectionCoord(centre.getX());
        int z = SectionPos.blockToSectionCoord(centre.getZ());

        for (int i = -chunkRange; i <= chunkRange; i++) {
            for (int j = -chunkRange; j <= chunkRange; j++) {
                Map<BlockPos, PathNode> inChunk = chunks.get(ChunkPos.pack(x + i, z + j));
                if (inChunk != null) nodes.putAll(inChunk);
            }
        }

        return nodes;
    }

    /**
     * 1.7 ModEventHandler:1030, server tick START. Its own throttle never decrements
     * ({@code RequestNetwork:44-51} re-arms the counter instead of counting it down), so the sweep runs every
     * tick; the tick counter here is that clock, and does not depend on a level that may no longer be loaded.
     */
    public static void updateEntries(MinecraftServer server) {
        tick++;

        NODES.values().removeIf(chunks -> {
            chunks.values().removeIf(inChunk -> {
                inChunk.values().removeIf(node -> node.lease < tick - MAX_AGE);
                return inChunk.isEmpty();
            });
            return chunks.isEmpty();
        });
    }

    public static void onServerStopping() {
        clear();
    }

    /** Drops every node. The index is process state keyed by position, so a reused position inherits it. */
    public static void clear() {
        NODES.clear();
        tick = 0;
    }

    /**
     * 1.7: bidirectional, because 1.7's own ray trace answered differently from either end. 26.1's
     * {@link Level#clip} is not that function and is asked once.
     */
    public static boolean hasPath(ServerLevel level, BlockPos from, BlockPos to) {
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 end = Vec3.atCenterOf(to);
        if (start.distanceTo(end) > MAX_LINK_RANGE) return false;

        return Compat.clip(level, start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE)
                .getType() == HitResult.Type.MISS;
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.pack(SectionPos.blockToSectionCoord(pos.getX()),
                SectionPos.blockToSectionCoord(pos.getZ()));
    }

    /** A node the drones may fly to. */
    public static class PathNode {

        public final BlockPos pos;
        /** Waypoints link to anything; a dock, provider or requester links only through a waypoint. */
        public final boolean waypoint;
        public final Set<BlockPos> reachable;
        public boolean active = true;
        long lease;

        public PathNode(BlockPos pos, Set<BlockPos> reachable) {
            this(pos, reachable, true);
        }

        PathNode(BlockPos pos, Set<BlockPos> reachable, boolean waypoint) {
            this.pos = pos;
            this.waypoint = waypoint;
            this.reachable = new HashSet<>(reachable);
        }
    }

    /** What a provider is holding, as of its last republish. */
    public static final class OfferNode extends PathNode {

        public final List<ItemStack> offer;

        public OfferNode(BlockPos pos, Set<BlockPos> reachable, List<ItemStack> offer) {
            super(pos, reachable, false);
            this.offer = new ArrayList<>(offer);
        }
    }

    /**
     * One unfulfilled request: the filter stack and the {@link ModulePatternMatcher} mode it is read under.
     *
     * <p>1.7 re-encoded the pair as an {@code AStack}, a vocabulary with no bedrock grade in it, so a
     * bedrock-mode slot asked for an ore dictionary name nothing carries. The matcher already answers the
     * pair, so it is asked directly and that mode works here.
     */
    public record Demand(ItemStack filter, String mode) {

        public static final Codec<Demand> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.CODEC.fieldOf("filter").forGetter(Demand::filter),
                Codec.STRING.fieldOf("mode").forGetter(Demand::mode)).apply(i, Demand::new));

        public boolean test(ItemStack stack) {
            return ModulePatternMatcher.matches(mode, filter, stack);
        }
    }

    /** What a requester is short of, as of its last republish. */
    public static final class RequestNode extends PathNode {

        public final List<Demand> request;

        public RequestNode(BlockPos pos, Set<BlockPos> reachable, List<Demand> request) {
            super(pos, reachable, false);
            this.request = new ArrayList<>(request);
        }
    }
}
