package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Client audio for the four-striker precision assembler. */
public final class PrecisionAssemblerSoundHandler {

    private PrecisionAssemblerSoundHandler() {}

    private static final Map<BlockPos, PrecisionAssemblerLoopSound> LOOPS = new HashMap<>();

    public static void tickLoop(TileEntityMachinePrecAss te) {
        BlockPos pos = te.getBlockPos();
        PrecisionAssemblerLoopSound loop = LOOPS.get(pos);
        if (te.isRemoved()) {
            LOOPS.remove(pos);
            return;
        }
        if (te.didProcess) {
            if (loop == null || loop.isStopped()) {
                PrecisionAssemblerLoopSound sound = new PrecisionAssemblerLoopSound(te);
                LOOPS.put(pos.immutable(), sound);
                Minecraft.getInstance().getSoundManager().play(sound);
            }
        } else if (loop != null && loop.isStopped()) {
            LOOPS.remove(pos);
        }
    }

    public static void playStrike(Level level, BlockPos pos, float volume) {
        play(level, pos, ModSounds.ASSEMBLER_STRIKE.get(), volume, 1.25F);
    }

    public static void playStart(Level level, BlockPos pos, float volume, float pitch) {
        play(level, pos, ModSounds.ASSEMBLER_START.get(), volume, pitch);
    }

    public static void playStop(Level level, BlockPos pos, float volume) {
        playStop(level, pos, volume, 1.25F);
    }

    public static void playStop(Level level, BlockPos pos, float volume, float pitch) {
        play(level, pos, ModSounds.ASSEMBLER_STOP.get(), volume, pitch);
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                sound, SoundSource.BLOCKS, volume, pitch, false);
    }
}
