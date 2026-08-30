package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineTurbofan;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class TurbofanSoundHandler {
    private TurbofanSoundHandler() {}
    private static final Map<BlockPos, TurbofanLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineTurbofan fan) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        BlockPos pos = fan.getBlockPos();
        TurbofanLoopSound loop = LOOPS.get(pos);
        double distance = minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        if (fan.momentum > 0 && distance <= 35D * 35D) {
            float volume = fan.getVolume(fan.momentum / 50F);
            float pitch = fan.momentum / 200F + 0.5F + fan.afterburner * 0.16F;
            if (loop == null || loop.isStopped()) {
                loop = new TurbofanLoopSound(fan, volume, pitch);
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
