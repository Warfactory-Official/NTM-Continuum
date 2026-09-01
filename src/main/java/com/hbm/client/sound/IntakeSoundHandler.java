package com.hbm.client.sound;

import com.hbm.tileentity.machine.TileEntityMachineIntake;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/** Client-side owner of the intake's positional motor loop. */
public final class IntakeSoundHandler {

    private IntakeSoundHandler() {}

    private static final double RANGE_SQUARED = 10D * 10D;
    private static final Map<BlockPos, IntakeLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineIntake intake) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = intake.getBlockPos();
        IntakeLoopSound loop = LOOPS.get(pos);
        boolean audible = intake.isMotorRunning() && minecraft.player != null
                && minecraft.player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D) <= RANGE_SQUARED;

        if (audible) {
            if (loop == null || loop.isStopped()) {
                loop = new IntakeLoopSound(intake);
                LOOPS.put(pos.immutable(), loop);
                minecraft.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.stopLoop();
            LOOPS.remove(pos);
        }
    }
}
