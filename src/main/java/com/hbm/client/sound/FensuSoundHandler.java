package com.hbm.client.sound;

import com.hbm.tileentity.machine.storage.TileEntityBatteryREDD;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client-side manager for the FEnSU's speed-dependent fensuHum loop. */
public final class FensuSoundHandler {

    private FensuSoundHandler() {}

    private static final Map<BlockPos, FensuLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityBatteryREDD battery) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        BlockPos pos = battery.getBlockPos();
        FensuLoopSound loop = LOOPS.get(pos);
        double distance = minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 5.5D, pos.getZ() + 0.5D);
        if (battery.getSpeed() > 0F && distance < 30D * 30D) {
            float volume = battery.getVolume(1.5F);
            float pitch = 0.5F + battery.getSpeed() / 15F * 1.5F;
            if (loop == null || loop.isStopped()) {
                loop = new FensuLoopSound(battery, volume, pitch);
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
