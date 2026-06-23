package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public final class AssemblerSoundHandler {

    private AssemblerSoundHandler() {}

    private static final Map<BlockPos, AssemblerLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachineAssemblyMachine te) {
        BlockPos pos = te.getBlockPos();
        AssemblerLoopSound loop = LOOPS.get(pos);
        if (te.didProcess) {
            if (loop == null || loop.isStopped()) {
                AssemblerLoopSound s = new AssemblerLoopSound(te);
                LOOPS.put(pos.immutable(), s);
                Minecraft.getInstance().getSoundManager().play(s);
            }
        } else if (loop != null && loop.isStopped()) {
            LOOPS.remove(pos);
        }
    }

    public static void playStrike(Level level, BlockPos pos, float volume) {
        play(level, pos, ModSounds.ASSEMBLER_STRIKE.get(), volume, 1.0F);
    }

    public static void playStart(Level level, BlockPos pos, float volume, float pitch) {
        play(level, pos, ModSounds.ASSEMBLER_START.get(), volume, pitch);
    }

    public static void playStop(Level level, BlockPos pos, float volume) {
        play(level, pos, ModSounds.ASSEMBLER_STOP.get(), volume, 1.5F);
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                sound, SoundSource.BLOCKS, volume, pitch, false);
    }
}
