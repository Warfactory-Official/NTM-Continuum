package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side audio for the ore acidizer: a single looping operating sound while it's processing
 * (the chemical-plant loop at a lower pitch, like the 1.12.2 tile). Driven from the machine's client
 * ticker. Client-only, never referenced from a server tick path, so a dedicated server never loads it.
 */
public final class CrystallizerSoundHandler {

    private CrystallizerSoundHandler() {}

    /** One active loop per machine position (so it isn't restarted every tick). */
    private static final Map<BlockPos, CrystallizerLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineCrystallizer te) {
        BlockPos pos = te.getBlockPos();
        CrystallizerLoopSound loop = LOOPS.get(pos);
        if (te.isOn) {
            if (loop == null || loop.isStopped()) {
                CrystallizerLoopSound s = new CrystallizerLoopSound(te);
                LOOPS.put(pos.immutable(), s);
                Minecraft.getInstance().getSoundManager().play(s);
            }
        } else if (loop != null && loop.isStopped()) {
            LOOPS.remove(pos);
        }
    }
}
