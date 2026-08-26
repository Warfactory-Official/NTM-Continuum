package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineLargeTurbine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client-side loop manager for the legacy large turbine. */
public final class LargeTurbineSoundHandler {

    private LargeTurbineSoundHandler() {}

    private static final Map<BlockPos, LargeTurbineLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineLargeTurbine turbine) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        BlockPos pos = turbine.getBlockPos();
        LargeTurbineLoopSound loop = LOOPS.get(pos);
        double distance = minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (turbine.fanAcceleration > 0F && distance <= 35D * 35D) {
            float speed = turbine.fanAcceleration / 15F;
            float volume = turbine.getVolume(0.4F * speed);
            float pitch = 0.25F + 0.75F * speed;
            if (loop == null || loop.isStopped()) {
                loop = new LargeTurbineLoopSound(turbine, volume, pitch);
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
