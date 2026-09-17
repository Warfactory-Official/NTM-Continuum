package com.hbm.sound;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * Common-side entry point for client-owned dynamic sounds; servers keep the null proxy.
 */
public final class AudioSystem {

    private static volatile Factory factory = Factory.SERVER;

    private AudioSystem() {
    }

    public static void installClientFactory(Factory clientFactory) {
        factory = clientFactory;
    }

    @Deprecated
    public static @Nullable AudioWrapper getLoopedSound(@Nullable SoundEvent sound, SoundSource category,
                                                        float x, float y, float z, float volume, float range,
                                                        float pitch) {
        return factory.getLoopedSound(sound, category, x, y, z, volume, range, pitch);
    }

    public static @Nullable AudioWrapper getLoopedSound(@Nullable SoundEvent sound, SoundSource category,
                                                        float x, float y, float z, float volume, float range,
                                                        float pitch, int keepAlive) {
        return factory.getLoopedSound(sound, category, x, y, z, volume, range, pitch, keepAlive);
    }

    public interface Factory {
        Factory SERVER = new Factory() {
        };

        default @Nullable AudioWrapper getLoopedSound(@Nullable SoundEvent sound, SoundSource category,
                                                      float x, float y, float z, float volume, float range,
                                                      float pitch) {
            return null;
        }

        default @Nullable AudioWrapper getLoopedSound(@Nullable SoundEvent sound, SoundSource category,
                                                      float x, float y, float z, float volume, float range,
                                                      float pitch, int keepAlive) {
            return null;
        }
    }
}
