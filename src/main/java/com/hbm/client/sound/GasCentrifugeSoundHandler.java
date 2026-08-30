package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineGasCent;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client-side manager for the gas centrifuge's ramped operating loop. */
public final class GasCentrifugeSoundHandler {

    private GasCentrifugeSoundHandler() {}

    private static final Map<BlockPos, GasCentrifugeLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineGasCent te) {
        BlockPos pos = te.getBlockPos();
        GasCentrifugeLoopSound loop = LOOPS.get(pos);
        Minecraft minecraft = Minecraft.getInstance();
        boolean audible = te.audioDuration > 10 && minecraft.player != null
                && minecraft.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 625;

        if (audible) {
            if (loop == null || loop.isStopped()) {
                loop = new GasCentrifugeLoopSound(te);
                LOOPS.put(pos.immutable(), loop);
                minecraft.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.shutdown();
            LOOPS.remove(pos);
        }
    }
}
