package com.hbm.handler.radiation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Proxy facade over the chunk radiation system, ported from the fork's {@code ChunkRadiationManager}
 * (which wraps only {@code RadiationSystemNT} — this branch removed the simple handler). Kept so ported
 * call sites read unchanged ({@code ChunkRadiationManager.proxy.getRadiation(...)}). Delegates to
 * {@link RadiationSystemNT}; all calls are server-side only.
 */
public final class ChunkRadiationManager {

    public static final ProxyClass proxy = new ProxyClass();

    public static final class ProxyClass {

        public double getRadiation(Level level, BlockPos pos) {
            if (!(level instanceof ServerLevel server)) return 0D;
            return RadiationSystemNT.getRadForCoord(server, pos);
        }

        public void setRadiation(Level level, BlockPos pos, double rad) {
            if (level instanceof ServerLevel server) RadiationSystemNT.setRadForCoord(server, pos, rad);
        }

        public void incrementRad(Level level, BlockPos pos, double rad) {
            incrementRad(level, pos, rad, Double.MAX_VALUE);
        }

        public void incrementRad(Level level, BlockPos pos, double rad, double max) {
            if (level instanceof ServerLevel server) RadiationSystemNT.incrementRad(server, pos, rad, max);
        }

        public void decrementRad(Level level, BlockPos pos, double rad) {
            if (level instanceof ServerLevel server) RadiationSystemNT.decrementRad(server, pos, rad);
        }

        public void clearSystem(Level level) {
            if (level instanceof ServerLevel server) RadiationSystemNT.clearWorld(server);
        }
    }
}
