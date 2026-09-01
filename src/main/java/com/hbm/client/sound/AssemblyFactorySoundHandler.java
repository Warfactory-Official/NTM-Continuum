package com.hbm.client.sound;

import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** Client sound dispatcher for the Assembly Factory's motor, carriage, striker, and saw cues. */
public final class AssemblyFactorySoundHandler {

    private static final Map<BlockPos, AssemblyFactoryLoopSound> LOOPS = new HashMap<>();

    /** Supplied by the central sound handoff; null deliberately means no invented replacement sound. */
    private static @Nullable SoundEvent assemblerCut;

    private AssemblyFactorySoundHandler() {}

    public static void setAssemblerCut(@Nullable SoundEvent sound) {
        assemblerCut = sound;
    }

    public static void tickLoop(TileEntityMachineAssemblyFactory factory) {
        BlockPos pos = factory.getBlockPos();
        AssemblyFactoryLoopSound loop = LOOPS.get(pos);
        Minecraft minecraft = Minecraft.getInstance();
        if (factory.isRemoved()) {
            if (loop != null) loop.stopLoop();
            LOOPS.remove(pos);
            return;
        }

        var player = minecraft.player;
        boolean audible = factory.isProcessing() && player != null
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) < 50D * 50D;
        if (audible) {
            if (loop == null || loop.isStopped()) {
                loop = new AssemblyFactoryLoopSound(factory);
                LOOPS.put(pos.immutable(), loop);
                minecraft.getSoundManager().play(loop);
            }
        } else if (loop != null) {
            loop.stopLoop();
            LOOPS.remove(pos);
        }
    }

    public static void playStart(Level level, BlockPos pos, float volume, float pitch) {
        play(level, pos, ModSounds.ASSEMBLER_START.get(), volume, pitch);
    }

    public static void playStrike(Level level, BlockPos pos, float volume) {
        play(level, pos, ModSounds.ASSEMBLER_STRIKE.get(), volume, 1F);
    }

    public static void playCut(Level level, BlockPos pos, float volume) {
        if (assemblerCut != null) play(level, pos, assemblerCut, volume, 1F);
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                sound, SoundSource.BLOCKS, volume, pitch, false);
    }
}
