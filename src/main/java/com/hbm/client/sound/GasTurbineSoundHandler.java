package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class GasTurbineSoundHandler {
    private GasTurbineSoundHandler() {}
    private static final Map<BlockPos, GasTurbineLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineTurbineGas turbine) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        BlockPos pos = turbine.getBlockPos();
        GasTurbineLoopSound loop = LOOPS.get(pos);
        double distance = minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (turbine.rpm >= 10 && turbine.state != -1 && distance <= 35D * 35D) {
            float speed = turbine.rpm / 100F;
            float volume = turbine.getVolume(2F);
            float pitch = 0.55F + 0.1F * turbine.rpm / 10F;
            if (loop == null || loop.isStopped()) {
                loop = new GasTurbineLoopSound(turbine, volume, pitch);
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
