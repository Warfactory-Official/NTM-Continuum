package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client-only operating loop for the Chemical Factory. */
public final class ChemicalFactorySoundHandler {

    private ChemicalFactorySoundHandler() {}

    private static final Map<BlockPos, ChemicalFactoryLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineChemicalFactory factory) {
        BlockPos pos = factory.getBlockPos();
        ChemicalFactoryLoopSound loop = LOOPS.get(pos);
        Minecraft minecraft = Minecraft.getInstance();
        boolean nearby = minecraft.player != null
                && minecraft.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 50D * 50D;
        if (factory.isProcessing() && nearby) {
            if (loop == null || loop.isStopped()) {
                ChemicalFactoryLoopSound sound = new ChemicalFactoryLoopSound(factory);
                LOOPS.put(pos.immutable(), sound);
                Minecraft.getInstance().getSoundManager().play(sound);
            }
        } else if (loop != null && loop.isStopped()) {
            LOOPS.remove(pos);
        }
    }
}
