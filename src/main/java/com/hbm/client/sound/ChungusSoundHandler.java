package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityChungus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side loop manager for the Leviathan turbine. The loop rides the fan's spin-up rather than the
 * steam flow, so it keeps playing while the fan coasts down after the machine stops.
 */
public final class ChungusSoundHandler {

    private ChungusSoundHandler() {}

    private static final Map<BlockPos, ChungusLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityChungus chungus) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        BlockPos pos = chungus.getBlockPos();
        ChungusLoopSound loop = LOOPS.get(pos);
        float speed = chungus.getTurbineSpeed();
        if (speed > 0F) {
            float volume = chungus.getVolume(0.5F * speed);
            float pitch = 0.25F + 0.75F * speed;
            if (loop == null || loop.isStopped()) {
                loop = new ChungusLoopSound(chungus, volume, pitch);
                LOOPS.put(pos.immutable(), loop);
                minecraft.getSoundManager().play(loop);
            }
            loop.update(volume, pitch);
        } else if (loop != null) {
            loop.stopLoop();
            LOOPS.remove(pos);
        }
    }
}
