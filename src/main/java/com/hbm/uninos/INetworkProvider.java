package com.hbm.uninos;

import com.hbm.lib.DirPos;
import net.minecraft.server.level.ServerLevel;

/** One kind of port node: power, or one fluid. A port of a NONE fluid is never enabled. */
public interface INetworkProvider {

    /** The DEBUG_PORT_VIEW particle at one connection; {@code receive} points it into the machine. */
    default void spawnDebugParticles(ServerLevel level, DirPos pos, boolean receive) { }
}
