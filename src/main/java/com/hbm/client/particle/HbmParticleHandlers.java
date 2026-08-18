package com.hbm.client.particle;

import com.hbm.particle.helper.HbmEffectNT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Binds each {@link HbmEffectNT} to the code that builds its particles.
 *
 * <p>Client only, and the reason {@link HbmEffectNT} itself is free of client types: the enum is the
 * shared vocabulary both sides name effects with, and this class is the half that only a client has.
 * Called once from each loader's client entrypoint.
 */
public final class HbmParticleHandlers {

    private HbmParticleHandlers() {}

    public static void register() {
        for (var entry : ParticleCreators.particleCreators.entrySet()) {
            HbmEffectNT effect = byCreatorKey(entry.getKey());
            if (effect == null) continue;
            var creator = entry.getValue();
            effect.setHandler((level, x, y, z, data) -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (!(level instanceof ClientLevel clientLevel)) return;
                creator.makeParticle(clientLevel, minecraft.player, clientLevel.getRandom(), x, y, z, data);
            });
        }
    }

    /**
     * Maps upstream's creator-table key onto the effect constant.
     *
     * <p>The two spellings differ ({@code "explosionSmall"} vs {@code ExplosionSmall}) because both
     * are upstream's and both are load-bearing — the string keys appear in old call sites, the enum
     * travels on the wire. Matching case-insensitively keeps one table instead of two.
     */
    private static HbmEffectNT byCreatorKey(String key) {
        for (HbmEffectNT effect : HbmEffectNT.values()) {
            if (effect.name().equalsIgnoreCase(key)) return effect;
        }
        return null;
    }
}
