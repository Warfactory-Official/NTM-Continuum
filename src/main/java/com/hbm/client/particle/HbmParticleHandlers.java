package com.hbm.client.particle;

import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.data.Tags;
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
        HbmEffectNT.RBMKMush.setHandler((level, x, y, z, data) -> spawnRBMKMush(level, x, y, z,
                Tags.getFloatOr(data, "scale", 20F)));
        HbmEffectNT.RBMKMushSmall.setHandler((level, x, y, z, data) -> spawnRBMKMush(level, x, y, z,
                Tags.getFloatOr(data, "scale", 5F)));
    }

    private static void spawnRBMKMush(net.minecraft.world.level.Level level, double x, double y, double z, float scale) {
        if (level instanceof ClientLevel clientLevel) {
            ParticleRBMKMush.spawn(clientLevel, x, y, z, scale);
            clientLevel.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    x, y, z, 0D, 0D, 0D);
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
