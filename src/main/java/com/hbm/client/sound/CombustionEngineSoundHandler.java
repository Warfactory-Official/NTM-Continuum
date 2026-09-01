package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineCombustionEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client-side owner of the combustion engine's 1.12.2 operating loop. */
public final class CombustionEngineSoundHandler {

    private static final double RANGE_SQUARED = 10D * 10D;
    private static final Map<BlockPos, CombustionEngineLoopSound> LOOPS = new HashMap<>();

    private CombustionEngineSoundHandler() {}

    public static void tickLoop(TileEntityMachineCombustionEngine engine) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = engine.getBlockPos();
        CombustionEngineLoopSound loop = LOOPS.get(pos);

        boolean audible = !engine.isRemoved() && engine.wasOn && minecraft.player != null
                && minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= RANGE_SQUARED;
        if (audible) {
            if (loop == null || loop.isStopped()) {
                loop = new CombustionEngineLoopSound(engine);
                LOOPS.put(pos.immutable(), loop);
                minecraft.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.stopLoop();
            LOOPS.remove(pos);
        }
    }

    public static void onRemoved(TileEntityMachineCombustionEngine engine) {
        CombustionEngineLoopSound loop = LOOPS.remove(engine.getBlockPos());
        if (loop != null) loop.stopLoop();
    }
}
