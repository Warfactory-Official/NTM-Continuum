package com.hbm.handler.radiation;

import com.hbm.config.RadiationConfig;
import com.hbm.particle.helper.HbmEffectNT;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1.7.10's radiation fog ({@code radFog}), rolled from the existing chunk radiation without touching it.
 *
 * <p>1.7's {@code ChunkRadiationHandlerSimple.updateSystem} ran once a second and, for every hand-over of a chunk's
 * radiation (nine per radiated chunk), rolled {@code 1 in fogCh} when the value was above {@code fogRad} and put one
 * {@code ParticleRadiationFog} at a random column of the chunk, {@code height + rand(5)} up. The port keeps its own
 * engine ({@link RadiationSystemNT}), so this reads it: once a second, every live chunk whose highest pocket
 * holds more than {@code fogRad} gets nine rolls, and a successful roll places the fog where the air at the
 * surface of the chosen column holds more than {@code fogRad}. 1.7 sent nothing from a dedicated server (the
 * effect went through the server-side proxy); the port sends the effect to the players tracking the chunk.
 */
public final class RadiationFog {

    private RadiationFog() {}

    private static int timer;
    /** Fog effects sent since start (harness evidence only). */
    public static int sent;

    public static void init() {
        dev.golem.core.platform.Lifecycle.onServerTick(RadiationFog::tick);
    }

    private static void tick(MinecraftServer server) {
        if (++timer < 20) return;
        timer = 0;
        for (ServerLevel level : server.getAllLevels()) roll(level);
    }

    static void roll(ServerLevel level) {
        RandomSource rand = level.getRandom();
        RadiationSystemNT.forEachChunkMax(level, (cx, cz, max) -> {
            if (max <= RadiationConfig.fogRad) return;
            for (int hand = 0; hand < 9; hand++) {
                if (rand.nextInt(RadiationConfig.fogCh) != 0) continue;
                if (level.getChunkSource().getChunkNow(cx, cz) == null) return;
                int x = cx * 16 + rand.nextInt(16);
                int z = cz * 16 + rand.nextInt(16);
                int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                int y = height + rand.nextInt(5);
                if (RadiationSystemNT.getRadForCoord(level, new BlockPos(x, height, z)) <= RadiationConfig.fogRad) continue;
                HbmEffectNT.RadFog.send(level, x, y, z, null);
                sent++;
            }
        });
    }
}
