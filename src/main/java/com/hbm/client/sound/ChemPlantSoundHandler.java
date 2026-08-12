package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side audio for the chemical plant — a single looping operating sound while it's processing,
 * ported from the 1.12.2 client sound branch. Driven from the machine's client ticker. Client-only —
 * never referenced from a server tick path, so a dedicated server never loads it.
 */
public final class ChemPlantSoundHandler {

    private ChemPlantSoundHandler() {}

    /** One active loop per machine position (so it isn't restarted every tick). */
    private static final Map<BlockPos, ChemPlantLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineChemicalPlant te) {
        BlockPos pos = te.getBlockPos();
        ChemPlantLoopSound loop = LOOPS.get(pos);
        if (te.didProcess) {
            if (loop == null || loop.isStopped()) {
                ChemPlantLoopSound s = new ChemPlantLoopSound(te);
                LOOPS.put(pos.immutable(), s);
                Minecraft.getInstance().getSoundManager().play(s);
            }
        } else if (loop != null && loop.isStopped()) {
            LOOPS.remove(pos);
        }
    }
}
