package com.hbm.foundry;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client loop manager for the old Arc Furnace electric-hum sound. */
public final class ArcFurnaceSoundHandler {

    private ArcFurnaceSoundHandler() {}

    private static final Map<BlockPos, ArcFurnaceLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(com.hbm.tileentity.machine.TileEntityMachineArcFurnaceLarge furnace) {
        BlockPos pos = furnace.getBlockPos();
        Minecraft minecraft = Minecraft.getInstance();
        boolean audible = furnace.isProgressing && minecraft.player != null
                && minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) < 625D;
        ArcFurnaceLoopSound loop = LOOPS.get(pos);
        if (audible) {
            if (loop == null || loop.isStopped()) {
                loop = new ArcFurnaceLoopSound(furnace);
                LOOPS.put(pos.immutable(), loop);
                minecraft.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.shutdown();
            LOOPS.remove(pos);
        }
    }
}
