package com.hbm.client.sound;

import com.hbm.tileentity.machine.oil.TileEntityMachinePyroOven;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client-side owner of the pyro oven's 1.12.2 operating loop. */
public final class PyroOvenSoundHandler {

    private static final double RANGE_SQUARED = 15D * 15D;
    private static final Map<BlockPos, PyroOvenLoopSound> LOOPS = new HashMap<>();

    private PyroOvenSoundHandler() {}

    public static void tickLoop(TileEntityMachinePyroOven oven) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = oven.getBlockPos();
        PyroOvenLoopSound loop = LOOPS.get(pos);

        boolean audible = !oven.isRemoved() && oven.isProgressing && minecraft.player != null
                && minecraft.player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= RANGE_SQUARED;
        if (audible) {
            if (loop == null || loop.isStopped()) {
                loop = new PyroOvenLoopSound(oven);
                LOOPS.put(pos.immutable(), loop);
                minecraft.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.stopLoop();
            LOOPS.remove(pos);
        }
    }

    public static void onRemoved(TileEntityMachinePyroOven oven) {
        PyroOvenLoopSound loop = LOOPS.remove(oven.getBlockPos());
        if (loop != null) loop.stopLoop();
    }
}
