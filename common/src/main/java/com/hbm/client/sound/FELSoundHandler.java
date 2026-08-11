package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityFEL;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side audio for the FEL. Driven from the machine's client ticker, which owns the ramp counter;
 * this only starts and forgets the loop. Client-only — never referenced from a server tick path.
 */
public final class FELSoundHandler {

    private FELSoundHandler() {}

    /** One active loop per machine position (so it isn't restarted every tick). */
    private static final Map<BlockPos, FELLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityFEL te) {
        BlockPos pos = te.getBlockPos();
        FELLoopSound loop = LOOPS.get(pos);
        if (te.isAudioPlaying()) {
            if (loop == null || loop.isStopped()) {
                FELLoopSound s = new FELLoopSound(te);
                LOOPS.put(pos.immutable(), s);
                Minecraft.getInstance().getSoundManager().play(s);
            }
        } else if (loop != null && loop.isStopped()) {
            LOOPS.remove(pos);
        }
    }
}
