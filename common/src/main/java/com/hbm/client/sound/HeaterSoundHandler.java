package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityHeaterElectric;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side audio for the electric heater — a hum loop while it's powered (gated by the synced
 * {@code isOn}). Driven from the heater's client ticker. Client-only — never referenced from a server
 * tick path, so a dedicated server never loads it.
 */
public final class HeaterSoundHandler {

    private HeaterSoundHandler() {}

    /** One active loop per heater position (so it isn't restarted every tick). */
    private static final Map<BlockPos, HeaterLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityHeaterElectric te) {
        BlockPos pos = te.getBlockPos();
        HeaterLoopSound loop = LOOPS.get(pos);
        if (te.isOn) {
            if (loop == null || loop.isStopped()) {
                HeaterLoopSound s = new HeaterLoopSound(te);
                LOOPS.put(pos.immutable(), s);
                Minecraft.getInstance().getSoundManager().play(s);
            }
        } else if (loop != null && loop.isStopped()) {
            LOOPS.remove(pos);
        }
    }
}
