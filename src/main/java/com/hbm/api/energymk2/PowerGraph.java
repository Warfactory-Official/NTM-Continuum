package com.hbm.api.energymk2;

import com.hbm.uninos.graph.LevelNodeGraph;
import net.minecraft.server.level.ServerLevel;

/** The per-level cable topology: one {@code hbm:power_graph} save per dimension. */
public final class PowerGraph {

    private PowerGraph() {}

    public static LevelNodeGraph<CableData> get(ServerLevel level) {
        return LevelNodeGraph.getOrCreate(level, PowerGraphProvider.INSTANCE, "power_graph");
    }
}
