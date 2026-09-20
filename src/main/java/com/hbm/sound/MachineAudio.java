package com.hbm.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * The 1.12.2 machine-loop dance in one place: start the wrapper when the machine wants to be heard,
 * keep it alive while it does, and drop it when it stops.
 */
public final class MachineAudio {

    private MachineAudio() {}

    public static @Nullable AudioWrapper tick(@Nullable AudioWrapper audio, boolean wanted, SoundEvent sound,
                                              SoundSource category, BlockPos pos, float volume, float range,
                                              float pitch, int keepAlive) {
        return tick(audio, wanted, sound, category, pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F,
                volume, range, pitch, keepAlive);
    }

    public static @Nullable AudioWrapper tick(@Nullable AudioWrapper audio, boolean wanted, SoundEvent sound,
                                              SoundSource category, float x, float y, float z, float volume,
                                              float range, float pitch, int keepAlive) {
        if (!wanted) {
            if (audio != null) audio.stopSound();
            return null;
        }

        if (audio == null || !audio.isPlaying()) {
            audio = AudioSystem.getLoopedSound(sound, category, x, y, z, volume, range, pitch, keepAlive);
            if (audio != null) audio.startSound();
        }

        if (audio != null) {
            audio.updateVolume(volume);
            audio.updatePitch(pitch);
            audio.keepAlive();
        }

        return audio;
    }
}
