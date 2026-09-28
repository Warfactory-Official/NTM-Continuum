package com.hbm.uninos.networkproviders;

import com.hbm.blocks.generic.BlockRebar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 1.7 {@code RebarNetwork}: every rebar is a node connected to its six neighbours, so a net is one face-connected
 * cage of rebar, and the net does nothing on its own ({@code update()} was empty) - a rebar receiving concrete
 * asks it for its members. The port answers that question with a flood fill over the rebar blocks instead of
 * keeping 1.7's node space, cached per start position until any rebar is placed or removed.
 */
public final class RebarNetwork {

    private RebarNetwork() {}

    private static final Map<Level, Map<BlockPos, List<BlockPos>>> CACHE = new WeakHashMap<>();

    /** A rebar appeared or vanished somewhere: every cached cage is stale. */
    public static synchronized void invalidate() {
        CACHE.clear();
    }

    /** Every rebar face-connected to {@code start}, itself included. */
    public static synchronized List<BlockPos> linksOf(Level level, BlockPos start) {
        Map<BlockPos, List<BlockPos>> perLevel = CACHE.computeIfAbsent(level, key -> new java.util.HashMap<>());
        List<BlockPos> cached = perLevel.get(start);
        if (cached != null) return cached;

        List<BlockPos> links = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start.immutable());
        seen.add(start.immutable());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!(level.getBlockState(pos).getBlock() instanceof BlockRebar)) continue;
            links.add(pos);
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (level.isLoaded(next) && seen.add(next)) queue.add(next);
            }
        }
        List<BlockPos> frozen = List.copyOf(links);
        for (BlockPos member : frozen) perLevel.put(member, frozen);
        return frozen;
    }
}
