package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineChemicalPlant;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

public final class ChemPlantSoundHandler {

    private ChemPlantSoundHandler() {}

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
