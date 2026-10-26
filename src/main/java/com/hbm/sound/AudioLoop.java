package com.hbm.sound;

import org.jspecify.annotations.Nullable;

/** A real machine-owned loop whose existing wrapper is maintained by the shared native audio lifecycle. */
public interface AudioLoop {
    @Nullable AudioWrapper getAudioLoop();
    void setAudioLoop(@Nullable AudioWrapper audio);
    @Nullable AudioWrapper createAudioLoop();
    float getVolume(float volume);
    default void audioLoop(boolean running, float volume) {
        setAudioLoop(MachineAudio.tick(this, getAudioLoop(), running, volume, 0F, false));
    }
    default void audioLoop(boolean running, float volume, float pitch) {
        setAudioLoop(MachineAudio.tick(this, getAudioLoop(), running, volume, pitch, true));
    }
}
