package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineIndustrialTurbine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client-side loop manager for the industrial turbine's speed-dependent sound. */
public final class IndustrialTurbineSoundHandler {

    private IndustrialTurbineSoundHandler() {}

    private static final Map<BlockPos, IndustrialTurbineLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineIndustrialTurbine turbine) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        BlockPos pos = turbine.getBlockPos();
        IndustrialTurbineLoopSound loop = LOOPS.get(pos);
        double distance = minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (turbine.spin > 0D && distance <= 35D * 35D) {
            float spin = (float) Math.min(1D, turbine.spin * 2D);
            float volume = turbine.getVolume(0.25F + spin * 0.75F);
            float pitch = 0.5F + spin * 0.5F + turbine.getAudioDesync();
            if (loop == null || loop.isStopped()) {
                loop = new IndustrialTurbineLoopSound(turbine, volume, pitch);
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
